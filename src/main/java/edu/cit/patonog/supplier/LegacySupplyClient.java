package edu.cit.patonog.supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
class LegacySupplyClient {

    private final String baseUrl;
    private final SessionManager sessionManager;
    private final HttpClient httpClient;

    LegacySupplyClient(
            @Value("${legacy-supply.base-url:https://legacysupply.onrender.com/api/v1}") String baseUrl,
            SessionManager sessionManager) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.sessionManager = sessionManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    record PlaceOrderResponse(boolean success, String poNumber, int statusCode, String errorCode, String errorMessage) {}

    PlaceOrderResponse placeOrder(String supplierSku, int qty, String buyerRef, String requestId) {
        String requestXml = "<PurchaseOrder>" +
                "<SupplierSku>" + escapeXml(supplierSku) + "</SupplierSku>" +
                "<Qty>" + qty + "</Qty>" +
                "<BuyerRef>" + escapeXml(buyerRef) + "</BuyerRef>" +
                "</PurchaseOrder>";

        int maxAttempts = 3;
        long backoffMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String token = sessionManager.getSessionToken();
            if (token == null || token.isBlank()) {
                token = sessionManager.refreshSessionToken();
            }

            if (token == null || token.isBlank()) {
                return new PlaceOrderResponse(false, null, 0, "NO_SESSION", "Unable to obtain LegacySupply session token");
            }

            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/purchase-orders"))
                        .timeout(Duration.ofSeconds(3))
                        .header("Content-Type", "application/xml")
                        .header("X-LS-Session", token)
                        .POST(HttpRequest.BodyPublishers.ofString(requestXml, StandardCharsets.UTF_8));

                if (requestId != null && !requestId.isBlank()) {
                    reqBuilder.header("X-Request-Id", requestId);
                }

                HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();

                if (status == 201 || status == 200) {
                    Document doc = parseXml(response.body());
                    String poNumber = getTagValue(doc, "PoNumber");
                    String codeStr = getTagValue(doc, "StatusCode");
                    int statusCode = 10;
                    if (codeStr != null && !codeStr.isBlank()) {
                        try {
                            statusCode = Integer.parseInt(codeStr.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    return new PlaceOrderResponse(true, poNumber, statusCode, null, null);
                }

                if (status == 401) {
                    sessionManager.invalidateSession();
                    if (attempt < maxAttempts) {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                        continue;
                    }
                }

                Document errorDoc = parseXml(response.body());
                String errCode = getTagValue(errorDoc, "Code");
                String errMsg = getTagValue(errorDoc, "Message");

                if (status >= 500 && attempt < maxAttempts) {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                    continue;
                }

                return new PlaceOrderResponse(false, null, status, errCode, errMsg != null ? errMsg : "HTTP " + status);
            } catch (Exception ex) {
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) {}
                    backoffMs *= 2;
                } else {
                    return new PlaceOrderResponse(false, null, 0, "TIMEOUT_OR_NETWORK", ex.getMessage());
                }
            }
        }

        return new PlaceOrderResponse(false, null, 0, "RETRY_EXHAUSTED", "Failed after 3 attempts");
    }

    record StatusCheckResponse(boolean success, int statusCode, String errorCode, String errorMessage) {}

    StatusCheckResponse checkOrderStatus(String poNumber) {
        if (poNumber == null || poNumber.isBlank()) {
            return new StatusCheckResponse(false, 0, "INVALID_PO", "PO number is blank");
        }

        String token = sessionManager.getSessionToken();
        if (token == null) {
            token = sessionManager.refreshSessionToken();
        }
        if (token == null) {
            return new StatusCheckResponse(false, 0, "NO_SESSION", "No session token");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/purchase-orders/" + poNumber))
                    .timeout(Duration.ofSeconds(3))
                    .header("X-LS-Session", token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                Document doc = parseXml(response.body());
                String codeStr = getTagValue(doc, "StatusCode");
                int statusCode = 10;
                if (codeStr != null && !codeStr.isBlank()) {
                    statusCode = Integer.parseInt(codeStr.trim());
                }
                return new StatusCheckResponse(true, statusCode, null, null);
            }

            if (response.statusCode() == 401) {
                sessionManager.invalidateSession();
            }

            Document errDoc = parseXml(response.body());
            return new StatusCheckResponse(false, response.statusCode(), getTagValue(errDoc, "Code"), getTagValue(errDoc, "Message"));
        } catch (Exception ex) {
            return new StatusCheckResponse(false, 0, "NETWORK_ERROR", ex.getMessage());
        }
    }

    private Document parseXml(String xml) {
        if (xml == null || xml.isBlank()) return null;
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private String getTagValue(Document doc, String tagName) {
        if (doc == null) return null;
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

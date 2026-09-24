package edu.cit.patonog.supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
class SessionManager {

    private final String baseUrl;
    private final String clientId;
    private final String apiKey;
    private final HttpClient httpClient;

    private String cachedToken;

    SessionManager(
            @Value("${legacy-supply.base-url:https://legacysupply.onrender.com/api/v1}") String baseUrl,
            @Value("${legacy-supply.client-id:23-0065-102}") String clientId,
            @Value("${LS_API_KEY:}") String apiKeyEnv,
            @Value("${legacy-supply.api-key:}") String apiKeyProp) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clientId = clientId;
        String resolvedKey = (apiKeyEnv != null && !apiKeyEnv.isBlank()) ? apiKeyEnv : apiKeyProp;
        if (resolvedKey == null || resolvedKey.isBlank()) {
            resolvedKey = System.getenv("LS_API_KEY");
        }
        this.apiKey = resolvedKey != null ? resolvedKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    synchronized String getSessionToken() {
        if (cachedToken != null && !cachedToken.isBlank()) {
            return cachedToken;
        }
        return refreshSessionToken();
    }

    synchronized String refreshSessionToken() {
        this.cachedToken = null;
        if (apiKey.isBlank()) {
            return null;
        }

        String requestXml = "<AuthRequest>" +
                "<ClientId>" + escapeXml(clientId) + "</ClientId>" +
                "<ApiKey>" + escapeXml(apiKey) + "</ApiKey>" +
                "</AuthRequest>";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/auth/token"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(requestXml, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));
                NodeList nodes = doc.getElementsByTagName("SessionToken");
                if (nodes.getLength() > 0) {
                    this.cachedToken = nodes.item(0).getTextContent().trim();
                    return this.cachedToken;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    synchronized void invalidateSession() {
        this.cachedToken = null;
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

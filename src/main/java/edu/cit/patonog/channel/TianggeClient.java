package edu.cit.patonog.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.patonog.config.AppInstanceHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
class TianggeClient {

    private final String baseUrl;
    private final String clientId;
    private final String apiKey;
    private final AppInstanceHolder appInstanceHolder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    TianggeClient(
            @Value("${tiangge.base-url:https://legacysupply.onrender.com/tiangge/v1}") String baseUrl,
            @Value("${tiangge.client-id:23-0065-102}") String clientId,
            @Value("${LS_API_KEY:}") String apiKeyEnv,
            @Value("${tiangge.api-key:}") String apiKeyProp,
            AppInstanceHolder appInstanceHolder,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clientId = clientId;
        String resolvedKey = (apiKeyEnv != null && !apiKeyEnv.isBlank()) ? apiKeyEnv : apiKeyProp;
        if (resolvedKey == null || resolvedKey.isBlank()) {
            resolvedKey = System.getenv("LS_API_KEY");
        }
        this.apiKey = resolvedKey != null ? resolvedKey.trim() : "";
        this.appInstanceHolder = appInstanceHolder;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    boolean sendHeartbeat(String appName, String startedAt, long uptimeSeconds) {
        try {
            TianggeDto.HeartbeatRequest body = new TianggeDto.HeartbeatRequest(appName, startedAt, uptimeSeconds);
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = newRequestBuilder("/instances/heartbeat")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            return response != null && response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    boolean publishListings(List<TianggeDto.ListingPayload> listings) {
        try {
            String json = objectMapper.writeValueAsString(listings);
            HttpRequest request = newRequestBuilder("/listings")
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            return response != null && (response.statusCode() == 200 || response.statusCode() == 204);
        } catch (Exception e) {
            return false;
        }
    }

    boolean publishStock(List<TianggeDto.StockPayload> stock) {
        try {
            String json = objectMapper.writeValueAsString(stock);
            HttpRequest request = newRequestBuilder("/stock")
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            return response != null && (response.statusCode() == 200 || response.statusCode() == 204);
        } catch (Exception e) {
            return false;
        }
    }

    TianggeDto.FeedResponse fetchFeed(long afterCursor, int limit) {
        try {
            String path = "/feed?after=" + afterCursor + "&limit=" + limit;
            HttpRequest request = newRequestBuilder(path)
                    .GET()
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            if (response != null && response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), TianggeDto.FeedResponse.class);
            }
        } catch (Exception ignored) {
        }
        return new TianggeDto.FeedResponse(Collections.emptyList(), afterCursor);
    }

    boolean sendDecision(String orderId, String decision, String shopOrderId, String reason) {
        try {
            TianggeDto.DecisionPayload payload = new TianggeDto.DecisionPayload(decision, shopOrderId, reason);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = newRequestBuilder("/orders/" + orderId + "/decision")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            return response != null && (response.statusCode() == 200 || response.statusCode() == 201);
        } catch (Exception e) {
            return false;
        }
    }

    boolean sendResolution(String orderId, String status) {
        try {
            TianggeDto.ResolutionPayload payload = new TianggeDto.ResolutionPayload(status);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = newRequestBuilder("/orders/" + orderId + "/resolution")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            return response != null && (response.statusCode() == 200 || response.statusCode() == 204);
        } catch (Exception e) {
            return false;
        }
    }

    boolean confirmCancellation(String orderId, boolean restocked) {
        try {
            TianggeDto.CancellationPayload payload = new TianggeDto.CancellationPayload(restocked);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = newRequestBuilder("/orders/" + orderId + "/cancellation")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = executeWithRetry(request);
            return response != null && (response.statusCode() == 200 || response.statusCode() == 204);
        } catch (Exception e) {
            return false;
        }
    }

    private HttpRequest.Builder newRequestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("X-Client-Id", clientId)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Client-Instance", appInstanceHolder.getInstanceId());
    }

    private HttpResponse<String> executeWithRetry(HttpRequest request) {
        int maxAttempts = 3;
        long backoffMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 500) {
                    return response;
                }
                if (attempt < maxAttempts) {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                }
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) {}
                    backoffMs *= 2;
                }
            }
        }
        return null;
    }
}

package edu.cit.patonog.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

class TianggeDto {

    record HeartbeatRequest(String appName, String startedAt, long uptimeSeconds) {}

    record HeartbeatResponse(String serverTime, int nextHeartbeatSeconds) {}

    record ListingPayload(String sellerSku, String title, String supplierSku) {}

    record StockPayload(String sellerSku, int available) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeedResponse(List<FeedEvent> events, Long nextCursor) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeedEvent(
            long seq,
            String eventId,
            String type,
            String orderId,
            String placedAt,
            String decisionDeadline,
            List<FeedLine> lines,
            BuyerDto buyer,
            String cancelledAt,
            String confirmDeadline
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeedLine(String sellerSku, int qty) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BuyerDto(String name, String city) {}

    record DecisionPayload(String decision, String shopOrderId, String reason) {}

    record ResolutionPayload(String status) {}

    record CancellationPayload(boolean restocked) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorResponse(String error, String message) {}
}

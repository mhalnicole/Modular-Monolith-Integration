package edu.cit.patonog.events;

import java.time.LocalDateTime;

public class OrderRejectedEvent {

    private final String orderId;
    private final String reason;
    private final LocalDateTime timestamp;

    public OrderRejectedEvent(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

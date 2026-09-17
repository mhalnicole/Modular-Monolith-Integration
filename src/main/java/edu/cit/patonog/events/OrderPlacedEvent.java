package edu.cit.patonog.events;

import java.time.LocalDateTime;

public class OrderPlacedEvent {

    private final String orderId;
    private final int totalItems;
    private final LocalDateTime timestamp;

    public OrderPlacedEvent(String orderId, int totalItems) {
        this.orderId = orderId;
        this.totalItems = totalItems;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

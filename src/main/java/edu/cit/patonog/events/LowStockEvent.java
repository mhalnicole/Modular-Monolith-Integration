package edu.cit.patonog.events;

import java.time.LocalDateTime;

public class LowStockEvent {

    private final String productId;
    private final String productName;
    private final int remainingStock;
    private final LocalDateTime timestamp;

    public LowStockEvent(String productId, String productName, int remainingStock) {
        this.productId = productId;
        this.productName = productName;
        this.remainingStock = remainingStock;
        this.timestamp = LocalDateTime.now();
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

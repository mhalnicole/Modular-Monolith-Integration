package edu.cit.patonog.events;

public class StockChangedEvent {

    private final String productId;
    private final int remainingStock;

    public StockChangedEvent(String productId, int remainingStock) {
        this.productId = productId;
        this.remainingStock = remainingStock;
    }

    public String getProductId() {
        return productId;
    }

    public int getRemainingStock() {
        return remainingStock;
    }
}

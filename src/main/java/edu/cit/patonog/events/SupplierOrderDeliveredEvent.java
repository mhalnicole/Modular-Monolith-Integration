package edu.cit.patonog.events;

public class SupplierOrderDeliveredEvent {

    private final String productId;
    private final int units;
    private final String buyerRef;
    private final String poNumber;

    public SupplierOrderDeliveredEvent(String productId, int units, String buyerRef, String poNumber) {
        this.productId = productId;
        this.units = units;
        this.buyerRef = buyerRef;
        this.poNumber = poNumber;
    }

    public String getProductId() {
        return productId;
    }

    public int getUnits() {
        return units;
    }

    public String getBuyerRef() {
        return buyerRef;
    }

    public String getPoNumber() {
        return poNumber;
    }
}

package edu.cit.patonog.shop;

import edu.cit.patonog.inventory.InventoryItem;

public class OrderResponse {

    private String orderId;
    private String status;
    private String reason;
    private InventoryItem inventory;

    public OrderResponse() {
    }

    public OrderResponse(String orderId, String status, String reason, InventoryItem inventory) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.inventory = inventory;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public InventoryItem getInventory() {
        return inventory;
    }

    public void setInventory(InventoryItem inventory) {
        this.inventory = inventory;
    }
}

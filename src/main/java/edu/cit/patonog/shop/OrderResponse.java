package edu.cit.patonog.shop;

import edu.cit.patonog.inventory.InventoryItem;

import java.util.ArrayList;
import java.util.List;

public class OrderResponse {

    private String orderId;
    private String status;
    private String reason;
    private List<OrderItemDto> items;
    private Object inventory;

    public OrderResponse() {
        this.items = new ArrayList<>();
    }

    public OrderResponse(String orderId, String status, String reason, List<OrderItemDto> items, Object inventory) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.items = items != null ? items : new ArrayList<>();
        this.inventory = inventory;
    }

    public OrderResponse(String orderId, String status, String reason, Object inventory) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.items = new ArrayList<>();
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

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public Object getInventory() {
        return inventory;
    }

    public void setInventory(Object inventory) {
        this.inventory = inventory;
    }
}

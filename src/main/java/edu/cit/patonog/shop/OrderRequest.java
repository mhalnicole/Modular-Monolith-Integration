package edu.cit.patonog.shop;

import java.util.ArrayList;
import java.util.List;

public class OrderRequest {

    private List<OrderItemDto> items;
    private String productId;
    private Integer quantity;

    public OrderRequest() {
        this.items = new ArrayList<>();
    }

    public OrderRequest(List<OrderItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public OrderRequest(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.items = new ArrayList<>();
        if (productId != null && quantity != null) {
            this.items.add(new OrderItemDto(productId, quantity));
        }
    }

    public List<OrderItemDto> getItems() {
        if ((items == null || items.isEmpty()) && productId != null && quantity != null) {
            List<OrderItemDto> fallback = new ArrayList<>();
            fallback.add(new OrderItemDto(productId, quantity));
            return fallback;
        }
        return items != null ? items : new ArrayList<>();
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

package edu.cit.patonog.shop;

public class OrderItemDto {

    private String productId;
    private Integer quantity;
    private String outcome;

    public OrderItemDto() {
    }

    public OrderItemDto(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public OrderItemDto(String productId, Integer quantity, String outcome) {
        this.productId = productId;
        this.quantity = quantity;
        this.outcome = outcome;
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

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
}

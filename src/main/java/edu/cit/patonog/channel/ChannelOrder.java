package edu.cit.patonog.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_orders")
class ChannelOrder {

    @Id
    @Column(name = "tiangge_order_id", nullable = false)
    private String tianggeOrderId;

    @Column(name = "shop_order_id")
    private String shopOrderId;

    @Column(name = "decision", nullable = false)
    private String decision;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "lines_json", nullable = false, columnDefinition = "TEXT")
    private String linesJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ChannelOrder() {}

    public ChannelOrder(String tianggeOrderId, String shopOrderId, String decision, String status, String linesJson) {
        this.tianggeOrderId = tianggeOrderId;
        this.shopOrderId = shopOrderId;
        this.decision = decision;
        this.status = status;
        this.linesJson = linesJson;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getTianggeOrderId() {
        return tianggeOrderId;
    }

    public void setTianggeOrderId(String tianggeOrderId) {
        this.tianggeOrderId = tianggeOrderId;
    }

    public String getShopOrderId() {
        return shopOrderId;
    }

    public void setShopOrderId(String shopOrderId) {
        this.shopOrderId = shopOrderId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLinesJson() {
        return linesJson;
    }

    public void setLinesJson(String linesJson) {
        this.linesJson = linesJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

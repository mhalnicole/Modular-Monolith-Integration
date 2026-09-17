package edu.cit.patonog.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "notification_id", nullable = false, length = 100)
    private String notificationId;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(String notificationId, String message, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

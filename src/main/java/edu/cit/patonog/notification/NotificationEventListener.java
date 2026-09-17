package edu.cit.patonog.notification;

import edu.cit.patonog.events.LowStockEvent;
import edu.cit.patonog.events.OrderPlacedEvent;
import edu.cit.patonog.events.OrderRejectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;

    public NotificationEventListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        String msg = String.format("Order %s confirmed (%d item%s)",
                event.getOrderId(), event.getTotalItems(), event.getTotalItems() > 1 ? "s" : "");
        saveNotification(msg);
    }

    @EventListener
    public void onOrderRejected(OrderRejectedEvent event) {
        String msg = String.format("Order %s rejected: %s",
                event.getOrderId(), event.getReason());
        saveNotification(msg);
    }

    @EventListener
    public void onLowStock(LowStockEvent event) {
        String msg = String.format("Reorder needed: %s (%s) stock is down to %d units",
                event.getProductName(), event.getProductId(), event.getRemainingStock());
        saveNotification(msg);
    }

    private void saveNotification(String message) {
        String id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Notification notification = new Notification(id, message, LocalDateTime.now());
        notificationRepository.save(notification);
    }
}

package edu.cit.patonog.shop;

import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;

    /**
     * Constructor injection depends strictly on the public InventoryService interface,
     * maintaining architectural decoupling from the package-private InventoryServiceImpl.
     */
    public OrderService(InventoryService inventoryService, OrderRepository orderRepository) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        String productId = request.getProductId() != null ? request.getProductId().trim() : null;
        int quantity = request.getQuantity() != null ? request.getQuantity() : 0;

        if (productId == null || productId.isEmpty() || quantity <= 0) {
            return recordAndRespond(productId != null ? productId : "UNKNOWN", quantity, "REJECTED", "Invalid product ID or non-positive quantity", null);
        }

        Optional<InventoryItem> itemOptional = inventoryService.getItem(productId);
        if (itemOptional.isEmpty()) {
            return recordAndRespond(productId, quantity, "REJECTED", "Product not found: " + productId, null);
        }

        InventoryItem currentItem = itemOptional.get();
        if (currentItem.getStock() < quantity) {
            String reason = String.format("Insufficient stock: Requested %d, but only %d available", quantity, currentItem.getStock());
            return recordAndRespond(productId, quantity, "REJECTED", reason, currentItem);
        }

        boolean reserved = inventoryService.reserve(productId, quantity);
        if (reserved) {
            InventoryItem updatedItem = inventoryService.getItem(productId).orElse(currentItem);
            String reason = String.format("Order placed successfully for %d unit(s) of %s", quantity, updatedItem.getName());
            return recordAndRespond(productId, quantity, "CONFIRMED", reason, updatedItem);
        } else {
            InventoryItem latestItem = inventoryService.getItem(productId).orElse(currentItem);
            String reason = String.format("Reservation failed: Insufficient stock (current stock: %d)", latestItem.getStock());
            return recordAndRespond(productId, quantity, "REJECTED", reason, latestItem);
        }
    }

    private OrderResponse recordAndRespond(String productId, int quantity, String status, String reason, InventoryItem item) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Order order = new Order();
        order.setOrderId(orderId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(status);
        order.setReason(reason);
        order.setCreatedAt(LocalDateTime.now());
        
        orderRepository.save(order);

        return new OrderResponse(orderId, status, reason, item);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
}

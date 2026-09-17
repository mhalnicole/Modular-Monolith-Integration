package edu.cit.patonog.shop;

import edu.cit.patonog.events.OrderPlacedEvent;
import edu.cit.patonog.events.OrderRejectedEvent;
import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(InventoryService inventoryService,
                        OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<OrderItemDto> requestedItems = request.getItems();

        if (requestedItems == null || requestedItems.isEmpty()) {
            String reason = "Order must contain at least one line item";
            recordOrder(orderId, "REJECTED", reason, Collections.emptyList());
            eventPublisher.publishEvent(new OrderRejectedEvent(orderId, reason));
            return new OrderResponse(orderId, "REJECTED", reason, Collections.emptyList(), inventoryService.getAllItems());
        }

        List<OrderItemDto> processedItems = new ArrayList<>();
        for (OrderItemDto item : requestedItems) {
            String productId = item.getProductId() != null ? item.getProductId().trim() : null;
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            if (productId == null || productId.isEmpty() || quantity <= 0) {
                String reason = "Invalid line item specification (invalid product ID or non-positive quantity)";
                recordOrder(orderId, "REJECTED", reason, requestedItems);
                eventPublisher.publishEvent(new OrderRejectedEvent(orderId, reason));
                return new OrderResponse(orderId, "REJECTED", reason, requestedItems, inventoryService.getAllItems());
            }

            Optional<InventoryItem> inventoryItemOpt = inventoryService.getItem(productId);
            if (inventoryItemOpt.isEmpty()) {
                String reason = String.format("Product not found: %s", productId);
                for (OrderItemDto it : requestedItems) {
                    processedItems.add(new OrderItemDto(it.getProductId(), it.getQuantity(), "FAILED"));
                }
                recordOrder(orderId, "REJECTED", reason, processedItems);
                eventPublisher.publishEvent(new OrderRejectedEvent(orderId, reason));
                return new OrderResponse(orderId, "REJECTED", reason, processedItems, inventoryService.getAllItems());
            }

            InventoryItem inventoryItem = inventoryItemOpt.get();
            if (inventoryItem.getStock() < quantity) {
                String reason = String.format("Insufficient stock for %s (%s): Requested %d, but only %d available",
                        inventoryItem.getName(), productId, quantity, inventoryItem.getStock());
                for (OrderItemDto it : requestedItems) {
                    processedItems.add(new OrderItemDto(it.getProductId(), it.getQuantity(), "FAILED"));
                }
                recordOrder(orderId, "REJECTED", reason, processedItems);
                eventPublisher.publishEvent(new OrderRejectedEvent(orderId, reason));
                return new OrderResponse(orderId, "REJECTED", reason, processedItems, inventoryService.getAllItems());
            }
        }

        for (OrderItemDto item : requestedItems) {
            String productId = item.getProductId().trim();
            int quantity = item.getQuantity();

            boolean reserved = inventoryService.reserve(productId, quantity);
            if (!reserved) {
                String reason = String.format("Reservation failed unexpectedly for product %s", productId);
                recordOrder(orderId, "REJECTED", reason, requestedItems);
                eventPublisher.publishEvent(new OrderRejectedEvent(orderId, reason));
                return new OrderResponse(orderId, "REJECTED", reason, requestedItems, inventoryService.getAllItems());
            }
            processedItems.add(new OrderItemDto(productId, quantity, "RESERVED"));
        }

        String reason = String.format("Order placed successfully with %d line item(s)", processedItems.size());
        Order savedOrder = recordOrder(orderId, "CONFIRMED", reason, processedItems);
        eventPublisher.publishEvent(new OrderPlacedEvent(orderId, processedItems.size()));

        return new OrderResponse(orderId, "CONFIRMED", reason, processedItems, inventoryService.getAllItems());
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is already CANCELLED: " + orderId);
        }

        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CONFIRMED orders can be cancelled. Current status: " + order.getStatus());
        }

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                inventoryService.restock(item.getProductId(), item.getQuantity());
            }
        }

        String reason = "Order cancelled and items restocked to inventory";
        order.setStatus("CANCELLED");
        order.setReason(reason);
        orderRepository.updateOrderStatus(orderId, "CANCELLED", reason);
        return order;
    }

    private Order recordOrder(String orderId, String status, String reason, List<OrderItemDto> items) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(status);
        order.setReason(reason);
        order.setCreatedAt(LocalDateTime.now());

        if (items != null) {
            for (OrderItemDto dto : items) {
                if (dto.getProductId() != null && dto.getQuantity() != null) {
                    order.addItem(dto.getProductId(), dto.getQuantity());
                }
            }
        }

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
}

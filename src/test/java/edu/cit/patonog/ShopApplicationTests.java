package edu.cit.patonog;

import edu.cit.patonog.events.LowStockEvent;
import edu.cit.patonog.events.OrderPlacedEvent;
import edu.cit.patonog.events.OrderRejectedEvent;
import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import edu.cit.patonog.notification.Notification;
import edu.cit.patonog.notification.NotificationEventListener;
import edu.cit.patonog.notification.NotificationRepository;
import edu.cit.patonog.shop.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopApplicationTests {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private OrderService orderService;

    private InventoryItem mouseItem;
    private InventoryItem keyboardItem;
    private InventoryItem hubItem;

    @BeforeEach
    void setUp() {
        mouseItem = new InventoryItem("P100", "Wireless Mouse", 25);
        keyboardItem = new InventoryItem("P200", "Mechanical Keyboard", 10);
        hubItem = new InventoryItem("P300", "USB-C Hub", 0);
    }

    @Test
    @DisplayName("Multi-Item Order CONFIRMED: All items pass validation, stock is reserved, event published")
    void testMultiItemOrder_Confirmed_AllItemsReserved() {
        when(inventoryService.getItem("P100")).thenReturn(Optional.of(mouseItem));
        when(inventoryService.getItem("P200")).thenReturn(Optional.of(keyboardItem));
        when(inventoryService.reserve(eq("P100"), eq(2))).thenReturn(true);
        when(inventoryService.reserve(eq("P200"), eq(1))).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = new OrderRequest(List.of(
                new OrderItemDto("P100", 2),
                new OrderItemDto("P200", 1)
        ));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(2, response.getItems().size());
        assertEquals("RESERVED", response.getItems().get(0).getOutcome());
        assertEquals("RESERVED", response.getItems().get(1).getOutcome());

        verify(inventoryService, times(1)).reserve("P100", 2);
        verify(inventoryService, times(1)).reserve("P200", 1);
        verify(eventPublisher, times(1)).publishEvent(any(OrderPlacedEvent.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Multi-Item Order REJECTED: All-or-nothing rollback when one item has insufficient stock")
    void testMultiItemOrder_Rejected_AllOrNothingRollback() {
        when(inventoryService.getItem("P100")).thenReturn(Optional.of(mouseItem));
        when(inventoryService.getItem("P300")).thenReturn(Optional.of(hubItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = new OrderRequest(List.of(
                new OrderItemDto("P100", 1),
                new OrderItemDto("P300", 1)
        ));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertTrue(response.getReason().contains("Insufficient stock"));

        verify(inventoryService, never()).reserve(anyString(), anyInt());
        verify(eventPublisher, times(1)).publishEvent(any(OrderRejectedEvent.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Multi-Item Order REJECTED: Product not found")
    void testMultiItemOrder_Rejected_ProductNotFound() {
        when(inventoryService.getItem("P999")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = new OrderRequest(List.of(
                new OrderItemDto("P999", 1)
        ));

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertTrue(response.getReason().contains("Product not found"));
        verify(inventoryService, never()).reserve(anyString(), anyInt());
        verify(eventPublisher, times(1)).publishEvent(any(OrderRejectedEvent.class));
    }

    @Test
    @DisplayName("Cancel Order Success: Order is set to CANCELLED and items are restocked")
    void testCancelOrder_Success_RestocksInventory() {
        Order existingOrder = new Order("ORD-1234", "CONFIRMED", "Confirmed order", LocalDateTime.now());
        existingOrder.addItem("P100", 2);
        existingOrder.addItem("P200", 1);

        when(orderRepository.findById("ORD-1234")).thenReturn(Optional.of(existingOrder));

        Order cancelledOrder = orderService.cancelOrder("ORD-1234");

        assertNotNull(cancelledOrder);
        assertEquals("CANCELLED", cancelledOrder.getStatus());
        verify(inventoryService, times(1)).restock("P100", 2);
        verify(inventoryService, times(1)).restock("P200", 1);
        verify(orderRepository, times(1)).updateOrderStatus(eq("ORD-1234"), eq("CANCELLED"), anyString());
    }

    @Test
    @DisplayName("Cancel Order 404: Non-existent order throws NOT_FOUND")
    void testCancelOrder_NotFound_Throws404() {
        when(orderRepository.findById("ORD-NONEXISTENT")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.cancelOrder("ORD-NONEXISTENT");
        });

        assertEquals(404, ex.getStatusCode().value());
        verify(inventoryService, never()).restock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Cancel Order 409: Already cancelled order throws CONFLICT")
    void testCancelOrder_AlreadyCancelled_Throws409() {
        Order alreadyCancelled = new Order("ORD-9999", "CANCELLED", "Already cancelled", LocalDateTime.now());
        when(orderRepository.findById("ORD-9999")).thenReturn(Optional.of(alreadyCancelled));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.cancelOrder("ORD-9999");
        });

        assertEquals(409, ex.getStatusCode().value());
        verify(inventoryService, never()).restock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Notification Listener: Correctly persists notifications for all domain events")
    void testNotificationEventListener_PersistsNotifications() {
        NotificationEventListener listener = new NotificationEventListener(notificationRepository);

        listener.onOrderPlaced(new OrderPlacedEvent("ORD-101", 2));
        listener.onOrderRejected(new OrderRejectedEvent("ORD-102", "Out of stock"));
        listener.onLowStock(new LowStockEvent("P100", "Wireless Mouse", 3));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertTrue(saved.get(0).getMessage().contains("Order ORD-101 confirmed"));
        assertTrue(saved.get(1).getMessage().contains("Order ORD-102 rejected: Out of stock"));
        assertTrue(saved.get(2).getMessage().contains("Reorder needed: Wireless Mouse (P100) stock is down to 3 units"));
    }
}

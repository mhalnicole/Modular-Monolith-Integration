package edu.cit.patonog;

import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import edu.cit.patonog.shop.Order;
import edu.cit.patonog.shop.OrderRepository;
import edu.cit.patonog.shop.OrderRequest;
import edu.cit.patonog.shop.OrderResponse;
import edu.cit.patonog.shop.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @DisplayName("Confirmed Path: Order should be CONFIRMED when stock is sufficient")
    void testOrderConfirmed_Success() {
        // Arrange: P100 has 25 in stock, we order 2
        when(inventoryService.getItem("P100"))
                .thenReturn(Optional.of(mouseItem))
                .thenReturn(Optional.of(new InventoryItem("P100", "Wireless Mouse", 23)));
        when(inventoryService.reserve(eq("P100"), eq(2))).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest("P100", 2);

        // Act
        OrderResponse response = orderService.placeOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        assertTrue(response.getReason().contains("Order placed successfully"));
        assertNotNull(response.getInventory());
        assertEquals(23, response.getInventory().getStock());
        verify(inventoryService, times(1)).reserve("P100", 2);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Rejected Path: Order should be REJECTED when requested quantity exceeds stock")
    void testOrderRejected_InsufficientStock() {
        // Arrange: P200 has 10 in stock, we order 15
        when(inventoryService.getItem("P200")).thenReturn(Optional.of(keyboardItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest("P200", 15);

        // Act
        OrderResponse response = orderService.placeOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertTrue(response.getReason().contains("Insufficient stock"));
        assertEquals(10, response.getInventory().getStock());
        // Verify reserve was never called because stock check failed early
        verify(inventoryService, never()).reserve(anyString(), anyInt());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Rejected Path: Order should be REJECTED for out-of-stock item (stock = 0)")
    void testOrderRejected_ZeroStock() {
        // Arrange: P300 has 0 in stock
        when(inventoryService.getItem("P300")).thenReturn(Optional.of(hubItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest("P300", 1);

        // Act
        OrderResponse response = orderService.placeOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertTrue(response.getReason().contains("Insufficient stock"));
        assertEquals(0, response.getInventory().getStock());
        verify(inventoryService, never()).reserve(anyString(), anyInt());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Rejected Path: Order should be REJECTED when product is not found")
    void testOrderRejected_ProductNotFound() {
        when(inventoryService.getItem("P999")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest("P999", 1);

        OrderResponse response = orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertTrue(response.getReason().contains("Product not found"));
        assertNull(response.getInventory());
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}

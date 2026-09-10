package edu.cit.patonog.shop;

import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000"})
public class OrderController {

    private final OrderService orderService;
    private final InventoryService inventoryService;

    public OrderController(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    /**
     * Required REST endpoint: POST /api/orders
     * Request: { "productId": "P100", "quantity": 2 }
     * Response: { "orderId": "...", "status": "CONFIRMED", "reason": "...", "inventory": { ... } }
     */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper endpoint to fetch current stock for product dropdown and live inventory UI.
     */
    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryItem>> getInventory() {
        return ResponseEntity.ok(inventoryService.getAllItems());
    }

    /**
     * Helper endpoint to retrieve order history for testing and verification.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}

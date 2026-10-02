package edu.cit.patonog.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import edu.cit.patonog.shop.OrderRequest;
import edu.cit.patonog.shop.OrderResponse;
import edu.cit.patonog.shop.OrderService;
import edu.cit.patonog.supplier.SupplierGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelProcessorTests {

    @Mock
    private OrderService orderService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private SupplierGateway supplierGateway;

    @Mock
    private TianggeClient tianggeClient;

    @Mock
    private ChannelOrderRepository channelOrderRepository;

    @Mock
    private MarketplaceGateway marketplaceGateway;

    private ObjectMapper objectMapper;
    private ChannelOrderProcessor processor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new ChannelOrderProcessor(
                orderService,
                inventoryService,
                supplierGateway,
                tianggeClient,
                channelOrderRepository,
                marketplaceGateway,
                objectMapper
        );
    }

    @Test
    @DisplayName("Tiangge Order Placed: ACCEPTED when stock is available")
    void testOrderPlaced_Accepted_WhenStockAvailable() {
        TianggeDto.FeedLine line = new TianggeDto.FeedLine("P100", 2);
        TianggeDto.FeedEvent event = new TianggeDto.FeedEvent(
                1L, "evt_1", "ORDER_PLACED", "TG-101",
                "2026-10-01T00:00:00Z", "2026-10-01T00:01:00Z",
                List.of(line), null, null, null
        );

        when(channelOrderRepository.existsById("TG-101")).thenReturn(false);
        when(inventoryService.getItem("P100")).thenReturn(Optional.of(new InventoryItem("P100", "Wireless Mouse", 20)));
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(new OrderResponse("ORD-901", "CONFIRMED", "Success", Collections.emptyList(), Collections.emptyList()));

        processor.handleOrderPlaced(event);

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
        verify(tianggeClient, times(1)).sendDecision(eq("TG-101"), eq("ACCEPTED"), eq("ORD-901"), any());
        verify(marketplaceGateway, times(1)).syncStock("P100");
        verify(channelOrderRepository, times(1)).save(any(ChannelOrder.class));
    }

    @Test
    @DisplayName("Tiangge Order Placed: BACKORDERED when stock is insufficient but supplier order exists")
    void testOrderPlaced_Backordered_WhenOpenSupplierOrderExists() {
        TianggeDto.FeedLine line = new TianggeDto.FeedLine("P300", 5);
        TianggeDto.FeedEvent event = new TianggeDto.FeedEvent(
                2L, "evt_2", "ORDER_PLACED", "TG-102",
                "2026-10-01T00:00:00Z", "2026-10-01T00:01:00Z",
                List.of(line), null, null, null
        );

        when(channelOrderRepository.existsById("TG-102")).thenReturn(false);
        when(inventoryService.getItem("P300")).thenReturn(Optional.of(new InventoryItem("P300", "USB-C Hub", 0)));
        when(supplierGateway.hasOpenOrder("P300")).thenReturn(true);

        processor.handleOrderPlaced(event);

        verify(orderService, never()).placeOrder(any(OrderRequest.class));
        verify(tianggeClient, times(1)).sendDecision(eq("TG-102"), eq("BACKORDERED"), eq("BO-TG-102"), any());
        verify(channelOrderRepository, times(1)).save(any(ChannelOrder.class));
    }

    @Test
    @DisplayName("Tiangge Order Cancelled: Cancels local order, confirms cancellation and syncs stock")
    void testOrderCancelled_CancelsLocalOrderAndConfirms() {
        TianggeDto.FeedEvent event = new TianggeDto.FeedEvent(
                3L, "evt_3", "ORDER_CANCELLED", "TG-103",
                null, null, null, null,
                "2026-10-01T00:02:00Z", "2026-10-01T00:03:00Z"
        );

        ChannelOrder order = new ChannelOrder("TG-103", "ORD-888", "ACCEPTED", "ACCEPTED", "[]");
        when(channelOrderRepository.findById("TG-103")).thenReturn(Optional.of(order));

        processor.handleOrderCancelled(event);

        verify(orderService, times(1)).cancelOrder("ORD-888");
        verify(tianggeClient, times(1)).confirmCancellation(eq("TG-103"), eq(true));
        verify(marketplaceGateway, times(1)).syncAllStock();
    }
}

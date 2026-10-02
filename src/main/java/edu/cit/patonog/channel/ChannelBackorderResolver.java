package edu.cit.patonog.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.patonog.events.SupplierOrderDeliveredEvent;
import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import edu.cit.patonog.shop.OrderItemDto;
import edu.cit.patonog.shop.OrderRequest;
import edu.cit.patonog.shop.OrderResponse;
import edu.cit.patonog.shop.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
class ChannelBackorderResolver {

    private final ChannelOrderRepository channelOrderRepository;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final TianggeClient tianggeClient;
    private final MarketplaceGateway marketplaceGateway;
    private final ObjectMapper objectMapper;

    ChannelBackorderResolver(
            ChannelOrderRepository channelOrderRepository,
            OrderService orderService,
            InventoryService inventoryService,
            TianggeClient tianggeClient,
            MarketplaceGateway marketplaceGateway,
            ObjectMapper objectMapper) {
        this.channelOrderRepository = channelOrderRepository;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.tianggeClient = tianggeClient;
        this.marketplaceGateway = marketplaceGateway;
        this.objectMapper = objectMapper;
    }

    @EventListener
    @Transactional
    public void onSupplierDelivery(SupplierOrderDeliveredEvent event) {
        List<ChannelOrder> backorders = channelOrderRepository.findByStatus("BACKORDERED");
        if (backorders.isEmpty()) {
            return;
        }

        for (ChannelOrder co : backorders) {
            try {
                List<TianggeDto.FeedLine> lines = objectMapper.readValue(
                        co.getLinesJson(),
                        new TypeReference<List<TianggeDto.FeedLine>>() {}
                );

                if (lines == null || lines.isEmpty()) {
                    continue;
                }

                boolean canFulfill = true;
                for (TianggeDto.FeedLine line : lines) {
                    Optional<InventoryItem> itemOpt = inventoryService.getItem(line.sellerSku());
                    if (itemOpt.isEmpty() || itemOpt.get().getStock() < line.qty()) {
                        canFulfill = false;
                        break;
                    }
                }

                if (canFulfill) {
                    List<OrderItemDto> dtoList = lines.stream()
                            .map(l -> new OrderItemDto(l.sellerSku(), l.qty()))
                            .toList();

                    OrderResponse response = orderService.placeOrder(new OrderRequest(dtoList));
                    if ("CONFIRMED".equals(response.getStatus())) {
                        tianggeClient.sendResolution(co.getTianggeOrderId(), "ACCEPTED");
                        co.setStatus("ACCEPTED");
                        co.setShopOrderId(response.getOrderId());
                        co.setUpdatedAt(LocalDateTime.now());
                        channelOrderRepository.save(co);
                        marketplaceGateway.syncAllStock();
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}

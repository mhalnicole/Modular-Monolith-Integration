package edu.cit.patonog.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import edu.cit.patonog.shop.OrderItemDto;
import edu.cit.patonog.shop.OrderRequest;
import edu.cit.patonog.shop.OrderResponse;
import edu.cit.patonog.shop.OrderService;
import edu.cit.patonog.supplier.SupplierGateway;
import edu.cit.patonog.supplier.SupplierReorderResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
class ChannelOrderProcessor {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final SupplierGateway supplierGateway;
    private final TianggeClient tianggeClient;
    private final ChannelOrderRepository channelOrderRepository;
    private final MarketplaceGateway marketplaceGateway;
    private final ObjectMapper objectMapper;

    ChannelOrderProcessor(
            OrderService orderService,
            InventoryService inventoryService,
            SupplierGateway supplierGateway,
            TianggeClient tianggeClient,
            ChannelOrderRepository channelOrderRepository,
            MarketplaceGateway marketplaceGateway,
            ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.supplierGateway = supplierGateway;
        this.tianggeClient = tianggeClient;
        this.channelOrderRepository = channelOrderRepository;
        this.marketplaceGateway = marketplaceGateway;
        this.objectMapper = objectMapper;
    }

    void handleOrderPlaced(TianggeDto.FeedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }

        if (channelOrderRepository.existsById(event.orderId())) {
            return;
        }

        String linesJson = "[]";
        try {
            linesJson = objectMapper.writeValueAsString(event.lines());
        } catch (Exception ignored) {
        }

        if (event.lines() == null || event.lines().isEmpty()) {
            String shopOrderId = "REJ-" + event.orderId();
            tianggeClient.sendDecision(event.orderId(), "REJECTED", shopOrderId, "No line items");
            channelOrderRepository.save(new ChannelOrder(event.orderId(), shopOrderId, "REJECTED", "REJECTED", linesJson));
            return;
        }

        boolean canFulfill = true;
        List<String> missingProducts = new ArrayList<>();

        for (TianggeDto.FeedLine line : event.lines()) {
            Optional<InventoryItem> itemOpt = inventoryService.getItem(line.sellerSku());
            if (itemOpt.isEmpty() || itemOpt.get().getStock() < line.qty()) {
                canFulfill = false;
                missingProducts.add(line.sellerSku());
            }
        }

        if (canFulfill) {
            List<OrderItemDto> dtoList = event.lines().stream()
                    .map(l -> new OrderItemDto(l.sellerSku(), l.qty()))
                    .toList();

            OrderResponse response = orderService.placeOrder(new OrderRequest(dtoList));
            if ("CONFIRMED".equals(response.getStatus())) {
                String shopOrderId = response.getOrderId();
                tianggeClient.sendDecision(event.orderId(), "ACCEPTED", shopOrderId, "Order accepted");
                channelOrderRepository.save(new ChannelOrder(event.orderId(), shopOrderId, "ACCEPTED", "ACCEPTED", linesJson));
                for (TianggeDto.FeedLine line : event.lines()) {
                    marketplaceGateway.syncStock(line.sellerSku());
                }
            } else {
                String shopOrderId = "REJ-" + event.orderId();
                tianggeClient.sendDecision(event.orderId(), "REJECTED", shopOrderId, response.getReason());
                channelOrderRepository.save(new ChannelOrder(event.orderId(), shopOrderId, "REJECTED", "REJECTED", linesJson));
            }
        } else {
            boolean allMissingHaveOpenPO = true;
            for (String missingProduct : missingProducts) {
                boolean hasOpen = supplierGateway.hasOpenOrder(missingProduct);
                if (!hasOpen) {
                    SupplierReorderResult reorderResult = supplierGateway.reorder(missingProduct, 15);
                    hasOpen = (reorderResult != null && reorderResult.success()) || supplierGateway.hasOpenOrder(missingProduct);
                }
                if (!hasOpen) {
                    allMissingHaveOpenPO = false;
                }
            }

            if (allMissingHaveOpenPO) {
                String shopOrderId = "BO-" + event.orderId();
                tianggeClient.sendDecision(event.orderId(), "BACKORDERED", shopOrderId, "Awaiting supplier delivery");
                channelOrderRepository.save(new ChannelOrder(event.orderId(), shopOrderId, "BACKORDERED", "BACKORDERED", linesJson));
            } else {
                String shopOrderId = "REJ-" + event.orderId();
                tianggeClient.sendDecision(event.orderId(), "REJECTED", shopOrderId, "Insufficient stock");
                channelOrderRepository.save(new ChannelOrder(event.orderId(), shopOrderId, "REJECTED", "REJECTED", linesJson));
            }
        }
    }

    void handleOrderCancelled(TianggeDto.FeedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }

        Optional<ChannelOrder> coOpt = channelOrderRepository.findById(event.orderId());
        if (coOpt.isPresent()) {
            ChannelOrder co = coOpt.get();
            if ("ACCEPTED".equals(co.getStatus()) && co.getShopOrderId() != null && co.getShopOrderId().startsWith("ORD-")) {
                try {
                    orderService.cancelOrder(co.getShopOrderId());
                } catch (Exception ignored) {
                }
            }
            co.setStatus("CANCELLED_BY_CUSTOMER");
            co.setUpdatedAt(LocalDateTime.now());
            channelOrderRepository.save(co);
        }

        tianggeClient.confirmCancellation(event.orderId(), true);
        marketplaceGateway.syncAllStock();
    }
}

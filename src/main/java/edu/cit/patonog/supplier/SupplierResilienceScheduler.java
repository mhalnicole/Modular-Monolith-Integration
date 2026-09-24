package edu.cit.patonog.supplier;

import edu.cit.patonog.events.SupplierOrderDeliveredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
class SupplierResilienceScheduler {

    private final SupplierOrderRepository orderRepository;
    private final LegacySupplyClient legacySupplyClient;
    private final SupplierSkuTranslator skuTranslator;
    private final ApplicationEventPublisher eventPublisher;

    SupplierResilienceScheduler(
            SupplierOrderRepository orderRepository,
            LegacySupplyClient legacySupplyClient,
            SupplierSkuTranslator skuTranslator,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.legacySupplyClient = legacySupplyClient;
        this.skuTranslator = skuTranslator;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 5000)
    public void retryPendingOrders() {
        List<SupplierOrder> pendingOrders = orderRepository.findByStatus(SupplierOrderStatus.PENDING);
        for (SupplierOrder order : pendingOrders) {
            SupplierSkuTranslator.SkuMapping mapping = skuTranslator.getMapping(order.getProductId());
            if (mapping == null) continue;

            LegacySupplyClient.PlaceOrderResponse response = legacySupplyClient.placeOrder(
                    mapping.supplierSku(),
                    order.getCases(),
                    order.getBuyerRef(),
                    order.getRequestId()
            );

            if (response.success()) {
                order.setPoNumber(response.poNumber());
                order.setStatus(mapStatusCode(response.statusCode()));
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            } else if ("E-IDEM-04".equals(response.errorCode()) || "E-SKU-02".equals(response.errorCode())) {
                order.setStatus(SupplierOrderStatus.FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            }
        }
    }

    @Scheduled(fixedDelay = 20000, initialDelay = 10000)
    public void pollOpenPurchaseOrders() {
        List<SupplierOrder> openOrders = orderRepository.findByStatusIn(List.of(
                SupplierOrderStatus.ACCEPTED,
                SupplierOrderStatus.SUBMITTED,
                SupplierOrderStatus.PICKING,
                SupplierOrderStatus.SHIPPED
        ));

        for (SupplierOrder order : openOrders) {
            if (order.getPoNumber() == null || order.getPoNumber().isBlank()) continue;

            LegacySupplyClient.StatusCheckResponse response = legacySupplyClient.checkOrderStatus(order.getPoNumber());
            if (response.success()) {
                SupplierOrderStatus newStatus = mapStatusCode(response.statusCode());
                if (newStatus != order.getStatus()) {
                    order.setStatus(newStatus);
                    order.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(order);

                    if (newStatus == SupplierOrderStatus.DELIVERED) {
                        eventPublisher.publishEvent(new SupplierOrderDeliveredEvent(
                                order.getProductId(),
                                order.getUnits(),
                                order.getBuyerRef(),
                                order.getPoNumber()
                        ));
                    }
                }
            }
        }
    }

    private SupplierOrderStatus mapStatusCode(int statusCode) {
        return switch (statusCode) {
            case 10 -> SupplierOrderStatus.ACCEPTED;
            case 20 -> SupplierOrderStatus.PICKING;
            case 30 -> SupplierOrderStatus.SHIPPED;
            case 40 -> SupplierOrderStatus.DELIVERED;
            case 90 -> SupplierOrderStatus.CANCELLED;
            default -> SupplierOrderStatus.SUBMITTED;
        };
    }
}

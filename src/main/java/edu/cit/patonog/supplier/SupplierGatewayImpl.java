package edu.cit.patonog.supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
class SupplierGatewayImpl implements SupplierGateway {

    private final SupplierOrderRepository orderRepository;
    private final LegacySupplyClient legacySupplyClient;
    private final SupplierSkuTranslator skuTranslator;

    SupplierGatewayImpl(
            SupplierOrderRepository orderRepository,
            LegacySupplyClient legacySupplyClient,
            SupplierSkuTranslator skuTranslator) {
        this.orderRepository = orderRepository;
        this.legacySupplyClient = legacySupplyClient;
        this.skuTranslator = skuTranslator;
    }

    @Override
    @Transactional
    public SupplierReorderResult reorder(String productId, int unitsNeeded) {
        if (productId == null || unitsNeeded <= 0) {
            return new SupplierReorderResult(false, null, null, 0, 0, SupplierOrderStatus.FAILED, "Invalid reorder parameters");
        }

        SupplierSkuTranslator.SkuMapping mapping = skuTranslator.getMapping(productId);
        int cases = skuTranslator.calculateCases(productId, unitsNeeded);
        int replenishedUnits = skuTranslator.calculateReplenishedUnits(productId, cases);

        String requestId = UUID.randomUUID().toString();
        SupplierOrder order = new SupplierOrder(productId, "TEMP", requestId, cases, replenishedUnits, SupplierOrderStatus.PENDING);
        order = orderRepository.save(order);

        String buyerRef = "RO-" + order.getId();
        order.setBuyerRef(buyerRef);
        order = orderRepository.save(order);

        if (mapping == null) {
            return new SupplierReorderResult(false, buyerRef, null, cases, replenishedUnits, SupplierOrderStatus.PENDING, "No SKU mapping for product " + productId);
        }

        LegacySupplyClient.PlaceOrderResponse response = legacySupplyClient.placeOrder(
                mapping.supplierSku(),
                cases,
                buyerRef,
                requestId
        );

        if (response.success()) {
            order.setPoNumber(response.poNumber());
            order.setStatus(mapStatusCode(response.statusCode()));
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            return new SupplierReorderResult(
                    true,
                    buyerRef,
                    response.poNumber(),
                    cases,
                    replenishedUnits,
                    order.getStatus(),
                    "Order accepted by supplier"
            );
        }

        if ("E-IDEM-04".equals(response.errorCode()) || "E-SKU-02".equals(response.errorCode()) || "E-QTY-11".equals(response.errorCode())) {
            order.setStatus(SupplierOrderStatus.FAILED);
        } else {
            order.setStatus(SupplierOrderStatus.PENDING);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return new SupplierReorderResult(
                false,
                buyerRef,
                null,
                cases,
                replenishedUnits,
                order.getStatus(),
                response.errorMessage()
        );
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

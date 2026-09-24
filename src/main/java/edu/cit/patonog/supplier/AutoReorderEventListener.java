package edu.cit.patonog.supplier;

import edu.cit.patonog.events.LowStockEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AutoReorderEventListener {

    private final SupplierGateway supplierGateway;

    AutoReorderEventListener(SupplierGateway supplierGateway) {
        this.supplierGateway = supplierGateway;
    }

    @EventListener
    public void onLowStock(LowStockEvent event) {
        supplierGateway.reorder(event.getProductId(), 15);
    }
}

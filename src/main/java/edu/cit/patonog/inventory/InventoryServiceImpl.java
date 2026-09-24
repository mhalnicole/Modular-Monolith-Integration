package edu.cit.patonog.inventory;

import edu.cit.patonog.events.LowStockEvent;
import edu.cit.patonog.events.SupplierOrderDeliveredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final int lowStockThreshold;

    InventoryServiceImpl(InventoryRepository inventoryRepository,
                         ApplicationEventPublisher eventPublisher,
                         @Value("${inventory.low-stock-threshold:5}") int lowStockThreshold) {
        this.inventoryRepository = inventoryRepository;
        this.eventPublisher = eventPublisher;
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryItem> getItem(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Optional.empty();
        }
        return inventoryRepository.findById(productId.trim());
    }

    @Override
    @Transactional
    public boolean reserve(String productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return false;
        }

        Optional<InventoryItem> optionalItem = inventoryRepository.findById(productId.trim());
        if (optionalItem.isEmpty()) {
            return false;
        }

        InventoryItem item = optionalItem.get();
        if (item.getStock() < quantity) {
            return false;
        }

        int updatedStock = item.getStock() - quantity;
        item.setStock(updatedStock);
        inventoryRepository.save(item);

        if (updatedStock <= lowStockThreshold) {
            eventPublisher.publishEvent(new LowStockEvent(item.getProductId(), item.getName(), updatedStock));
        }

        return true;
    }

    @Override
    @Transactional
    public void restock(String productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }

        Optional<InventoryItem> optionalItem = inventoryRepository.findById(productId.trim());
        if (optionalItem.isPresent()) {
            InventoryItem item = optionalItem.get();
            item.setStock(item.getStock() + quantity);
            inventoryRepository.save(item);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAllByOrderByProductIdAsc();
    }

    @EventListener
    @Transactional
    public void onSupplierOrderDelivered(SupplierOrderDeliveredEvent event) {
        if (event != null && event.getProductId() != null && event.getUnits() > 0) {
            restock(event.getProductId(), event.getUnits());
        }
    }
}

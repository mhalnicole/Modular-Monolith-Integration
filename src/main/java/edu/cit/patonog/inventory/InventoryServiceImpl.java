package edu.cit.patonog.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of InventoryService.
 * Explicitly package-private (no 'public' modifier) to enforce the modular monolith boundary.
 * The Shop/Order module is only permitted to reference the public InventoryService interface.
 */
@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
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

        item.setStock(item.getStock() - quantity);
        inventoryRepository.save(item);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAllByOrderByProductIdAsc();
    }
}

package edu.cit.patonog.inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    Optional<InventoryItem> getItem(String productId);

    boolean reserve(String productId, int quantity);

    List<InventoryItem> getAllItems();
}

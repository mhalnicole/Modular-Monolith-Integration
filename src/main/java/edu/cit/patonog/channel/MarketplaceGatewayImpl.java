package edu.cit.patonog.channel;

import edu.cit.patonog.inventory.InventoryItem;
import edu.cit.patonog.inventory.InventoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class MarketplaceGatewayImpl implements MarketplaceGateway {

    private final InventoryService inventoryService;
    private final TianggeClient tianggeClient;

    MarketplaceGatewayImpl(InventoryService inventoryService, TianggeClient tianggeClient) {
        this.inventoryService = inventoryService;
        this.tianggeClient = tianggeClient;
    }

    @Override
    public void syncStock(String productId) {
        if (productId == null || productId.isBlank()) {
            return;
        }
        Optional<InventoryItem> itemOpt = inventoryService.getItem(productId.trim());
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            tianggeClient.publishStock(List.of(new TianggeDto.StockPayload(item.getProductId(), item.getStock())));
        }
    }

    @Override
    public void syncAllStock() {
        List<InventoryItem> items = inventoryService.getAllItems();
        if (items != null && !items.isEmpty()) {
            List<TianggeDto.StockPayload> payloads = items.stream()
                    .map(i -> new TianggeDto.StockPayload(i.getProductId(), i.getStock()))
                    .toList();
            tianggeClient.publishStock(payloads);
        }
    }
}

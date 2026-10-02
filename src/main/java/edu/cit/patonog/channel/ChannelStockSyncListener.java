package edu.cit.patonog.channel;

import edu.cit.patonog.events.StockChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ChannelStockSyncListener {

    private final MarketplaceGateway marketplaceGateway;

    ChannelStockSyncListener(MarketplaceGateway marketplaceGateway) {
        this.marketplaceGateway = marketplaceGateway;
    }

    @EventListener
    public void onStockChanged(StockChangedEvent event) {
        if (event != null && event.getProductId() != null) {
            marketplaceGateway.syncStock(event.getProductId(), event.getRemainingStock());
        }
    }
}

package edu.cit.patonog.channel;

public interface MarketplaceGateway {
    void syncStock(String productId);
    void syncStock(String productId, int available);
    void syncAllStock();
}

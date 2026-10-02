package edu.cit.patonog.channel;

import edu.cit.patonog.config.AppInstanceHolder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ChannelStartupRunner implements ApplicationRunner {

    private final TianggeClient tianggeClient;
    private final AppInstanceHolder appInstanceHolder;
    private final MarketplaceGateway marketplaceGateway;

    ChannelStartupRunner(
            TianggeClient tianggeClient,
            AppInstanceHolder appInstanceHolder,
            MarketplaceGateway marketplaceGateway) {
        this.tianggeClient = tianggeClient;
        this.appInstanceHolder = appInstanceHolder;
        this.marketplaceGateway = marketplaceGateway;
    }

    @Override
    public void run(ApplicationArguments args) {
        tianggeClient.sendHeartbeat(
                "modular-shop-app",
                appInstanceHolder.getStartedAt().toString(),
                appInstanceHolder.getUptimeSeconds()
        );

        List<TianggeDto.ListingPayload> listings = List.of(
                new TianggeDto.ListingPayload("P100", "Wireless Mouse", "ZAX-1614"),
                new TianggeDto.ListingPayload("P200", "Mechanical Keyboard", "ZAX-1252"),
                new TianggeDto.ListingPayload("P300", "USB-C Hub", "ZAX-4488")
        );
        tianggeClient.publishListings(listings);

        marketplaceGateway.syncAllStock();
    }
}

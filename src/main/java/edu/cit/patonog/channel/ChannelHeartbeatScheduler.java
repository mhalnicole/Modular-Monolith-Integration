package edu.cit.patonog.channel;

import edu.cit.patonog.config.AppInstanceHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ChannelHeartbeatScheduler {

    private final TianggeClient tianggeClient;
    private final AppInstanceHolder appInstanceHolder;

    ChannelHeartbeatScheduler(TianggeClient tianggeClient, AppInstanceHolder appInstanceHolder) {
        this.tianggeClient = tianggeClient;
        this.appInstanceHolder = appInstanceHolder;
    }

    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void sendHeartbeat() {
        tianggeClient.sendHeartbeat(
                "modular-shop-app",
                appInstanceHolder.getStartedAt().toString(),
                appInstanceHolder.getUptimeSeconds()
        );
    }
}

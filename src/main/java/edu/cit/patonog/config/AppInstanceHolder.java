package edu.cit.patonog.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AppInstanceHolder {

    private final String instanceId = UUID.randomUUID().toString();
    private final Instant startedAt = Instant.now();

    public String getInstanceId() {
        return instanceId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getUptimeSeconds() {
        return Math.max(0, Instant.now().getEpochSecond() - startedAt.getEpochSecond());
    }
}

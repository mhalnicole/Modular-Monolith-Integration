package edu.cit.patonog.channel;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
class ChannelFeedPoller {

    private final TianggeClient tianggeClient;
    private final ChannelCursorRepository cursorRepository;
    private final ChannelProcessedEventRepository processedEventRepository;
    private final ChannelOrderProcessor orderProcessor;

    ChannelFeedPoller(
            TianggeClient tianggeClient,
            ChannelCursorRepository cursorRepository,
            ChannelProcessedEventRepository processedEventRepository,
            ChannelOrderProcessor orderProcessor) {
        this.tianggeClient = tianggeClient;
        this.cursorRepository = cursorRepository;
        this.processedEventRepository = processedEventRepository;
        this.orderProcessor = orderProcessor;
    }

    @Scheduled(fixedDelay = 2500, initialDelay = 4000)
    public void pollFeed() {
        long cursor = cursorRepository.findById(1)
                .map(ChannelCursor::getNextCursor)
                .orElse(0L);

        TianggeDto.FeedResponse response = tianggeClient.fetchFeed(cursor, 20);
        if (response == null || response.events() == null) {
            return;
        }

        for (TianggeDto.FeedEvent event : response.events()) {
            if (event.eventId() != null && processedEventRepository.existsById(event.eventId())) {
                continue;
            }

            if ("ORDER_PLACED".equalsIgnoreCase(event.type())) {
                orderProcessor.handleOrderPlaced(event);
            } else if ("ORDER_CANCELLED".equalsIgnoreCase(event.type())) {
                orderProcessor.handleOrderCancelled(event);
            }

            if (event.eventId() != null) {
                processedEventRepository.save(new ChannelProcessedEvent(event.eventId(), event.type()));
            }
        }

        if (response.nextCursor() != null && response.nextCursor() != cursor) {
            ChannelCursor entity = cursorRepository.findById(1)
                    .orElse(new ChannelCursor(1, response.nextCursor()));
            entity.setNextCursor(response.nextCursor());
            entity.setUpdatedAt(LocalDateTime.now());
            cursorRepository.save(entity);
        }
    }
}

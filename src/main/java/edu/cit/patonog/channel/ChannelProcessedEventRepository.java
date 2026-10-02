package edu.cit.patonog.channel;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChannelProcessedEventRepository extends JpaRepository<ChannelProcessedEvent, String> {
}

package edu.cit.patonog.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ChannelOrderRepository extends JpaRepository<ChannelOrder, String> {
    Optional<ChannelOrder> findByShopOrderId(String shopOrderId);
    List<ChannelOrder> findByStatus(String status);
}

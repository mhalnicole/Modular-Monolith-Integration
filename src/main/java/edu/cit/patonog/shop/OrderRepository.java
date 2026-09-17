package edu.cit.patonog.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status, o.reason = :reason WHERE o.orderId = :orderId")
    void updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status, @Param("reason") String reason);
}

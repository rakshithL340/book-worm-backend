package com.bookworm.order.repository;

import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Order> findByOrderIdAndUserUserId(UUID orderId, UUID userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.userId = :userId AND o.status = :status")
    long countOrdersByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);
}

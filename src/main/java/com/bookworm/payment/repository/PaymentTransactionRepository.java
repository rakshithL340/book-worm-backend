package com.bookworm.payment.repository;

import com.bookworm.payment.entity.PaymentStatus;
import com.bookworm.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByOrderOrderId(UUID orderId);

    Optional<PaymentTransaction> findByOrderOrderIdAndStatus(UUID orderId, PaymentStatus status);

    Optional<PaymentTransaction> findByGatewayRefId(String gatewayRefId);
}

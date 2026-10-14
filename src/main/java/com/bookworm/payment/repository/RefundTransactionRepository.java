package com.bookworm.payment.repository;

import com.bookworm.payment.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {

    List<RefundTransaction> findByOrderOrderId(UUID orderId);

    Optional<RefundTransaction> findByPaymentTransactionTransactionId(UUID transactionId);
}

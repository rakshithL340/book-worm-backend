package com.bookworm.payment.service;

import com.bookworm.payment.dto.PaymentInitiateRequest;
import com.bookworm.payment.dto.PaymentInitiateResponse;
import com.bookworm.payment.dto.WalletBalanceResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

    PaymentInitiateResponse processPayment(UUID userId, PaymentInitiateRequest request);

    WalletBalanceResponse getWalletBalance(UUID userId);

    WalletBalanceResponse topUpWallet(UUID userId, BigDecimal amount);
}

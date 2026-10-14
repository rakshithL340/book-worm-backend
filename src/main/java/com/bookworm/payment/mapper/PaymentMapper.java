package com.bookworm.payment.mapper;

import com.bookworm.payment.dto.PaymentInitiateResponse;
import com.bookworm.payment.dto.WalletBalanceResponse;
import com.bookworm.payment.entity.PaymentTransaction;
import com.bookworm.payment.entity.WalletAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "status", expression = "java(tx.getStatus() != null ? tx.getStatus().name() : null)")
    @Mapping(target = "message", constant = "Payment completed successfully")
    PaymentInitiateResponse toInitiateResponse(PaymentTransaction tx);

    WalletBalanceResponse toWalletResponse(WalletAccount account);
}

package com.bookworm.common.exception;

public class InsufficientWalletBalanceException extends BaseBusinessException {

    public InsufficientWalletBalanceException(String message) {
        super(ErrorCode.INSUFFICIENT_WALLET_BALANCE, message);
    }
}

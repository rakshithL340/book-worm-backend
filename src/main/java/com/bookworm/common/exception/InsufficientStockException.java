package com.bookworm.common.exception;

public class InsufficientStockException extends BaseBusinessException {

    public InsufficientStockException(String message) {
        super(ErrorCode.INSUFFICIENT_STOCK, message);
    }
}

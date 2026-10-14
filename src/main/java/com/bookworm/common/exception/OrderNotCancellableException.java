package com.bookworm.common.exception;

public class OrderNotCancellableException extends BaseBusinessException {

    public OrderNotCancellableException(String message) {
        super(ErrorCode.ORDER_NOT_CANCELLABLE, message);
    }
}

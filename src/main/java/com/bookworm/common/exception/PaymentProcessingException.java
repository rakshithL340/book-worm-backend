package com.bookworm.common.exception;

public class PaymentProcessingException extends BaseBusinessException {

    public PaymentProcessingException(String message) {
        super(ErrorCode.PAYMENT_FAILED, message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(ErrorCode.PAYMENT_FAILED, message, cause);
    }
}

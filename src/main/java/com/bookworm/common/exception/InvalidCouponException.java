package com.bookworm.common.exception;

public class InvalidCouponException extends BaseBusinessException {

    public InvalidCouponException(String message) {
        super(ErrorCode.INVALID_COUPON, message);
    }
}

package com.soma.coupon.module.coupon.dto;

public record IssueCouponRequest(
        Long memberId,
        Long couponId
) {
}

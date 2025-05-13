package com.soma.coupon.module.coupon.dto;

public record UseCouponRequest(
        Long memberCouponId,
        Long memberId
) {
}

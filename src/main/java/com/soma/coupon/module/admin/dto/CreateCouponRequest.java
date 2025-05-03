package com.soma.coupon.module.admin.dto;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.CouponType;
import java.time.LocalDateTime;

public record CreateCouponRequest(
        String name,
        Long count,
        CouponType couponType,
        LocalDateTime expireTime
) {
    public Coupon toDomain() {
        return new Coupon(name, count, couponType, expireTime);
    }
}

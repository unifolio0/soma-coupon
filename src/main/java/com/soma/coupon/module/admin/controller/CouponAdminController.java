package com.soma.coupon.module.admin.controller;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponService couponService;

    @PostMapping("api/admin/coupon")
    public Coupon createCoupon(@RequestBody CreateCouponRequest request) {
        return couponService.create(request);
    }
}

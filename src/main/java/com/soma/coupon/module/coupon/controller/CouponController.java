package com.soma.coupon.module.coupon.controller;

import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("api/coupon/issue")
    public void issueCoupon(@RequestBody IssueCouponRequest request) {
        couponService.issue(request);
    }
}

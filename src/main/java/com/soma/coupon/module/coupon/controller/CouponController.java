package com.soma.coupon.module.coupon.controller;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.service.CouponService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("api/coupon/issue")
    public MemberCoupon issueCoupon(@RequestBody IssueCouponRequest request) {
        return couponService.issue(request);
    }

    @GetMapping("api/coupon")
    public List<Coupon> getCoupons() {
        return couponService.getCoupons();
    }

    @GetMapping("api/coupon/{userId}")
    public List<MemberCoupon> getCoupons(@PathVariable Long userId) {
        return couponService.getUserCoupons(userId);
    }

    @PatchMapping("api/coupon/used")
    public MemberCoupon useCoupon(@RequestBody UseCouponRequest request) {
        return couponService.use(request);
    }
}

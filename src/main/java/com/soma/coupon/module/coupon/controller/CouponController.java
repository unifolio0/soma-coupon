package com.soma.coupon.module.coupon.controller;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.service.CouponService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("api/coupon/issue/redis")
    public MemberCoupon issueCouponForRedis(@RequestBody IssueCouponRequest request) {
        return couponService.issueForRedisLock(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("api/coupon/used/redis")
    public MemberCoupon useCouponForRedis(@RequestBody UseCouponRequest request) {
        return couponService.useForRedisLock(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("api/coupon/issue/xlock")
    public MemberCoupon issueCouponForXLock(@RequestBody IssueCouponRequest request) {
        return couponService.issueForXLock(request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("api/coupon/used/xlock")
    public MemberCoupon useCouponForXLock(@RequestBody UseCouponRequest request) {
        return couponService.useForXLock(request);
    }

    @GetMapping("api/coupon")
    public List<Coupon> getCoupons() {
        return couponService.getCoupons();
    }

    @GetMapping("api/coupon/{userId}")
    public List<MemberCoupon> getCoupons(@PathVariable Long userId) {
        return couponService.getUserCoupons(userId);
    }
}

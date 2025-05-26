package com.soma.coupon.module.coupon.controller;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.service.CouponService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("api/coupon/issue/redis")
    public MemberCoupon issueCouponForRedis(@RequestBody IssueCouponRequest request) {
        log.info("redis api start memberId: {}", request.userId());
        return couponService.issueForRedisLock(request);
    }

    @PostMapping("api/coupon/used/redis")
    public MemberCoupon useCouponForRedis(@RequestBody UseCouponRequest request) {
        return couponService.useForRedisLock(request);
    }

    @PostMapping("api/coupon/issue/xlock")
    public MemberCoupon issueCouponForXLock(@RequestBody IssueCouponRequest request) {
        log.info("x lock api start memberId: {}", request.userId());
        return couponService.issueForXLock(request);
    }

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

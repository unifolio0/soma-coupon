package com.soma.coupon.module.coupon.tool;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import com.soma.coupon.module.coupon.repository.MemberCouponRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponReader {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    public List<MemberCoupon> getUserCoupons(Long userId) {
        return memberCouponRepository.findByMemberId(userId);
    }

    public List<Coupon> getCoupons() {
        return couponRepository.findAll();
    }

    public Coupon getCouponByIdForUpdate(Long couponId) {
        return couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. 쿠폰 id: " + couponId));
    }

    public Coupon getCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. 쿠폰 id: " + couponId));
    }
}

package com.soma.coupon.module.coupon.tool;

import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberCouponReader {

    private final MemberCouponRepository memberCouponRepository;

    public void alreadyIssued(Long couponId, Long memberId) {
        if (memberCouponRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다");
        }
    }

    public MemberCoupon getMemberCouponById(Long memberCouponId) {
        return memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. 쿠폰 id: " + memberCouponId));
    }

    public MemberCoupon getMemberCouponByIdForUpdate(Long memberCouponId) {
        return memberCouponRepository.findByIdForUpdate(memberCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다. 쿠폰 id: " + memberCouponId));
    }
}

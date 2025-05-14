package com.soma.coupon.module.coupon.tool;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import com.soma.coupon.module.coupon.repository.MemberCouponRepository;
import com.soma.coupon.module.user.domain.Member;
import com.soma.coupon.module.user.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponWriter {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;

    public Coupon create(Coupon coupon) {
        return couponRepository.save(coupon);
    }
  
    @Transactional
    public MemberCoupon issueForXLock(IssueCouponRequest request) {
        if (memberCouponRepository.existsByCouponIdAndMemberId(request.couponId(), request.userId())) {
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다");
        }
        Coupon coupon = couponRepository.findById(request.couponId()).orElseThrow();
        if (!coupon.issuable() || coupon.isExpired()) {
            throw new IllegalArgumentException("모두 소진된 쿠폰입니다.");
        }
        coupon.decrease();
        Member member = memberRepository.findById(request.userId()).orElseThrow();
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        return memberCouponRepository.save(memberCoupon);
    }  

    @Transactional
    public MemberCoupon issueForRedisLock(IssueCouponRequest request) {
        if (memberCouponRepository.existsByCouponIdAndMemberId(request.couponId(), request.userId())) {
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다");
        }
        Coupon coupon = couponRepository.findByIdForUpdate(request.couponId()).orElseThrow();
        if (!coupon.issuable() || coupon.isExpired()) {
            throw new IllegalArgumentException("모두 소진된 쿠폰입니다.");
        }
        coupon.decrease();
        Member member = memberRepository.findById(request.userId()).orElseThrow();
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        return memberCouponRepository.save(memberCoupon);
    }

    public List<MemberCoupon> getUserCoupons(Long userId) {
        return memberCouponRepository.findByMemberId(userId);
    }

    public List<Coupon> getCoupons() {
        return couponRepository.findAll();
    }

    @Transactional
    public MemberCoupon usedForXLock(Long memberCouponId, Long memberId) {
        MemberCoupon memberCoupon = memberCouponRepository.findByIdForUpdate(memberCouponId).orElseThrow();
        if (!memberCoupon.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("쿠폰을 발급받은 회원이 아닙니다.");
        }
        if (!memberCoupon.usable()) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다.");
        }
        memberCoupon.use();
        return memberCoupon;
    }
  
    @Transactional
    public MemberCoupon usedForRedisLock(Long memberCouponId, Long memberId) {
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId).orElseThrow();
        if (!memberCoupon.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("쿠폰을 발급받은 회원이 아닙니다.");
        }
        if (!memberCoupon.usable()) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다.");
        }
        memberCoupon.use();
        return memberCoupon;
    }
}

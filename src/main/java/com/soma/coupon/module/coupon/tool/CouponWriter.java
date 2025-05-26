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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
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
        log.info("쿠폰 발급 내역 조회 memberId: {}", request.userId());
        if (memberCouponRepository.existsByCouponIdAndMemberId(request.couponId(), request.userId())) {
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다");
        }
        log.info("x lock 획득 시도 memberId: {}", request.userId());
        Coupon coupon = couponRepository.findByIdForUpdate(request.couponId()).orElseThrow();
        log.info("x lock 획득 memberId: {}", request.userId());
        return issueCoupon(request, coupon);
    }

    @Transactional
    public MemberCoupon issueForRedisLock(IssueCouponRequest request) {
        log.info("쿠폰 발급 내역 조회 memberId: {}", request.userId());
        if (memberCouponRepository.existsByCouponIdAndMemberId(request.couponId(), request.userId())) {
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다");
        }
        Coupon coupon = couponRepository.findById(request.couponId()).orElseThrow();
        log.info("coupon 조회 memberId: {}", request.userId());
        return issueCoupon(request, coupon);
    }

    private MemberCoupon issueCoupon(IssueCouponRequest request, Coupon coupon) {
        if (!coupon.issuable()) {
            throw new IllegalArgumentException("모두 소진된 쿠폰입니다.");
        }
        if (coupon.isExpired()) {
            throw new IllegalArgumentException("쿠폰이 만료되었습니다.");
        }
        coupon.decrease();
        log.info("coupon 갯수 감소 memberId: {}", request.userId());
        Member member = memberRepository.findById(request.userId()).orElseThrow();
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        log.info("memberCoupon 저장 시도 memberId: {}", request.userId());
        MemberCoupon savedMemberCoupon = memberCouponRepository.save(memberCoupon);
        log.info("쿠폰 발급 완료 memberId: {}", request.userId());
        return savedMemberCoupon;
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
        if (!memberCoupon.isOwner(memberId)) {
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
        if (!memberCoupon.isOwner(memberId)) {
            throw new IllegalArgumentException("쿠폰을 발급받은 회원이 아닙니다.");
        }
        if (!memberCoupon.usable()) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다.");
        }
        memberCoupon.use();
        return memberCoupon;
    }
}

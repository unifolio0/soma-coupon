package com.soma.coupon.module.coupon.service;

import com.soma.coupon.common.redis.DistributedLock;
import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.tool.CouponReader;
import com.soma.coupon.module.coupon.tool.CouponWriter;
import com.soma.coupon.module.coupon.tool.MemberCouponReader;
import com.soma.coupon.module.coupon.tool.MemberCouponWriter;
import com.soma.coupon.module.user.domain.Member;
import com.soma.coupon.module.user.tool.MemberReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final CouponReader couponReader;
    private final MemberReader memberReader;
    private final MemberCouponReader memberCouponReader;
    private final MemberCouponWriter memberCouponWriter;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponWriter.create(coupon);
    }

    @Transactional
    public MemberCoupon issueForXLock(IssueCouponRequest request) {
        log.info("쿠폰 발급 내역 조회 memberId: {}", request.userId());
        memberCouponReader.alreadyIssued(request.userId(), request.couponId());
        Member member = memberReader.getMemberById(request.userId());
        log.info("x lock 획득 시도 memberId: {}", member.getId());
        Coupon coupon = couponReader.getCouponByIdForUpdate(request.couponId());
        log.info("x lock 획득 memberId: {}", member.getId());
        coupon.validateIssuable();
        coupon.decrease();
        log.info("coupon 갯수 감소 memberId: {}", member.getId());
        log.info("memberCoupon 저장 시도 memberId: {}", member.getId());
        MemberCoupon savedMemberCoupon = memberCouponWriter.save(member, coupon);
        log.info("쿠폰 발급 완료 memberId: {}", member.getId());
        return savedMemberCoupon;
    }

    @DistributedLock(key = "#request.couponId()")
    @Transactional
    public MemberCoupon issueForRedisLock(IssueCouponRequest request) {
        log.info("쿠폰 발급 내역 조회 memberId: {}", request.userId());
        memberCouponReader.alreadyIssued(request.userId(), request.couponId());
        Member member = memberReader.getMemberById(request.userId());
        Coupon coupon = couponReader.getCouponById(request.couponId());
        coupon.validateIssuable();
        coupon.decrease();
        log.info("coupon 갯수 감소 memberId: {}", member.getId());
        log.info("memberCoupon 저장 시도 memberId: {}", member.getId());
        MemberCoupon savedMemberCoupon = memberCouponWriter.save(member, coupon);
        log.info("쿠폰 발급 완료 memberId: {}", member.getId());
        return savedMemberCoupon;
    }

    public List<MemberCoupon> getUserCoupons(Long userId) {
        return couponReader.getUserCoupons(userId);
    }

    public List<Coupon> getCoupons() {
        return couponReader.getCoupons().stream()
                .filter(coupon -> !coupon.isExpired())
                .toList();
    }

    @Transactional
    public MemberCoupon useForXLock(UseCouponRequest request) {
        MemberCoupon memberCoupon = memberCouponReader.getMemberCouponByIdForUpdate(request.memberCouponId());
        memberCoupon.validateUsable(request.memberId());
        memberCoupon.use();
        return memberCoupon;
    }

    @DistributedLock(key = "'couponId:' + #request.memberCouponId() + ':memberId:' + #request.memberId()")
    @Transactional
    public MemberCoupon useForRedisLock(UseCouponRequest request) {
        MemberCoupon memberCoupon = memberCouponReader.getMemberCouponById(request.memberCouponId());
        memberCoupon.validateUsable(request.memberId());
        memberCoupon.use();
        return memberCoupon;
    }
}

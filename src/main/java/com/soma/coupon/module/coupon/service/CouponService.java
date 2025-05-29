package com.soma.coupon.module.coupon.service;

import com.soma.coupon.common.redis.lock.DistributedLock;
import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.tool.CouponReader;
import com.soma.coupon.module.coupon.tool.CouponRedisManager;
import com.soma.coupon.module.coupon.tool.CouponWriter;
import com.soma.coupon.module.coupon.tool.MemberCouponReader;
import com.soma.coupon.module.coupon.tool.MemberCouponWriter;
import com.soma.coupon.module.user.domain.Member;
import com.soma.coupon.module.user.tool.MemberReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final CouponReader couponReader;
    private final MemberReader memberReader;
    private final MemberCouponReader memberCouponReader;
    private final MemberCouponWriter memberCouponWriter;
    private final CouponRedisManager couponRedisManager;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        couponRedisManager.cachingCoupon(coupon);
        return couponWriter.create(coupon);
    }

    @Transactional
    public MemberCoupon issueForXLock(IssueCouponRequest request) {
        memberCouponReader.alreadyIssued(request.userId(), request.couponId());
        Member member = memberReader.getMemberById(request.userId());
        Coupon coupon = couponReader.getCouponByIdForUpdate(request.couponId());
        coupon.validateIssuable();
        coupon.decrease();
        return memberCouponWriter.save(member, coupon);
    }

    @DistributedLock(key = "#request.couponId()")
    @Transactional
    public MemberCoupon issueForRedisLock(IssueCouponRequest request) {
        memberCouponReader.alreadyIssued(request.userId(), request.couponId());
        Member member = memberReader.getMemberById(request.userId());
        Coupon coupon = couponReader.getCouponById(request.couponId());
        coupon.validateIssuable();
        coupon.decrease();
        return memberCouponWriter.save(member, coupon);
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

    @DistributedLock(key = "'memberId:' + #request.memberId() + ':couponId:' + #request.memberCouponId()")
    @Transactional
    public MemberCoupon useForRedisLock(UseCouponRequest request) {
        MemberCoupon memberCoupon = memberCouponReader.getMemberCouponById(request.memberCouponId());
        memberCoupon.validateUsable(request.memberId());
        memberCoupon.use();
        return memberCoupon;
    }
}

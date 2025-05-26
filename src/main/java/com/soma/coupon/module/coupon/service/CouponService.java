package com.soma.coupon.module.coupon.service;

import com.soma.coupon.common.redis.DistributedLock;
import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.tool.CouponReader;
import com.soma.coupon.module.coupon.tool.CouponWriter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final CouponReader couponReader;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponWriter.create(coupon);
    }

    public MemberCoupon issueForXLock(IssueCouponRequest request) {
        return couponWriter.issueForXLock(request);
    }

    @DistributedLock(key = "#request.couponId()")
    public MemberCoupon issueForRedisLock(IssueCouponRequest request) {
        return couponWriter.issueForRedisLock(request);
    }

    public List<MemberCoupon> getUserCoupons(Long userId) {
        return couponReader.getUserCoupons(userId);
    }

    public List<Coupon> getCoupons() {
        return couponReader.getCoupons().stream()
                .filter(coupon -> !coupon.isExpired())
                .toList();
    }

    public MemberCoupon useForXLock(UseCouponRequest request) {
        return couponWriter.usedForXLock(request.memberCouponId(), request.memberId());
    }

    @DistributedLock(key = "'couponId:' + #request.memberCouponId() + ':memberId:' + #request.memberId()")
    public MemberCoupon useForRedisLock(UseCouponRequest request) {
        return couponWriter.usedForRedisLock(request.memberCouponId(), request.memberId());
    }
}

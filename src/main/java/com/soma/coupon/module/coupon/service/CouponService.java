package com.soma.coupon.module.coupon.service;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.tool.CouponWriter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final RedissonClient redissonClient;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponWriter.create(coupon);
    }

    public MemberCoupon issueForXLock(IssueCouponRequest request) {
        return couponWriter.issueForXLock(request);
    }

    public MemberCoupon issueForRedisLock(IssueCouponRequest request) {
        String lockName = request.couponId() + ":lock";
        RLock lock = redissonClient.getLock(lockName);
        log.info("redis lock 획득 시도 memberId: {}", request.userId());
        lock.lock();
        log.info("redis lock 획득 memberId: {}", request.userId());
        try {
            return couponWriter.issueForRedisLock(request);
        } finally {
            log.info("redis unlock memberId: {}", request.userId());
            lock.unlock();
        }
    }

    public List<MemberCoupon> getUserCoupons(Long userId) {
        return couponWriter.getUserCoupons(userId);
    }

    public List<Coupon> getCoupons() {
        return couponWriter.getCoupons().stream()
                .filter(coupon -> !coupon.isExpired())
                .toList();
    }

    public MemberCoupon useForXLock(UseCouponRequest request) {
        return couponWriter.usedForXLock(request.memberCouponId(), request.memberId());
    }

    public MemberCoupon useForRedisLock(UseCouponRequest request) {
        String lockName = "couponId:" + request.memberCouponId() + "memberId:" + request.memberId() + ":lock";
        RLock lock = redissonClient.getLock(lockName);
        lock.lock();
        try {
            return couponWriter.usedForRedisLock(request.memberCouponId(), request.memberId());
        } finally {
            lock.unlock();
        }
    }
}

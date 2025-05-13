package com.soma.coupon.module.coupon.service;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.tool.CouponWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponWriter couponWriter;
    private final RedissonClient redissonClient;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponWriter.create(coupon);
    }

    public MemberCoupon issue(IssueCouponRequest request) {
        String lockName = request.couponId() + ":lock";
        RLock lock = redissonClient.getLock(lockName);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalArgumentException("락 획득 실패");
            }
            return couponWriter.issue(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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

    public MemberCoupon use(Long id) {
        String lockName = id + ":lock";
        RLock lock = redissonClient.getLock(lockName);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalArgumentException("락 획득 실패");
            }
            return couponWriter.used(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

package com.soma.coupon.module.coupon.tool;

import com.soma.coupon.module.coupon.domain.Coupon;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponRedisManager {

    private static final String COUPON_KEY_FORMAT = "coupon:%s";
    private static final String MEMBER_COUPON_KEY_FORMAT = "member:%s:coupon:%s";

    private final RedissonClient redissonClient;

    public void cachingCoupon(Coupon coupon) {
        RAtomicLong bucket = redissonClient.getAtomicLong(String.format(COUPON_KEY_FORMAT, coupon.getId()));
        bucket.set(coupon.getCount());
    }

    public void isProcessing(Long memberId, Long couponId) {
        RBucket<String> bucket = redissonClient.getBucket(String.format(MEMBER_COUPON_KEY_FORMAT, memberId, couponId));
        if (!bucket.setIfAbsent("1", Duration.ofSeconds(10))) {
            throw new IllegalArgumentException("해당 작업이 진행 중 입니다.");
        }
    }

    public void decreaseCount(Coupon coupon) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(String.format(COUPON_KEY_FORMAT, coupon.getId()));
        if (atomicLong.decrementAndGet() < 0) {
            throw new IllegalArgumentException("모두 소진된 쿠폰입니다.");
        }
    }

    public long getCouponCount(Long couponId) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(String.format(COUPON_KEY_FORMAT, couponId));
        return atomicLong.get();
    }
}

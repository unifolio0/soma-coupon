package com.soma.coupon.module.coupon.tool;

import com.soma.coupon.module.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponRedisManager {

    private static final String COUPON_KEY_FORMAT = "coupon:%s";

    private final RedissonClient redissonClient;

    public void cachingCoupon(Coupon coupon) {
        RBucket<Long> bucket = redissonClient.getBucket(String.format(COUPON_KEY_FORMAT, coupon.getId()));
        bucket.set(coupon.getCount());
    }
}

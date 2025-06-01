package com.soma.coupon.module.coupon.tool;

import com.soma.coupon.module.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponAsyncWriter {

    private final CouponRepository couponRepository;

    @Async
    @Transactional
    public void decreaseAvailableCount(Long couponId) {
        couponRepository.decreaseAvailableCount(couponId);
    }
}

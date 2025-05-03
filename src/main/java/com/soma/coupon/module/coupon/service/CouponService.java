package com.soma.coupon.module.coupon.service;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Cacheable(cacheNames = "cacheName", key = "#request")
    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponRepository.save(coupon);
    }

    public void issue(IssueCouponRequest request) {

    }
}

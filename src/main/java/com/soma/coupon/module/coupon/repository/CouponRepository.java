package com.soma.coupon.module.coupon.repository;

import com.soma.coupon.module.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}

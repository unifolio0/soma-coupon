package com.soma.coupon.module.coupon.repository;

import com.soma.coupon.module.coupon.domain.UserCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    List<UserCoupon> findByUserId(Long userId);
}

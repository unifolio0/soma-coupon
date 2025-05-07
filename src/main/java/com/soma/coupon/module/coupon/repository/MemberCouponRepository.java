package com.soma.coupon.module.coupon.repository;

import com.soma.coupon.module.coupon.domain.MemberCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);

    List<MemberCoupon> findByMemberId(Long memberId);
}

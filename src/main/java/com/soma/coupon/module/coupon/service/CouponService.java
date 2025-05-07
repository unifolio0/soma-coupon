package com.soma.coupon.module.coupon.service;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import com.soma.coupon.module.coupon.repository.MemberCouponRepository;
import com.soma.coupon.module.user.domain.Member;
import com.soma.coupon.module.user.repository.MemberRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final RedissonClient redissonClient;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponRepository.save(coupon);
    }

    @Transactional
    public MemberCoupon issue(IssueCouponRequest request) {
        String lockName = request.couponId() + ":lock";
        RLock lock = redissonClient.getLock(lockName);

        try {
            if (!lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException();
            }
            if (memberCouponRepository.existsByCouponIdAndMemberId(request.couponId(), request.userId())) {
                throw new IllegalArgumentException();
            }
            Coupon coupon = couponRepository.findById(request.couponId()).orElseThrow();
            if (!coupon.issuable() || coupon.isExpired()) {
                throw new IllegalArgumentException();
            }
            coupon.decrease();
            Member member = memberRepository.findById(request.userId()).orElseThrow();
            MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
            return memberCouponRepository.save(memberCoupon);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    public List<MemberCoupon> getUserCoupons(Long userId) {
        return memberCouponRepository.findByMemberId(userId);
    }

    public List<Coupon> getCoupons() {
        return couponRepository.findAll().stream()
                .filter(coupon -> !coupon.isExpired())
                .toList();
    }

    @Transactional
    public MemberCoupon used(Long id) {
        String lockName = id + ":lock";
        RLock lock = redissonClient.getLock(lockName);
        try {
            if (!lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException();
            }
            MemberCoupon memberCoupon = memberCouponRepository.findById(id).orElseThrow();
            if (!memberCoupon.usable()) {
                throw new IllegalArgumentException();
            }
            memberCoupon.use();
            return memberCoupon;
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }
    }
}

package com.soma.coupon.module.coupon.service;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.UserCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import com.soma.coupon.module.coupon.repository.UserCouponRepository;
import com.soma.coupon.module.user.domain.User;
import com.soma.coupon.module.user.repository.UserRepository;
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
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;

    public Coupon create(CreateCouponRequest request) {
        Coupon coupon = request.toDomain();
        return couponRepository.save(coupon);
    }

    @Transactional
    public UserCoupon issue(IssueCouponRequest request) {
        String lockName = request.couponId() + ":lock";
        RLock lock = redissonClient.getLock(lockName);

        try {
            if (!lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException();
            }
            if (userCouponRepository.existsByCouponIdAndUserId(request.couponId(), request.userId())) {
                throw new IllegalArgumentException();
            }
            Coupon coupon = couponRepository.findById(request.couponId()).orElseThrow();
            if (!coupon.issuable() || coupon.isExpired()) {
                throw new IllegalArgumentException();
            }
            coupon.decrease();
            User user = userRepository.findById(request.userId()).orElseThrow();
            UserCoupon userCoupon = new UserCoupon(user, coupon);
            return userCouponRepository.save(userCoupon);
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    public List<UserCoupon> getUserCoupons(Long userId) {
        return userCouponRepository.findByUserId(userId);
    }

    public List<Coupon> getCoupons() {
        return couponRepository.findAll().stream()
                .filter(coupon -> !coupon.isExpired())
                .toList();
    }

    @Transactional
    public UserCoupon used(Long id) {
        String lockName = id + ":lock";
        RLock lock = redissonClient.getLock(lockName);
        try {
            if (!lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException();
            }
            UserCoupon userCoupon = userCouponRepository.findById(id).orElseThrow();
            if (!userCoupon.usable()) {
                throw new IllegalArgumentException();
            }
            userCoupon.use();
            return userCoupon;
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }
    }
}

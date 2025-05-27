package com.soma.coupon.module.coupon.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long count;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private LocalDateTime expireTime;

    public Coupon(String name, Long count, CouponType couponType, LocalDateTime expireTime) {
        this.name = name;
        this.count = count;
        this.couponType = couponType;
        this.expireTime = expireTime;
    }

    public void decrease() {
        count--;
    }

    public void validateIssuable() {
        if (isExpired()) {
            throw new IllegalArgumentException("쿠폰이 만료되었습니다.");
        }
        if (!issuable()) {
            throw new IllegalArgumentException("모두 소진된 쿠폰입니다.");
        }
    }

    public boolean isExpired() {
        return expireTime.isBefore(LocalDateTime.now());
    }

    private boolean issuable() {
        return count > 0;
    }
}

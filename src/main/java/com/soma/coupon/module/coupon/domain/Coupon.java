package com.soma.coupon.module.coupon.domain;

import jakarta.persistence.Entity;
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

    private CouponType couponType;

    private LocalDateTime expireTime;

    public Coupon(String name, Long count, CouponType couponType, LocalDateTime expireTime) {
        this.name = name;
        this.count = count;
        this.couponType = couponType;
        this.expireTime = expireTime;
    }

    public boolean isExpired() {
        return expireTime.isBefore(LocalDateTime.now());
    }

    public boolean issuable() {
        return count > 0;
    }

    public void decrease() {
        count--;
    }
}

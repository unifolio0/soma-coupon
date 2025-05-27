package com.soma.coupon.module.coupon.domain;

import com.soma.coupon.module.user.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    private Boolean used;
    private LocalDateTime usedAt;

    public MemberCoupon(Member member, Coupon coupon) {
        this.member = member;
        this.coupon = coupon;
        this.used = false;
        this.usedAt = LocalDateTime.now();
    }

    public void use() {
        used = true;
        usedAt = LocalDateTime.now();
    }

    public void validateUsable(Long memberId) {
        if (!isOwner(memberId)) {
            throw new IllegalArgumentException("쿠폰을 발급받은 회원이 아닙니다.");
        }
        if (!usable()) {
            throw new IllegalArgumentException("쿠폰을 사용할 수 없습니다.");
        }
    }

    private boolean isOwner(Long memberId) {
        return member.getId().equals(memberId);
    }

    private boolean usable() {
        return !used && !coupon.isExpired();
    }
}

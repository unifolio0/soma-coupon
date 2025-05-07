package com.soma.coupon.module.user.repository;

import com.soma.coupon.module.user.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

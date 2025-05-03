package com.soma.coupon.module.user.repository;

import com.soma.coupon.module.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

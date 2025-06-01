package com.soma.coupon.module.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.soma.coupon.module.admin.dto.CreateCouponRequest;
import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.CouponType;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.dto.UseCouponRequest;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import com.soma.coupon.module.coupon.repository.MemberCouponRepository;
import com.soma.coupon.module.coupon.tool.CouponRedisManager;
import com.soma.coupon.module.user.domain.Member;
import com.soma.coupon.module.user.domain.Role;
import com.soma.coupon.module.user.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CouponServiceTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponRedisManager couponRedisManager;

    @Test
    @DisplayName("여러 명의 회원이 쿠폰 발급을 동시에 신청해도, 발급 갯수만큼만 발급되어야 한다.")
    void createMemberCouponForRedisLockByManyUsers() throws InterruptedException {
        List<Member> members = new ArrayList<>();
        int memberCount = 800;
        long couponCount = 1000;
        for (int i = 0; i < memberCount; i++) {
            Member member = memberRepository.save(new Member("member" + i, "pw", Role.MEMBER));
            members.add(member);
        }
        Coupon coupon = couponService.create(
                new CreateCouponRequest(
                        "치킨 쿠폰",
                        couponCount,
                        CouponType.CHICKEN,
                        LocalDateTime.now().plusDays(1))
        );

        ExecutorService executor = Executors.newFixedThreadPool(memberCount);
        CountDownLatch latch = new CountDownLatch(memberCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < memberCount; i++) {
            Member member = members.get(i);
            executor.submit(() -> {
                try {
                    couponService.issueForRedisLock(new IssueCouponRequest(member.getId(), coupon.getId()));
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Coupon usedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();

        assertThat(couponRedisManager.getCouponCount(usedCoupon.getId())).isEqualTo(
                Math.max(couponCount - memberCount, 0));
    }

    @Test
    @DisplayName("회원이 자신의 쿠폰을 동시에 여러 번 사용하려고 시도하면 예외가 발생한다.")
    void useForRedisLockMemberCouponConcurrently() throws InterruptedException {
        Member member = new Member("member", "pw", Role.MEMBER);
        memberRepository.save(member);
        Coupon coupon = new Coupon("치킨 쿠폰", 10L, CouponType.CHICKEN, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    couponService.useForRedisLock(new UseCouponRequest(memberCoupon.getId(), member.getId()));
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(exceptions).hasSize(threadCount - 1).allMatch(IllegalArgumentException.class::isInstance);
    }
}

package com.soma.coupon.module.coupon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.soma.coupon.module.coupon.domain.Coupon;
import com.soma.coupon.module.coupon.domain.CouponType;
import com.soma.coupon.module.coupon.domain.MemberCoupon;
import com.soma.coupon.module.coupon.dto.IssueCouponRequest;
import com.soma.coupon.module.coupon.repository.CouponRepository;
import com.soma.coupon.module.coupon.repository.MemberCouponRepository;
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

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CouponWriterTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("여러 명의 회원이 쿠폰 발급을 동시에 신청해도, 발급 갯수만큼만 발급되어야 한다.")
    void createMemberCouponByManyUsers() throws InterruptedException {
        List<Member> members = new ArrayList<>();
        int memberCount = 100;
        long couponCount = 10;
        for (int i = 0; i < memberCount; i++) {
            Member member = memberRepository.save(new Member("member" + i, "pw", Role.MEMBER));
            members.add(member);
        }
        Coupon coupon = new Coupon("vhjvj", couponCount, CouponType.CHICKEN, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);

        ExecutorService executor = Executors.newFixedThreadPool(memberCount);
        CountDownLatch latch = new CountDownLatch(memberCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < memberCount; i++) {
            Member member = members.get(i);
            executor.submit(() -> {
                long start = System.currentTimeMillis();
                try {
                    couponService.issue(new IssueCouponRequest(member.getId(), coupon.getId()));
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    long end = System.currentTimeMillis();
                    System.out.printf("Thread for member %d took %d ms%n", member.getId(), (end - start));
                    latch.countDown();
                }
            });
        }

        latch.await();
        exceptions.forEach(e -> System.out.println(e.getMessage()));

        Coupon usedCoupon = couponRepository.findById(coupon.getId()).get();
        assertEquals(memberCount - couponCount, exceptions.size(), "100개 성공, 900개 실패");
        assertEquals(usedCoupon.getCount(), 0, "쿠폰은 모두 소진되어 0개 남음");
        assertTrue(exceptions.stream().allMatch(e -> e.getMessage().equals("모두 소진된 쿠폰입니다.")), "모든 예외는 쿠폰 소진에 의해 발생");
    }

    @Test
    @DisplayName("회원이 자신의 쿠폰을 동시에 여러 번 사용하려고 시도하면 예외가 발생한다.")
    void useMemberCouponConcurrently() throws InterruptedException {
        // given
        Member member = new Member("member", "pw", Role.MEMBER);
        memberRepository.save(member);
        Coupon coupon = new Coupon("vhjvj", 10L, CouponType.CHICKEN, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    couponService.use(memberCoupon.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        assertEquals(threadCount - 1, exceptions.size(), "1개 성공, 나머지는 실패해야 함");
        assertTrue(exceptions.stream().allMatch(IllegalArgumentException.class::isInstance
        ), "모든 실패는 IllegalArgumentException 이어야 함");
    }
}

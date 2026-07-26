package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseParticipantRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class GroupPurchaseConcurrencyTest {

    @Autowired
    private GroupPurchaseLockFacade groupPurchaseLockFacade;

    @Autowired
    private GroupPurchaseRepository groupPurchaseRepository;

    @Autowired
    private GroupPurchaseParticipantRepository groupPurchaseParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupPurchaseCategoryRepository groupPurchaseCategoryRepository;

    private Long savedGroupPurchaseId;
    private Long categoryId;
    private Long creatorId;

    @BeforeEach
    void setUp() {
        // 1. 유저 100명 미리 생성
        for (int i = 0; i < 100; i++) {
            User user = User.builder()
                    .email("test_concurrent_" + i + "_" + System.currentTimeMillis() + "@test.com")
                    .password("1234")
                    .username("user_concurrent_" + i)
                    .build();
            userRepository.save(user);
        }

        // 2. 카테고리 생성
        Category category = Category.builder().name("테스트식품" + System.currentTimeMillis()).build();
        Category savedCategory = groupPurchaseCategoryRepository.save(category);
        categoryId = savedCategory.getId();

        // 3. 개설자 가져오기
        User creator = userRepository.findByEmail("test_concurrent_0_" + System.currentTimeMillis() + "@test.com").orElseGet(() -> userRepository.findAll().get(0));
        creatorId = creator.getId();

        // 4. 공동구매 생성(정원 100명)
        GroupPurchase gp = GroupPurchase.builder()
                .creatorId(creatorId)
                .categoryId(categoryId)
                .title("동시성 테스트 공구")
                .content("동시성 테스트 공구입니다.")
                .price(10000)
                .minParticipants(10)
                .maxParticipants(100)
                .deadline(LocalDateTime.now().plusDays(7))
                .pickupLocation("테스트 장소")
                .build();
        GroupPurchase savedGp = groupPurchaseRepository.save(gp);
        savedGroupPurchaseId = savedGp.getId();
    }

    @AfterEach
    void tearDown() {
        // FK 제약 조건 때문에 일괄 삭제 생략
    }

    @Test
    @DisplayName("동시성 제어 테스트: 100명의 유저가 동시에 참여 버튼을 누른다")
    void joinGroupPurchase_concurrency_test() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 유저 100명 가져오기(각각 다른 유저가 참여)
        var users = userRepository.findAll();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            Long userId = users.get(i).getId();
            executorService.submit(() -> {
                try {
                    groupPurchaseLockFacade.joinGroupPurchase(userId, savedGroupPurchaseId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        GroupPurchase gp = groupPurchaseRepository.findById(savedGroupPurchaseId).orElseThrow();
        
        System.out.println("성공한 요청 수: " + successCount.get());
        System.out.println("실패한 요청 수: " + failCount.get());
        System.out.println("실제 DB에 기록된 참여자 수: " + gp.getCurrentParticipants());
        
        // 성공한 요청 수와 DB에 기록된 참여자 수가 일치해야 함
        assertThat(gp.getCurrentParticipants()).isEqualTo(successCount.get());
    }
}

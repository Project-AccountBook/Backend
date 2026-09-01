package com.chaewookim.accountbookformoms.benchmark;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.notification.dao.NotificationRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.InterestCategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Tag("benchmark")
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Rollback(false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DbIoBenchmark {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("benchdb")
            .withUsername("bench")
            .withPassword("benchpw")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("serverTimezone", "UTC")
            .withUrlParam("rewriteBatchedStatements", "true");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.properties.hibernate.jdbc.batch_size",
                () -> System.getProperty("bench.batch.size", "20"));
        r.add("spring.jpa.properties.hibernate.jdbc.batch_versioned_data", () -> "true");
        r.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
        r.add("spring.jpa.properties.hibernate.order_updates", () -> "true");
        r.add("spring.jpa.properties.hibernate.default_batch_fetch_size", () -> "100");
        r.add("spring.jpa.show-sql", () -> "false");
        r.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        r.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
        // 배제 (DataJpaTest 는 기본적으로 최소한만 로드하지만 확실히 하기 위해)
        r.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
              + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
              + "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration,"
              + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,"
              + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration,"
              + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration");
    }

    @Autowired BoardRepository boardRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired InterestCategoryRepository interestCategoryRepository;
    @Autowired FixedTransactionRepository fixedTransactionRepository;
    @Autowired PlatformTransactionManager txManager;
    @PersistenceContext EntityManager em;
    @PersistenceUnit EntityManagerFactory emf;

    private Statistics stats() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    private void resetStats() {
        Statistics s = stats();
        s.setStatisticsEnabled(true);
        s.clear();
    }

    private long queryCount() {
        // prepared statements (each SQL execute)
        return stats().getPrepareStatementCount();
    }

    static final int BOARD_COUNT = 10_000;
    static final int COMMENT_COUNT = 100_000;
    static final int USER_COUNT = 100;

    // R3~R5, U2 시드 규모
    static final int NOTIF_USER_COUNT = 100;
    static final int NOTIF_PER_USER = 200;                 // R3: 100 * 200 = 20,000
    static final int USER_DEVICE_COUNT = 10_000;           // R4: user_id 는 1..10000 랜덤(FK 없음)
    static final int ACCOUNT_USER_COUNT = 100;
    static final int ACCOUNT_PER_USER = 30;                // R5: 100 * 30 = 3,000
    static final int U2_USER_COUNT = 200;                  // U2: 200 유저 각 200 알림 = 40,000
    static final int U2_NOTIF_PER_USER = 200;
    // U2 유저는 R3 시드 범위 이후로 배정 (충돌 방지)
    static final long U2_USER_ID_BASE = 1000L;             // 1000 ~ 1199

    // R6, R7 시드 규모
    static final int GP_COUNT = 10_000;                    // R6: group_purchase 10,000 rows
    static final int GP_CATEGORY_MAX = 50;                 // category_id 1..50
    static final int GP_CREATOR_MAX = 100;                 // creator_id 1..100
    static final int TXCAT_USER_COUNT = 100;               // R7: 100 users
    static final int TXCAT_PER_USER = 20;                  // R7: 100 * 20 = 2,000 rows
    // R3 별도 알림 시드에서 사용할 user_id 범위: 1..100 (Board 시드와 겹쳐도 무관, FK 없음)

    // U3, U4 시드 규모
    static final int U3_USER_COUNT = 60;                   // U3: user 1..60, each 100 interest_category = 6,000 rows
    static final int U3_IC_PER_USER = 100;
    static final int GP_CATEGORY_SEED_COUNT = 20;          // group_purchase_category 20 rows
    static final int U4_ACCOUNT_COUNT = 60;                // U4: 60 accounts (신규, R5 account 와 별개)
    static final int U4_FIXED_PER_ACCOUNT = 100;           // 60 * 100 = 6,000 fixed_transaction rows
    static final int U4_TXCAT_MAX = 20;                    // transaction_category_id 1..20 재사용 (R7 시드)
    // U4 accounts 를 user_id 1..60 에 걸쳐 시드. account_id 는 auto-increment (R5 의 3000 이후로 배정됨)

    static final Path CSV_PATH =
            Paths.get("docs/benchmark/results/2026-08-05_db_io.csv");

    @BeforeAll
    void seed() throws IOException {
        System.out.println("=== [BENCH] Testcontainers MySQL URL: " + MYSQL.getJdbcUrl());
        System.out.println("=== [BENCH] hibernate.jdbc.batch_size = "
                + System.getProperty("bench.batch.size", "20"));

        ensureCsvHeader();

        // 시드는 batch=1 실행일 때는 스킵 (이미 첫 실행에서 seed 됐다고 가정하지 않고,
        // 매 실행마다 새 컨테이너이므로 항상 시드해야 함). 실제로 컨테이너는 매 JVM 마다 새로 생김.
        long boardExisting = boardRepository.count();
        if (boardExisting >= BOARD_COUNT) {
            System.out.println("=== [BENCH] Seed skipped (board rows already " + boardExisting + ")");
            return;
        }

        long t0 = System.currentTimeMillis();
        seedUsers();
        long tU = System.currentTimeMillis();
        System.out.println("=== [BENCH] User seed done: " + (tU - t0) + " ms");

        seedBoards();
        long t1 = System.currentTimeMillis();
        System.out.println("=== [BENCH] Board seed done: " + (t1 - tU) + " ms");

        seedComments();
        long t2 = System.currentTimeMillis();
        System.out.println("=== [BENCH] Comment seed done: " + (t2 - t1) + " ms");

        seedNotifications();
        long t3 = System.currentTimeMillis();
        System.out.println("=== [BENCH] Notification seed done: " + (t3 - t2) + " ms");

        seedUserDevices();
        long t4 = System.currentTimeMillis();
        System.out.println("=== [BENCH] UserDevice seed done: " + (t4 - t3) + " ms");

        seedAccounts();
        long t5 = System.currentTimeMillis();
        System.out.println("=== [BENCH] Account seed done: " + (t5 - t4) + " ms");

        seedU2Notifications();
        long t6 = System.currentTimeMillis();
        System.out.println("=== [BENCH] U2 Notification seed done: " + (t6 - t5) + " ms");

        seedGroupPurchases();
        long t7 = System.currentTimeMillis();
        System.out.println("=== [BENCH] GroupPurchase seed done: " + (t7 - t6) + " ms");

        seedTransactionCategories();
        long t8 = System.currentTimeMillis();
        System.out.println("=== [BENCH] TransactionCategory seed done: " + (t8 - t7) + " ms");

        seedGpCategories();
        long t9 = System.currentTimeMillis();
        System.out.println("=== [BENCH] GpCategory seed done: " + (t9 - t8) + " ms");

        seedInterestCategories();
        long t10 = System.currentTimeMillis();
        System.out.println("=== [BENCH] InterestCategory seed done: " + (t10 - t9) + " ms");

        seedU4AccountsAndFixedTx();
        long t11 = System.currentTimeMillis();
        System.out.println("=== [BENCH] U4 Account + FixedTransaction seed done: " + (t11 - t10) + " ms");

        // ANALYZE
        runInTx(() -> {
            em.createNativeQuery("ANALYZE TABLE board").getResultList();
            em.createNativeQuery("ANALYZE TABLE comment").getResultList();
            em.createNativeQuery("ANALYZE TABLE notification").getResultList();
            em.createNativeQuery("ANALYZE TABLE user_device").getResultList();
            em.createNativeQuery("ANALYZE TABLE account").getResultList();
            em.createNativeQuery("ANALYZE TABLE group_purchase").getResultList();
            em.createNativeQuery("ANALYZE TABLE transaction_category").getResultList();
            em.createNativeQuery("ANALYZE TABLE group_purchase_category").getResultList();
            em.createNativeQuery("ANALYZE TABLE interest_category").getResultList();
            em.createNativeQuery("ANALYZE TABLE fixed_transaction").getResultList();
        });
        System.out.println("=== [BENCH] ANALYZE done");
    }

    // R6 시드: group_purchase 10,000 rows
    // - status: PurchaseStatus enum (RECRUITING/SUCCESS/FAILED/CLOSED 균등)
    // - category_id 1..50, creator_id 1..100, deadline 랜덤 미래
    // creator_id 는 raw Long (FK 없음) 이므로 1..100 이면 됨
    private void seedGroupPurchases() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            Random rnd = new Random(606);
            String[] statuses = {"RECRUITING", "SUCCESS", "FAILED", "CLOSED"};
            for (int i = 1; i <= GP_COUNT; i++) {
                long creatorId = rnd.nextInt(GP_CREATOR_MAX) + 1;
                long categoryId = rnd.nextInt(GP_CATEGORY_MAX) + 1;
                String status = statuses[i % 4];
                // deadline: now + random(1..365) days
                int daysAhead = rnd.nextInt(365) + 1;
                int price = 1000 + rnd.nextInt(50000);
                int minP = 2 + rnd.nextInt(5);
                int maxP = minP + 5 + rnd.nextInt(20);
                em.createNativeQuery(
                        "INSERT INTO group_purchase "
                      + "(creator_id, category_id, title, content, price, min_participants, "
                      + " max_participants, current_participants, status, deadline, pickup_location, "
                      + " view_count, created_at, updated_at) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, DATE_ADD(NOW(), INTERVAL ? DAY), ?, 0, NOW(), NOW())")
                        .setParameter(1, creatorId)
                        .setParameter(2, categoryId)
                        .setParameter(3, "gp-title-" + i)
                        .setParameter(4, "gp-content-" + i)
                        .setParameter(5, price)
                        .setParameter(6, minP)
                        .setParameter(7, maxP)
                        .setParameter(8, status)
                        .setParameter(9, daysAhead)
                        .setParameter(10, "pickup-loc-" + (i % 20))
                        .executeUpdate();
                if (i % 1000 == 0) {
                    em.flush();
                    em.clear();
                    System.out.println("  ... group_purchases inserted " + i);
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // R7 시드: transaction_category 2,000 rows
    // - user_id 1..100 (FK: user 테이블. 이미 seedUsers 로 1..1200 시드됨)
    // - name = "cat_{seq}", seq 0..19 (per user)
    // - type = INCOME/EXPENSE/TRANSFER 순환
    private void seedTransactionCategories() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            String[] types = {"INCOME", "EXPENSE", "TRANSFER"};
            int total = 0;
            for (long uid = 1; uid <= TXCAT_USER_COUNT; uid++) {
                for (int k = 0; k < TXCAT_PER_USER; k++) {
                    String type = types[(int) ((uid + k) % 3)];
                    em.createNativeQuery(
                            "INSERT INTO transaction_category "
                          + "(user_id, name, type, created_at, updated_at) "
                          + "VALUES (?, ?, ?, NOW(), NOW())")
                            .setParameter(1, uid)
                            .setParameter(2, "cat_" + k)
                            .setParameter(3, type)
                            .executeUpdate();
                    total++;
                    if (total % 500 == 0) {
                        em.flush();
                        em.clear();
                        System.out.println("  ... transaction_categories inserted " + total);
                    }
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // U3 지원: group_purchase_category 20 rows (InterestCategory.category_id FK)
    private void seedGpCategories() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            for (int i = 1; i <= GP_CATEGORY_SEED_COUNT; i++) {
                em.createNativeQuery(
                        "INSERT INTO group_purchase_category "
                      + "(name, sort_order, description, created_at, updated_at) "
                      + "VALUES (?, ?, ?, NOW(), NOW())")
                        .setParameter(1, "gp-cat-" + i)
                        .setParameter(2, i)
                        .setParameter(3, "gp-cat-desc-" + i)
                        .executeUpdate();
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // U3 시드: interest_category = 60 users * 100 = 6,000 rows
    // user_id 1..60, category_id 1..20 순환
    private void seedInterestCategories() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            int total = 0;
            for (long uid = 1; uid <= U3_USER_COUNT; uid++) {
                for (int k = 0; k < U3_IC_PER_USER; k++) {
                    long catId = (k % GP_CATEGORY_SEED_COUNT) + 1;
                    em.createNativeQuery(
                            "INSERT INTO interest_category "
                          + "(user_id, category_id, is_alarm_enabled, created_at, updated_at) "
                          + "VALUES (?, ?, false, NOW(), NOW())")
                            .setParameter(1, uid)
                            .setParameter(2, catId)
                            .executeUpdate();
                    total++;
                    if (total % 2000 == 0) {
                        em.flush();
                        em.clear();
                        System.out.println("  ... interest_categories inserted " + total);
                    }
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // U4 시드:
    //  1) account 60개 신규 (user_id 1..60, R5 accounts 뒤에 auto-increment 로 배정)
    //     → 배정된 account_id 를 List 로 수집해 상수로 노출
    //  2) 각 account 마다 fixed_transaction 100건 → 6,000 rows
    //     transaction_category_id 1..20 (R7 시드 재사용)
    private final List<Long> u4AccountIds = new ArrayList<>();
    private void seedU4AccountsAndFixedTx() {
        // 1) accounts 신규 60개
        {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            TransactionStatus tx = txManager.getTransaction(def);
            try {
                for (int i = 0; i < U4_ACCOUNT_COUNT; i++) {
                    long uid = i + 1; // user_id 1..60
                    Object idObj = em.createNativeQuery(
                            "INSERT INTO account (user_id, account_name, initial_balance, current_balance, role, kind, "
                          + "goal_achieved_notified, created_at, updated_at) "
                          + "VALUES (?, ?, 0, 0, 'CHECKING', 'ASSET', false, NOW(), NOW())")
                            .setParameter(1, uid)
                            .setParameter(2, "u4-acc-" + uid)
                            .executeUpdate();
                    // executeUpdate() 는 int (row count) 반환. auto-increment 값을 얻기 위해 별도 SELECT.
                    Object lastId = em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
                    u4AccountIds.add(((Number) lastId).longValue());
                }
                em.flush();
                em.clear();
                txManager.commit(tx);
            } catch (RuntimeException ex) {
                txManager.rollback(tx);
                throw ex;
            }
            System.out.println("  ... U4 accounts seeded, ids=" + u4AccountIds.get(0) + ".." + u4AccountIds.get(u4AccountIds.size()-1));
        }
        // 2) fixed_transaction 6000건
        {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            TransactionStatus tx = txManager.getTransaction(def);
            try {
                String[] types = {"INCOME", "EXPENSE", "TRANSFER"};
                int total = 0;
                for (int i = 0; i < u4AccountIds.size(); i++) {
                    long accountId = u4AccountIds.get(i);
                    long uid = i + 1; // 매칭된 owner user_id
                    for (int k = 0; k < U4_FIXED_PER_ACCOUNT; k++) {
                        long catId = (k % U4_TXCAT_MAX) + 1; // 1..20 (R7 시드 - user_id 1..100 에 걸쳐 존재)
                        String type = types[k % 3];
                        em.createNativeQuery(
                                "INSERT INTO fixed_transaction "
                              + "(user_id, account_id, category_id, type, amount, frequency, repeat_day, "
                              + " repeat_month, start_date, end_date, last_executed_date, next_execution_date, "
                              + " description, is_active, created_at, updated_at) "
                              + "VALUES (?, ?, ?, ?, ?, 'MONTHLY', 1, NULL, CURDATE(), NULL, NULL, "
                              + "        DATE_ADD(CURDATE(), INTERVAL 30 DAY), ?, true, NOW(), NOW())")
                                .setParameter(1, uid)
                                .setParameter(2, accountId)
                                .setParameter(3, catId)
                                .setParameter(4, type)
                                .setParameter(5, 1000 + k)
                                .setParameter(6, "u4-ft-" + accountId + "-" + k)
                                .executeUpdate();
                        total++;
                        if (total % 2000 == 0) {
                            em.flush();
                            em.clear();
                            System.out.println("  ... fixed_transactions inserted " + total);
                        }
                    }
                }
                em.flush();
                em.clear();
                txManager.commit(tx);
            } catch (RuntimeException ex) {
                txManager.rollback(tx);
                throw ex;
            }
        }
    }

    // User 필수 컬럼: email (unique, notnull), username (notnull), role (notnull enum),
    //                provider (notnull enum), created_at, updated_at (BaseEntity, notnull)
    // U2 유저까지 포함해 넉넉히 시드 (id 1..1300 커버). identity PK 라 AUTO_INCREMENT 로 순차 배정됨.
    private void seedUsers() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            // 필요한 최대 user_id = max(NOTIF_USER_COUNT, ACCOUNT_USER_COUNT, U2_USER_ID_BASE + U2_USER_COUNT)
            int maxNeeded = (int) Math.max(
                    Math.max(NOTIF_USER_COUNT, ACCOUNT_USER_COUNT),
                    U2_USER_ID_BASE + U2_USER_COUNT);
            // Board 시드와 겹치지만 FK 없어 무관. UserDevice 는 raw user_id 1..10000 랜덤이라 굳이 다 만들 필요 X.
            for (int i = 1; i <= maxNeeded; i++) {
                em.createNativeQuery(
                        "INSERT INTO user (email, password, username, role, provider, created_at, updated_at) "
                      + "VALUES (?, ?, ?, 'ROLE_USER', 'LOCAL', NOW(), NOW())")
                        .setParameter(1, "bench-" + i + "@example.com")
                        .setParameter(2, "pw")
                        .setParameter(3, "user-" + i)
                        .executeUpdate();
                if (i % 500 == 0) {
                    em.flush();
                    em.clear();
                    System.out.println("  ... users inserted " + i);
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void seedNotifications() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            int total = 0;
            for (long uid = 1; uid <= NOTIF_USER_COUNT; uid++) {
                for (int k = 0; k < NOTIF_PER_USER; k++) {
                    em.createNativeQuery(
                            "INSERT INTO notification (user_id, type, title, message, is_read, created_at, updated_at) "
                          + "VALUES (?, 'SYSTEM', ?, ?, false, NOW(), NOW())")
                            .setParameter(1, uid)
                            .setParameter(2, "n-title-" + uid + "-" + k)
                            .setParameter(3, "n-msg-" + uid + "-" + k)
                            .executeUpdate();
                    total++;
                    if (total % 5000 == 0) {
                        em.flush();
                        em.clear();
                        System.out.println("  ... notifications inserted " + total);
                    }
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void seedUserDevices() {
        // NOTE: user_device.user_id 에 실제 JPA FK (FK_user_device_user) 가 생성되므로
        // 시드된 user 범위 (1..seedUsers 최대값) 내에서 랜덤 배정한다.
        int maxUserId = (int) Math.max(
                Math.max(NOTIF_USER_COUNT, ACCOUNT_USER_COUNT),
                U2_USER_ID_BASE + U2_USER_COUNT);
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            Random rnd = new Random(1337);
            for (int i = 1; i <= USER_DEVICE_COUNT; i++) {
                long uid = rnd.nextInt(maxUserId) + 1;
                em.createNativeQuery(
                        "INSERT INTO user_device (user_id, fcm_token) VALUES (?, ?)")
                        .setParameter(1, uid)
                        .setParameter(2, "fcm-token-" + i)
                        .executeUpdate();
                if (i % 2000 == 0) {
                    em.flush();
                    em.clear();
                    System.out.println("  ... user_devices inserted " + i);
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void seedAccounts() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            int total = 0;
            for (long uid = 1; uid <= ACCOUNT_USER_COUNT; uid++) {
                for (int k = 0; k < ACCOUNT_PER_USER; k++) {
                    em.createNativeQuery(
                            "INSERT INTO account (user_id, account_name, initial_balance, current_balance, role, kind, "
                          + "goal_achieved_notified, created_at, updated_at) "
                          + "VALUES (?, ?, 0, 0, 'CHECKING', 'ASSET', false, NOW(), NOW())")
                            .setParameter(1, uid)
                            .setParameter(2, "acc-" + uid + "-" + k)
                            .executeUpdate();
                    total++;
                    if (total % 1000 == 0) {
                        em.flush();
                        em.clear();
                        System.out.println("  ... accounts inserted " + total);
                    }
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // U2 전용: user_id 1000..1199 각 사용자당 200 알림
    private void seedU2Notifications() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            int total = 0;
            for (int u = 0; u < U2_USER_COUNT; u++) {
                long uid = U2_USER_ID_BASE + u;
                for (int k = 0; k < U2_NOTIF_PER_USER; k++) {
                    em.createNativeQuery(
                            "INSERT INTO notification (user_id, type, title, message, is_read, created_at, updated_at) "
                          + "VALUES (?, 'SYSTEM', ?, ?, false, NOW(), NOW())")
                            .setParameter(1, uid)
                            .setParameter(2, "u2-title-" + uid + "-" + k)
                            .setParameter(3, "u2-msg-" + uid + "-" + k)
                            .executeUpdate();
                    total++;
                    if (total % 5000 == 0) {
                        em.flush();
                        em.clear();
                        System.out.println("  ... u2_notifications inserted " + total);
                    }
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void seedBoards() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            Random rnd = new Random(42);
            String[] types = {"QNA", "KNOWHOW"};
            for (int i = 1; i <= BOARD_COUNT; i++) {
                long userId = (i % USER_COUNT) + 1;
                String type = types[i % 2];
                String title = "title-" + i + "-" + rnd.nextInt(100000);
                String content = "content-body-" + rnd.nextInt(1_000_000);
                em.createNativeQuery(
                        "INSERT INTO board (user_id, category_id, title, content, type, views, "
                      + "admin_deleted, is_resolved, is_urgent, created_at, updated_at) "
                      + "VALUES (?, 1, ?, ?, ?, 0, false, false, false, NOW(), NOW())")
                        .setParameter(1, userId)
                        .setParameter(2, title)
                        .setParameter(3, content)
                        .setParameter(4, type)
                        .executeUpdate();
                if (i % 1000 == 0) {
                    em.flush();
                    em.clear();
                    System.out.println("  ... boards inserted " + i);
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void seedComments() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            Random rnd = new Random(7);
            int perBoard = COMMENT_COUNT / BOARD_COUNT; // 10
            int total = 0;
            for (long boardId = 1; boardId <= BOARD_COUNT; boardId++) {
                for (int k = 0; k < perBoard; k++) {
                    long userId = rnd.nextInt(USER_COUNT) + 1;
                    String content = "c-" + boardId + "-" + k;
                    em.createNativeQuery(
                            "INSERT INTO comment (user_id, reference_id, reference_type, parent_id, "
                          + "content, admin_deleted, is_accepted, created_at, updated_at) "
                          + "VALUES (?, ?, 'QNA', NULL, ?, false, false, NOW(), NOW())")
                            .setParameter(1, userId)
                            .setParameter(2, boardId)
                            .setParameter(3, content)
                            .executeUpdate();
                    total++;
                    if (total % 5000 == 0) {
                        em.flush();
                        em.clear();
                        System.out.println("  ... comments inserted " + total);
                    }
                }
            }
            em.flush();
            em.clear();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void runInTx(Runnable r) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            r.run();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // ============================================================
    // R1: Board index (user_id, type)
    // ============================================================
    @Test
    @Order(1)
    void r1_boardIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R1")) {
            System.out.println("=== [BENCH] R1 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R1 Board index effect ===");

        // EXPLAIN
        runInTx(() -> {
            System.out.println("[R1] EXPLAIN index_used:");
            explain("EXPLAIN SELECT * FROM board WHERE user_id=1 AND type='QNA' LIMIT 100");
            System.out.println("[R1] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT * FROM board IGNORE INDEX (idx_board_user_type, idx_board_type) "
                  + "WHERE user_id=1 AND type='QNA' LIMIT 100");
        });

        Random rnd = new Random(101);
        Result idxRes = benchQuery(
                "SELECT * FROM board WHERE user_id=?1 AND type=?2 LIMIT 100",
                rnd, 20, 100);
        Result scanRes = benchQuery(
                "SELECT * FROM board IGNORE INDEX (idx_board_user_type, idx_board_type) "
              + "WHERE user_id=?1 AND type=?2 LIMIT 100",
                new Random(101), 20, 100);

        appendCsv("R1", "index_used", "-", idxRes);
        appendCsv("R1", "full_scan", "-", scanRes);
        printResult("R1 index_used", idxRes);
        printResult("R1 full_scan", scanRes);
    }

    private Result benchQuery(String sql, Random rnd, int warmup, int iters) {
        String[] types = {"QNA", "KNOWHOW"};
        // warmup
        for (int i = 0; i < warmup; i++) {
            long uid = rnd.nextInt(USER_COUNT) + 1;
            String t = types[i % 2];
            runInTx(() -> em.createNativeQuery(sql)
                    .setParameter(1, uid)
                    .setParameter(2, t)
                    .getResultList());
        }
        resetStats();
        long[] samples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = rnd.nextInt(USER_COUNT) + 1;
            String t = types[i % 2];
            long t0 = System.nanoTime();
            runInTx(() -> em.createNativeQuery(sql)
                    .setParameter(1, uid)
                    .setParameter(2, t)
                    .getResultList());
            samples[i] = System.nanoTime() - t0;
        }
        long sqlCount = queryCount();
        return Result.of(samples, sqlCount);
    }

    // ============================================================
    // R2: Comment index (user_id, created_at)
    // ============================================================
    @Test
    @Order(2)
    void r2_commentIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R2")) {
            System.out.println("=== [BENCH] R2 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R2 Comment index effect ===");

        runInTx(() -> {
            System.out.println("[R2] EXPLAIN index_used:");
            explain("EXPLAIN SELECT * FROM comment WHERE user_id=1 ORDER BY created_at DESC LIMIT 20");
            System.out.println("[R2] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT * FROM comment IGNORE INDEX (idx_comment_user) "
                  + "WHERE user_id=1 ORDER BY created_at DESC LIMIT 20");
        });

        Random rnd = new Random(202);
        Result idxRes = benchCommentQuery(
                "SELECT * FROM comment WHERE user_id=?1 ORDER BY created_at DESC LIMIT 20",
                rnd, 20, 100);
        Result scanRes = benchCommentQuery(
                "SELECT * FROM comment IGNORE INDEX (idx_comment_user) "
              + "WHERE user_id=?1 ORDER BY created_at DESC LIMIT 20",
                new Random(202), 20, 100);

        appendCsv("R2", "index_used", "-", idxRes);
        appendCsv("R2", "full_scan", "-", scanRes);
        printResult("R2 index_used", idxRes);
        printResult("R2 full_scan", scanRes);
    }

    private Result benchCommentQuery(String sql, Random rnd, int warmup, int iters) {
        for (int i = 0; i < warmup; i++) {
            long uid = rnd.nextInt(USER_COUNT) + 1;
            runInTx(() -> em.createNativeQuery(sql).setParameter(1, uid).getResultList());
        }
        resetStats();
        long[] samples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = rnd.nextInt(USER_COUNT) + 1;
            long t0 = System.nanoTime();
            runInTx(() -> em.createNativeQuery(sql).setParameter(1, uid).getResultList());
            samples[i] = System.nanoTime() - t0;
        }
        long sqlCount = queryCount();
        return Result.of(samples, sqlCount);
    }

    // ============================================================
    // U1: JDBC batch effect on updates
    // ============================================================
    @Test
    @Order(3)
    void u1_jdbcBatch() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("U1")) {
            System.out.println("=== [BENCH] U1 skipped");
            return;
        }
        int batchSize = Integer.parseInt(System.getProperty("bench.batch.size", "20"));
        System.out.println("\n=== [BENCH] U1 JDBC batch effect (batch_size=" + batchSize + ") ===");

        // warmup 5
        for (int i = 0; i < 5; i++) runU1Once(i);
        resetStats();
        long[] samples = new long[20];
        for (int i = 0; i < 20; i++) {
            long t0 = System.nanoTime();
            runU1Once(i + 1000);
            samples[i] = System.nanoTime() - t0;
        }
        long sqlCount = queryCount();
        Result res = Result.of(samples, sqlCount);
        appendCsv("U1", "batch", String.valueOf(batchSize), res);
        printResult("U1 batch_size=" + batchSize, res);
    }

    private void runU1Once(int seed) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            List<Long> ids = new ArrayList<>(200);
            for (long id = 1; id <= 200; id++) ids.add(id);
            List<Comment> comments = commentRepository.findAllById(ids);
            for (int j = 0; j < comments.size(); j++) {
                comments.get(j).update("bench-" + seed + "-" + j);
            }
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // ============================================================
    // R3: Notification (user_id) 인덱스
    // ============================================================
    @Test
    @Order(4)
    void r3_notificationIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R3")) {
            System.out.println("=== [BENCH] R3 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R3 Notification index effect ===");

        runInTx(() -> {
            System.out.println("[R3] EXPLAIN index_used:");
            explain("EXPLAIN SELECT * FROM notification WHERE user_id=1 ORDER BY id DESC LIMIT 20");
            System.out.println("[R3] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT * FROM notification IGNORE INDEX (idx_notification_user) "
                  + "WHERE user_id=1 ORDER BY id DESC LIMIT 20");
        });

        Random rnd = new Random(303);
        Result idxRes = benchSimpleUserQuery(
                "SELECT * FROM notification WHERE user_id=?1 ORDER BY id DESC LIMIT 20",
                rnd, 20, 100, NOTIF_USER_COUNT);
        Result scanRes = benchSimpleUserQuery(
                "SELECT * FROM notification IGNORE INDEX (idx_notification_user) "
              + "WHERE user_id=?1 ORDER BY id DESC LIMIT 20",
                new Random(303), 20, 100, NOTIF_USER_COUNT);

        appendCsv("R3", "index_used", "-", idxRes);
        appendCsv("R3", "full_scan", "-", scanRes);
        printResult("R3 index_used", idxRes);
        printResult("R3 full_scan", scanRes);
    }

    // ============================================================
    // R4: UserDevice (user_id) 인덱스
    // ============================================================
    @Test
    @Order(5)
    void r4_userDeviceIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R4")) {
            System.out.println("=== [BENCH] R4 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R4 UserDevice index effect ===");

        runInTx(() -> {
            System.out.println("[R4] EXPLAIN index_used:");
            explain("EXPLAIN SELECT * FROM user_device WHERE user_id=1");
            System.out.println("[R4] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT * FROM user_device IGNORE INDEX (idx_userdevice_user) WHERE user_id=1");
        });

        // 실제 user_device 는 user_id 1..1200 (seedUsers 최대) 범위 로 시드됨.
        int userSpace = (int) Math.max(
                Math.max(NOTIF_USER_COUNT, ACCOUNT_USER_COUNT),
                U2_USER_ID_BASE + U2_USER_COUNT);
        Random rnd = new Random(404);
        Result idxRes = benchSimpleUserQuery(
                "SELECT * FROM user_device WHERE user_id=?1",
                rnd, 20, 100, userSpace);
        Result scanRes = benchSimpleUserQuery(
                "SELECT * FROM user_device IGNORE INDEX (idx_userdevice_user) WHERE user_id=?1",
                new Random(404), 20, 100, userSpace);

        appendCsv("R4", "index_used", "-", idxRes);
        appendCsv("R4", "full_scan", "-", scanRes);
        printResult("R4 index_used", idxRes);
        printResult("R4 full_scan", scanRes);
    }

    // ============================================================
    // R5: Account (user_id) 인덱스
    // ============================================================
    @Test
    @Order(6)
    void r5_accountIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R5")) {
            System.out.println("=== [BENCH] R5 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R5 Account index effect ===");

        runInTx(() -> {
            System.out.println("[R5] EXPLAIN index_used:");
            explain("EXPLAIN SELECT * FROM account WHERE user_id=1");
            System.out.println("[R5] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT * FROM account IGNORE INDEX (idx_account_user, idx_account_user_name) "
                  + "WHERE user_id=1");
        });

        Random rnd = new Random(505);
        Result idxRes = benchSimpleUserQuery(
                "SELECT * FROM account WHERE user_id=?1",
                rnd, 20, 100, ACCOUNT_USER_COUNT);
        Result scanRes = benchSimpleUserQuery(
                "SELECT * FROM account IGNORE INDEX (idx_account_user, idx_account_user_name) "
              + "WHERE user_id=?1",
                new Random(505), 20, 100, ACCOUNT_USER_COUNT);

        appendCsv("R5", "index_used", "-", idxRes);
        appendCsv("R5", "full_scan", "-", scanRes);
        printResult("R5 index_used", idxRes);
        printResult("R5 full_scan", scanRes);
    }

    // 단일 user_id 파라미터 쿼리 벤치 공통
    private Result benchSimpleUserQuery(String sql, Random rnd, int warmup, int iters, int userSpace) {
        for (int i = 0; i < warmup; i++) {
            long uid = rnd.nextInt(userSpace) + 1;
            runInTx(() -> em.createNativeQuery(sql).setParameter(1, uid).getResultList());
        }
        resetStats();
        long[] samples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = rnd.nextInt(userSpace) + 1;
            long t0 = System.nanoTime();
            runInTx(() -> em.createNativeQuery(sql).setParameter(1, uid).getResultList());
            samples[i] = System.nanoTime() - t0;
        }
        long sqlCount = queryCount();
        return Result.of(samples, sqlCount);
    }

    // ============================================================
    // U2: Notification bulk soft-delete (H1 핵심)
    //   before: 200 rows 를 개별 UPDATE 200회 (옛 findAll + forEach delete 시뮬)
    //   after : softDeleteByUserId 단일 벌크 UPDATE
    // 매 iteration 마다 새 user 사용 (warmup 3 + 측정 10) * 2 variant = 26 users 소진.
    // ============================================================
    @Test
    @Order(7)
    void u2_notificationBulkDelete() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("U2")) {
            System.out.println("=== [BENCH] U2 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] U2 Notification bulk soft-delete ===");

        int warmup = 3;
        int iters = 10;

        // 사용자 인덱스 카운터: warmup 부터 순차 소진
        int[] userCursor = {0};

        // ----- BEFORE: individual UPDATE per row -----
        for (int i = 0; i < warmup; i++) {
            long uid = U2_USER_ID_BASE + userCursor[0]++;
            runU2BeforeOnce(uid);
        }
        resetStats();
        long[] beforeSamples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = U2_USER_ID_BASE + userCursor[0]++;
            long t0 = System.nanoTime();
            runU2BeforeOnce(uid);
            beforeSamples[i] = System.nanoTime() - t0;
        }
        long beforeSql = queryCount();
        Result beforeRes = Result.of(beforeSamples, beforeSql);
        appendCsv("U2", "individual_update", "-", beforeRes);
        printResult("U2 individual_update", beforeRes);

        // ----- AFTER: single bulk UPDATE via repository -----
        for (int i = 0; i < warmup; i++) {
            long uid = U2_USER_ID_BASE + userCursor[0]++;
            runU2AfterOnce(uid);
        }
        resetStats();
        long[] afterSamples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = U2_USER_ID_BASE + userCursor[0]++;
            long t0 = System.nanoTime();
            runU2AfterOnce(uid);
            afterSamples[i] = System.nanoTime() - t0;
        }
        long afterSql = queryCount();
        Result afterRes = Result.of(afterSamples, afterSql);
        appendCsv("U2", "bulk_update", "-", afterRes);
        printResult("U2 bulk_update", afterRes);
    }

    private void runU2BeforeOnce(long userId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            // 1) SELECT ids for user (한 번의 조회)
            @SuppressWarnings("unchecked")
            List<Number> ids = em.createNativeQuery(
                    "SELECT id FROM notification WHERE user_id=?1 AND deleted_at IS NULL")
                    .setParameter(1, userId)
                    .getResultList();
            // 2) 각 row 개별 UPDATE
            for (Number idNum : ids) {
                em.createNativeQuery(
                        "UPDATE notification SET deleted_at=NOW() WHERE id=?1")
                        .setParameter(1, idNum.longValue())
                        .executeUpdate();
            }
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void runU2AfterOnce(long userId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            notificationRepository.softDeleteByUserId(userId);
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // ============================================================
    // R6: GroupPurchase (status, category_id, deadline) 인덱스
    //   쿼리: WHERE status=? AND category_id=? ORDER BY deadline ASC LIMIT 20
    // ============================================================
    @Test
    @Order(8)
    void r6_groupPurchaseIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R6")) {
            System.out.println("=== [BENCH] R6 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R6 GroupPurchase index effect ===");

        runInTx(() -> {
            System.out.println("[R6] EXPLAIN index_used:");
            explain("EXPLAIN SELECT * FROM group_purchase WHERE status='RECRUITING' AND category_id=1 "
                  + "ORDER BY deadline ASC LIMIT 20");
            System.out.println("[R6] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT * FROM group_purchase IGNORE INDEX (idx_gp_status_category_deadline) "
                  + "WHERE status='RECRUITING' AND category_id=1 ORDER BY deadline ASC LIMIT 20");
        });

        Random rnd = new Random(606);
        Result idxRes = benchGpQuery(
                "SELECT * FROM group_purchase WHERE status=?1 AND category_id=?2 "
              + "ORDER BY deadline ASC LIMIT 20",
                rnd, 20, 100);
        Result scanRes = benchGpQuery(
                "SELECT * FROM group_purchase IGNORE INDEX (idx_gp_status_category_deadline) "
              + "WHERE status=?1 AND category_id=?2 ORDER BY deadline ASC LIMIT 20",
                new Random(606), 20, 100);

        appendCsv("R6", "index_used", "-", idxRes);
        appendCsv("R6", "full_scan", "-", scanRes);
        printResult("R6 index_used", idxRes);
        printResult("R6 full_scan", scanRes);
    }

    // status 는 RECRUITING 위주로 편중 (실제 조회 패턴)
    private Result benchGpQuery(String sql, Random rnd, int warmup, int iters) {
        // RECRUITING 를 50% 나머지 나눔
        String[] statuses = {"RECRUITING", "RECRUITING", "SUCCESS", "FAILED", "CLOSED"};
        for (int i = 0; i < warmup; i++) {
            String s = statuses[rnd.nextInt(statuses.length)];
            long catId = rnd.nextInt(GP_CATEGORY_MAX) + 1;
            runInTx(() -> em.createNativeQuery(sql)
                    .setParameter(1, s)
                    .setParameter(2, catId)
                    .getResultList());
        }
        resetStats();
        long[] samples = new long[iters];
        for (int i = 0; i < iters; i++) {
            String s = statuses[rnd.nextInt(statuses.length)];
            long catId = rnd.nextInt(GP_CATEGORY_MAX) + 1;
            long t0 = System.nanoTime();
            runInTx(() -> em.createNativeQuery(sql)
                    .setParameter(1, s)
                    .setParameter(2, catId)
                    .getResultList());
            samples[i] = System.nanoTime() - t0;
        }
        long sqlCount = queryCount();
        return Result.of(samples, sqlCount);
    }

    // ============================================================
    // R7: TransactionCategory (user_id, name, type) 인덱스
    //   쿼리: SELECT 1 FROM transaction_category
    //         WHERE user_id=? AND name=? AND type=? LIMIT 1
    // ============================================================
    @Test
    @Order(9)
    void r7_transactionCategoryIndex() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("R7")) {
            System.out.println("=== [BENCH] R7 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] R7 TransactionCategory exists index effect ===");

        runInTx(() -> {
            System.out.println("[R7] EXPLAIN index_used:");
            explain("EXPLAIN SELECT 1 FROM transaction_category "
                  + "WHERE user_id=1 AND name='cat_0' AND type='INCOME' LIMIT 1");
            System.out.println("[R7] EXPLAIN full_scan:");
            explain("EXPLAIN SELECT 1 FROM transaction_category IGNORE INDEX (idx_txcategory_user_name_type) "
                  + "WHERE user_id=1 AND name='cat_0' AND type='INCOME' LIMIT 1");
        });

        Random rnd = new Random(707);
        Result idxRes = benchTxCatQuery(
                "SELECT 1 FROM transaction_category "
              + "WHERE user_id=?1 AND name=?2 AND type=?3 LIMIT 1",
                rnd, 20, 100);
        Result scanRes = benchTxCatQuery(
                "SELECT 1 FROM transaction_category IGNORE INDEX (idx_txcategory_user_name_type) "
              + "WHERE user_id=?1 AND name=?2 AND type=?3 LIMIT 1",
                new Random(707), 20, 100);

        appendCsv("R7", "index_used", "-", idxRes);
        appendCsv("R7", "full_scan", "-", scanRes);
        printResult("R7 index_used", idxRes);
        printResult("R7 full_scan", scanRes);
    }

    private Result benchTxCatQuery(String sql, Random rnd, int warmup, int iters) {
        String[] types = {"INCOME", "EXPENSE", "TRANSFER"};
        for (int i = 0; i < warmup; i++) {
            long uid = rnd.nextInt(TXCAT_USER_COUNT) + 1;
            String name = "cat_" + rnd.nextInt(TXCAT_PER_USER);
            String type = types[rnd.nextInt(types.length)];
            runInTx(() -> em.createNativeQuery(sql)
                    .setParameter(1, uid)
                    .setParameter(2, name)
                    .setParameter(3, type)
                    .getResultList());
        }
        resetStats();
        long[] samples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = rnd.nextInt(TXCAT_USER_COUNT) + 1;
            String name = "cat_" + rnd.nextInt(TXCAT_PER_USER);
            String type = types[rnd.nextInt(types.length)];
            long t0 = System.nanoTime();
            runInTx(() -> em.createNativeQuery(sql)
                    .setParameter(1, uid)
                    .setParameter(2, name)
                    .setParameter(3, type)
                    .getResultList());
            samples[i] = System.nanoTime() - t0;
        }
        long sqlCount = queryCount();
        return Result.of(samples, sqlCount);
    }

    // ============================================================
    // U3: InterestCategory bulk soft-delete (D8 확장)
    //   before: 100 rows 개별 UPDATE
    //   after : softDeleteByUserId 단일 벌크 UPDATE
    // 매 iteration 마다 새 user 사용 (warmup 3 + 측정 10) * 2 variant = 26 users 소진.
    // ============================================================
    @Test
    @Order(10)
    void u3_interestCategoryBulkDelete() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("U3")) {
            System.out.println("=== [BENCH] U3 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] U3 InterestCategory bulk soft-delete ===");

        int warmup = 3;
        int iters = 10;

        // U3 시드는 user_id 1..60 사용. userCursor 는 1부터 시작 (0 아님).
        int[] userCursor = {1};

        // ----- BEFORE -----
        for (int i = 0; i < warmup; i++) {
            long uid = userCursor[0]++;
            runU3BeforeOnce(uid);
        }
        resetStats();
        long[] beforeSamples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = userCursor[0]++;
            long t0 = System.nanoTime();
            runU3BeforeOnce(uid);
            beforeSamples[i] = System.nanoTime() - t0;
        }
        long beforeSql = queryCount();
        Result beforeRes = Result.of(beforeSamples, beforeSql);
        appendCsv("U3", "individual_update", "-", beforeRes);
        printResult("U3 individual_update", beforeRes);

        // ----- AFTER -----
        for (int i = 0; i < warmup; i++) {
            long uid = userCursor[0]++;
            runU3AfterOnce(uid);
        }
        resetStats();
        long[] afterSamples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long uid = userCursor[0]++;
            long t0 = System.nanoTime();
            runU3AfterOnce(uid);
            afterSamples[i] = System.nanoTime() - t0;
        }
        long afterSql = queryCount();
        Result afterRes = Result.of(afterSamples, afterSql);
        appendCsv("U3", "bulk_update", "-", afterRes);
        printResult("U3 bulk_update", afterRes);
    }

    private void runU3BeforeOnce(long userId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            @SuppressWarnings("unchecked")
            List<Number> ids = em.createNativeQuery(
                    "SELECT id FROM interest_category WHERE user_id=?1 AND deleted_at IS NULL")
                    .setParameter(1, userId)
                    .getResultList();
            for (Number idNum : ids) {
                em.createNativeQuery(
                        "UPDATE interest_category SET deleted_at=NOW() WHERE id=?1")
                        .setParameter(1, idNum.longValue())
                        .executeUpdate();
            }
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void runU3AfterOnce(long userId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            interestCategoryRepository.softDeleteByUserId(userId);
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // ============================================================
    // U4: FixedTransaction by account_id bulk soft-delete (D8 확장)
    //   before: 100 rows 개별 UPDATE
    //   after : softDeleteByAccountId 단일 벌크 UPDATE
    // 매 iteration 마다 새 account 사용. account_id 는 seed 시 수집한 u4AccountIds.
    // ============================================================
    @Test
    @Order(11)
    void u4_fixedTransactionBulkDelete() throws IOException {
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,R3,R4,R5,R6,R7,U1,U2,U3,U4");
        if (!scenarios.contains("U4")) {
            System.out.println("=== [BENCH] U4 skipped");
            return;
        }
        System.out.println("\n=== [BENCH] U4 FixedTransaction bulk soft-delete ===");

        int warmup = 3;
        int iters = 10;
        int[] cursor = {0}; // u4AccountIds index

        // ----- BEFORE -----
        for (int i = 0; i < warmup; i++) {
            long accId = u4AccountIds.get(cursor[0]++);
            runU4BeforeOnce(accId);
        }
        resetStats();
        long[] beforeSamples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long accId = u4AccountIds.get(cursor[0]++);
            long t0 = System.nanoTime();
            runU4BeforeOnce(accId);
            beforeSamples[i] = System.nanoTime() - t0;
        }
        long beforeSql = queryCount();
        Result beforeRes = Result.of(beforeSamples, beforeSql);
        appendCsv("U4", "individual_update", "-", beforeRes);
        printResult("U4 individual_update", beforeRes);

        // ----- AFTER -----
        for (int i = 0; i < warmup; i++) {
            long accId = u4AccountIds.get(cursor[0]++);
            runU4AfterOnce(accId);
        }
        resetStats();
        long[] afterSamples = new long[iters];
        for (int i = 0; i < iters; i++) {
            long accId = u4AccountIds.get(cursor[0]++);
            long t0 = System.nanoTime();
            runU4AfterOnce(accId);
            afterSamples[i] = System.nanoTime() - t0;
        }
        long afterSql = queryCount();
        Result afterRes = Result.of(afterSamples, afterSql);
        appendCsv("U4", "bulk_update", "-", afterRes);
        printResult("U4 bulk_update", afterRes);
    }

    private void runU4BeforeOnce(long accountId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            @SuppressWarnings("unchecked")
            List<Number> ids = em.createNativeQuery(
                    "SELECT id FROM fixed_transaction WHERE account_id=?1 AND deleted_at IS NULL")
                    .setParameter(1, accountId)
                    .getResultList();
            for (Number idNum : ids) {
                em.createNativeQuery(
                        "UPDATE fixed_transaction SET deleted_at=NOW() WHERE id=?1")
                        .setParameter(1, idNum.longValue())
                        .executeUpdate();
            }
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    private void runU4AfterOnce(long accountId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus tx = txManager.getTransaction(def);
        try {
            fixedTransactionRepository.softDeleteByAccountId(accountId);
            em.flush();
            txManager.commit(tx);
        } catch (RuntimeException ex) {
            txManager.rollback(tx);
            throw ex;
        }
    }

    // ============================================================
    // Helpers
    // ============================================================
    @SuppressWarnings("unchecked")
    private void explain(String sql) {
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        for (Object[] row : rows) {
            System.out.println("    " + Arrays.toString(row));
        }
    }

    private void ensureCsvHeader() throws IOException {
        Files.createDirectories(CSV_PATH.getParent());
        if (!Files.exists(CSV_PATH)) {
            try (BufferedWriter w = Files.newBufferedWriter(CSV_PATH, StandardOpenOption.CREATE_NEW)) {
                w.write("scenario,variant,batchSize,iterations,avg_ms,p50_ms,p95_ms,min_ms,max_ms,sql_count");
                w.newLine();
            }
        }
    }

    private void appendCsv(String scenario, String variant, String batchSize, Result r) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(CSV_PATH,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(String.format(Locale.ROOT, "%s,%s,%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d",
                    scenario, variant, batchSize, r.iterations,
                    r.avgMs, r.p50Ms, r.p95Ms, r.minMs, r.maxMs, r.sqlCount));
            w.newLine();
        }
    }

    private void printResult(String label, Result r) {
        System.out.printf(Locale.ROOT,
                "  [%s] iters=%d avg=%.3fms p50=%.3fms p95=%.3fms min=%.3fms max=%.3fms sql_count=%d%n",
                label, r.iterations, r.avgMs, r.p50Ms, r.p95Ms, r.minMs, r.maxMs, r.sqlCount);
    }

    static class Result {
        int iterations;
        double avgMs, p50Ms, p95Ms, minMs, maxMs;
        long sqlCount;

        static Result of(long[] nanos, long sqlCount) {
            Result r = new Result();
            r.iterations = nanos.length;
            r.sqlCount = sqlCount;
            long[] sorted = nanos.clone();
            Arrays.sort(sorted);
            long sum = 0;
            for (long v : nanos) sum += v;
            r.avgMs = (sum / (double) nanos.length) / 1_000_000.0;
            r.minMs = sorted[0] / 1_000_000.0;
            r.maxMs = sorted[sorted.length - 1] / 1_000_000.0;
            r.p50Ms = sorted[sorted.length / 2] / 1_000_000.0;
            int p95i = (int) Math.min(sorted.length - 1, Math.ceil(sorted.length * 0.95) - 1);
            if (p95i < 0) p95i = 0;
            r.p95Ms = sorted[p95i] / 1_000_000.0;
            return r;
        }
    }

}

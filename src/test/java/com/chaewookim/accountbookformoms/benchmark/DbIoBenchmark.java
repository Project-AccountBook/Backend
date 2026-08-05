package com.chaewookim.accountbookformoms.benchmark;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
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
        seedBoards();
        long t1 = System.currentTimeMillis();
        System.out.println("=== [BENCH] Board seed done: " + (t1 - t0) + " ms");

        seedComments();
        long t2 = System.currentTimeMillis();
        System.out.println("=== [BENCH] Comment seed done: " + (t2 - t1) + " ms");

        // ANALYZE
        runInTx(() -> {
            em.createNativeQuery("ANALYZE TABLE board").getResultList();
            em.createNativeQuery("ANALYZE TABLE comment").getResultList();
        });
        System.out.println("=== [BENCH] ANALYZE done");
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
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,U1");
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
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,U1");
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
        String scenarios = System.getProperty("bench.run.scenarios", "R1,R2,U1");
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

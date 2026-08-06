# Project: Joint Living (공동구매 & 가계부 포트폴리오)

## 프로젝트 개요
주부를 위한 가계부 관리 및 공동구매 커뮤니티 플랫폼
팀 구성: 3인 (공동구매 / 회원관리+가계부 / 게시판+비교)

## 기술 스택
- Backend: Java, Spring Boot, JPA, QueryDSL
- DB: MySQL, Redis
- Search: Elasticsearch (nori 분석기)
- Infra: AWS (S3, RDS), Docker
- CI/CD: GitHub Actions

---

## 기술적 의사결정

### 1. 인증 및 인가
- **도입**: JWT + Spring Security + OAuth2 Client
- **배제**: 세션 기반 인증
- **이유**: 다중 서버 환경에서 세션 클러스터링 불필요. 모바일 확장 고려해 토큰 방식 선택. Refresh Token으로 로그인 유지 구현

### 2. 캐싱 및 임시 데이터 저장
- **도입**: Redis
- **배제**: 로컬 캐시(Caffeine), Memcached
- **이유**: 다중 서버 환경에서 데이터 정합성 보장. List/Set 등 다양한 자료구조 지원으로 알림 시스템 등 확장 유리. Refresh Token, 이메일 인증번호 만료 시간 관리에도 활용

### 3. 게시판 검색
- **도입**: Elasticsearch (nori 분석기)
- **배제**: MySQL Full-Text Search
- **이유**: LIKE 검색은 인덱스 미사용으로 풀스캔 발생. nori 형태소 분석기로 한국어 검색 품질 향상. 접속 로그 수집 및 인기 게시물 랭킹에도 활용

### 4. 조회수 관리
- **도입**: Redis INCR + Spring Scheduler DB 동기화
- **배제**: DB 직접 업데이트
- **이유**: 조회마다 DB UPDATE 시 부하 증가. Redis INCR로 카운트 후 주기적으로 DB 동기화

### 5. 댓글/대댓글 캐싱
- **도입**: Redis Cache
- **배제**: Caffeine (로컬 캐시)
- **이유**: 분산 환경에서 서버 간 캐시 공유 가능. 댓글 추가/수정/삭제 시 캐시 무효화 후 갱신

### 6. 통계 집계 (예산/수입/지출 비교)
- **도입**: Spring Scheduler 배치 집계
- **배제**: 실시간 집계 쿼리
- **이유**: 나이대별/카테고리별 평균을 매번 실시간 집계하면 사용자 증가 시 DB 부하 급증. 배치로 미리 계산 후 Redis 저장

### 7. 조회 성능 최적화
- **도입**: DB Indexing
- **배제**: Redis Caching 단독 적용
- **이유**: 인덱스 없이 캐싱만 적용하면 캐시 미스 시 풀스캔 발생. 기본 최적화 선행 필수. user_id, category_id, created_at 단일 인덱스 및 복합 인덱스 적용

### 8. 위치 데이터 저장 및 조회
- **도입**: Redis GEO
- **배제**: MySQL Spatial
- **이유**: 이미 Redis 사용 중으로 추가 인프라 불필요. GEORADIUS 명령어로 근처 사용자 조회 간단. Redis를 캐싱/분산락/GEO 다양하게 활용

### 9. 좌표 변환
- **도입**: Kakao Geocoding API
- **배제**: Google Maps API
- **이유**: 한국 주소 체계 최적화. 무료 할당량 충분. 국내 서비스 표준

### 10. 공동구매 동시성 제어
- **도입**: Redis 분산 락 (Redisson)
- **배제**: 비관적 락 (Pessimistic Lock)
- **이유**: 인메모리 기반으로 속도 빠르고 DB 부하 감소. 비관적 락은 트래픽 집중 시 DB 커넥션을 오래 점유해 병목 발생

### 11. 도메인 간 결합도 완화
- **도입**: Spring ApplicationEvent 발행
- **배제**: 직접 호출
- **이유**: 공동구매 도메인이 가계부 도메인에 강하게 결합되는 문제 해소. 공동구매는 이벤트만 발행하고 가계부 리스너가 비동기로 처리

### 12. 글로벌 캐싱 (공동구매)
- **도입**: Redis 글로벌 캐시
- **배제**: 로컬 캐시, DB 직접 조회
- **이유**: 동시성 제어용 Redis 재활용. 카테고리 정보, 랭킹 데이터 캐싱으로 DB 부하 최소화. 단, Redis 단일 장애점 문제는 인지하고 있으며 추후 Sentinel 구성 고려

### 13. 고정 지출/수입 자동화
- **도입**: Spring @Scheduled + ShedLock
- **배제**: Spring Batch
- **이유**: Spring Batch는 대규모 데이터 처리용으로 현재 요구사항 대비 오버엔지니어링. ShedLock으로 다중 서버 환경에서 스케줄 중복 실행 방지

### 14. 실시간 알림
- **도입**: FCM + Spring ApplicationEvent
- **배제**: WebSocket, SSE
- **이유**: WebSocket은 단순 알림에 오버엔지니어링. SSE는 서버 연결 유지 부담. FCM은 알림 부하를 구글 서버에 위임 가능하고 모바일 백그라운드 푸시 지원

### 15. 이메일 전송
- **도입**: Spring Boot Starter Mail + Google SMTP
- **이유**: 이메일 인증번호 전송 용도. 별도 인프라 없이 빠른 구현 가능

### 16. 데이터 파일 내보내기
- **도입**: Apache POI (Excel) 또는 OpenCSV
- **비고**: 추후 확정 필요. Excel 형식 필요 시 Apache POI, 단순 데이터 추출 시 OpenCSV

---

## 주요 설계 결정

### DB 설계
- 모든 테이블 BaseEntity 상속 (created_at, updated_at, deleted_at)
- 소프트 삭제 방식 (is_deleted, deleted_at)
- 이미지 테이블 통합 관리 (reference_id + reference_type)
- 댓글 셀프 조인 방식 (parent_id, FK 제약 없음)

### 게시판
- Q&A / 노하우 게시판 2depth 댓글 구조
- 게시물 본문 HTML 저장, 이미지 S3 업로드 후 URL 삽입
- 삭제된 댓글은 "삭제된 댓글입니다" 표시 (대댓글 구조 유지)

### 비교 페이지
- 예산/수입/지출/종합 비교는 단일 페이지 탭 방식
- 위치 기반 비교는 별도 페이지로 분리
- 공개 설정한 사용자만 비교 대상에 포함

---

## 배포 전 보안 감사 (2026-07-10) — 요금 폭탄 방지

포트폴리오 배포 시 공격받으면 실비용이 발생할 수 있는 항목 감사 결과. 상세 완화 방안은 아래 "후속 작업 우선순위" 로 통합.

### 🔴 치명적 — 배포 전 반드시 패치

**S1. 이메일 인증 코드 발송 남용**
- 위치: `AuthController.sendSignupCode/sendPasswordCode` (line 50, 59) → `EmailVerificationService`
- 문제: Redis lock 1분 재발송 방지만 있고 IP/이메일별 총량 제한 없음. lock 만료 후 무제한 재발송 가능
- 영향: Gmail SMTP는 하루 500건 계정 정지 임계값. 자동화 시 계정 정지 + 인증 흐름 마비
- 완화: IP당·이메일당 하루 5회, 5분 lock, 캡차 도입 검토

**S2. Pageable 크기 제한 없음**
- 위치: `TransactionController.getAllUserTransactions` (line 55), `BoardController.search` (line 135), 대부분의 list 엔드포인트
- 문제: `?size=100000` override 가능. 특히 ES는 대량 검색 요청 몇 개로 노드 재시작 위험
- 영향: 1회 요청으로 서버 OOM, ES 클러스터 재부팅 시 데이터 손실 가능
- 완화: 모든 Pageable 파라미터에 `@Max(100)` 검증, ES query timeout 5s

**S3. AsyncConfig TaskExecutor 미구성**
- 위치: `global/config/AsyncConfig.java`
- 문제: `@EnableAsync` 만 있고 `ThreadPoolTaskExecutor` 빈 없음 → `SimpleAsyncTaskExecutor`(매 호출 새 스레드)
- 영향: FCM 알림·ES 인덱싱 트리거 경로에서 스레드 폭발, 메모리 누수
- 완화: `ThreadPoolTaskExecutor` 빈 정의 (corePoolSize=5, maxPoolSize=10, queueCapacity=100)

### 🟠 높음 — 배포 후 1주일 내

**S4. FCM 토큰 등록 검증 부재**
- 위치: `UserDeviceController` (추정)
- 문제: 임의 토큰 등록 가능. 사용자당 기기 수 제한 없음
- 영향: 스팸 알림 자동화로 FCM API 호출 폭증. Blaze plan 전환 시 과금
- 완화: FCM 토큰 format 검증, 1사용자당 최대 5개 디바이스

**S5. Redis GEO 무제한 저장**
- 위치: `UserLocationService.updateLocation`
- 문제: 위치 갱신 debounce 없음. 자동화 요청으로 Redis 메모리 폭증
- 완화: 최소 1시간 debounce, `user:geo` ZSET 크기 모니터링

**S6. Kakao Geocoding 캐싱 상태 확인 필요**
- 문제: 매 요청마다 호출되는 경로가 있으면 Kakao 무료 할당량(30만/일) 초과 시 유료 전환
- 완화: 주소 → 좌표 결과를 Redis에 24시간 이상 캐싱 (address hash 키)

### 🟡 중간 — 배포 후 1개월 내

**S7. Transaction Export 크기 제한 없음**
- 위치: `TransactionController.exportTransactions` (line 102-114) → `TransactionExportService.exportToCsv`
- 문제: 3년치 전체 거래 50만 건 Excel 변환 → 메모리 1GB+
- 완화: 월별 단위 export 강제 또는 비동기 job queue

**S8. Compare 위치 기반 조회 N+1**
- 위치: `ExpenseCompareService.compareByLocation` (line 155-177)
- 문제: GEORADIUS로 근처 사용자 100명 조회 후 각각 `sumPublicByUserIds` 호출
- 완화: 그룹 결과 Redis 1시간 사전 계산

**S9. ES 딥 페이지네이션**
- 문제: `page=1000, size=100` 요청 시 100,000개 건너뜀 → CPU/메모리 폭증
- 완화: Search-After 커서 기반 pagination

### 🟢 낮음

**S10. 로그인 bcrypt CPU 소모 공격** — IP당 실패 5회 후 block
**S11. 회원가입 자동화 DB row 폭증** — IP당 하루 5계정 제한

### 인프라 방어 체크리스트 (코드 외)

- **Cloudflare 무료 플랜** 프론트/API 앞단 → L7 DDoS, 봇 자동 차단 (가성비 최고)
- **AWS/Kakao/FCM/Gmail 각각 Budget Alert** 설정 (사고 조기 감지)
- **외부 API 호출 카운터 로깅** (SMTP/FCM/Kakao) → 이상 감지
- RDS slow query log, S3 egress 비용, FCM API 호출량 CloudWatch 모니터링

---

## DB 효율성 감사 (2026-08-05) — 전 도메인 스캔

Board / Comment / Transaction 도메인 최적화 완료 후, 전 도메인 대상으로 인덱스 / N+1 / fetch 전략 / 캐시 정합성 감사 실시.

### 이미 완료된 개선 (참고)

- Board `idx_board_user_type (user_id, type)` / Comment `idx_comment_user (user_id, created_at)` 복합 인덱스
- 전역 `hibernate.jdbc.batch_size=20`, `order_inserts/updates=true`, `default_batch_fetch_size=100`, `batch_versioned_data=true`
- User↔UserSetting LAZY 명시
- `spring.data.web.pageable.max-page-size: 100` 전역 설정
- Comment `@SQLRestriction` 미부착은 **의도된 설계** (대댓글 트리 유지 위해 soft-deleted 로드 필요). 감사 시 오탐 금지.
- 벤치마크 결과 (`docs/benchmark/results/2026-08-05_db_io.csv`):
  - Board findByUserIdAndType: 3.54 → 1.57 ms (-55.5%)
  - Comment findByUserId ORDER BY created_at DESC: 22.01 → 0.65 ms (-97.1%)
  - Comment 200건 bulk update: SQL 4020 → 40 (-99.0%)

### 도메인별 심각도 (2026-08-05 시점)

| 도메인 | High | Med | 상태 |
|---|---|---|---|
| account | 1 | 1 | ⚠️ 인덱스 전무 |
| notification | 1 | 2 | ⚠️ N+1 삭제 |
| userdevice | 1 | 1 | ⚠️ 인덱스+FCM 검증 |
| grouppruchase | 0 | 2 | ⚠️ 인덱스 전무 |
| budget | 0 | 2 | ⚠️ 소프트삭제 일관성 |
| expense/income compare | 0 | 1 | ⚠️ 캐시 evict 누락 |
| transactioncategory | 0 | 1 | 인덱스 보강 여지 |
| user, transaction, board, comment, like, bookmark, follow, tag, image, usersetting | 0 | 0~1 | ✓ 양호 |

### 🔴 High (배포 전 필수)

**D1. Notification 계정 삭제 시 N+1 DELETE**
- 파일: `domain/notification/application/NotificationService.java:103`
- 현상: `notificationRepository.deleteAll(notificationRepository.findAllByUserId(userId))` — 알림 N건 → SELECT 1 + DELETE N 왕복
- 영향: 1000건 알림 계정 삭제 시 트랜잭션 장시간 점유
- 완화: `@Modifying @Query("DELETE FROM Notification n WHERE n.userId = :userId") int deleteByUserId(Long userId)` 로 단일 벌크 DELETE

**D2. UserDevice 인덱스·검증 부재** (S4 요금폭탄 항목과 연결)
- 파일: `domain/notification/entity/UserDevice.java`, `dao/UserDeviceRepository.java`
- 현상:
  - `@Table(indexes=...)` 없음 → `findByUser` 호출 시 user_id 인덱스 없어 풀스캔 가능성
  - FCM 토큰 컬럼 length/format 검증 없음, 사용자당 device 수 제한 없음
- 영향: 스팸 토큰 등록 자동화 시 FCM API 호출 폭증 (Blaze plan 과금)
- 완화: `@Index(user_id)` + `findByUserId(Long)` 신설 + `@Column(length=250)` + FCM 토큰 format 검증 + 1 user 최대 5 device

**D3. Account 인덱스 전무**
- 파일: `domain/asset/entity/Account.java:29~34`
- 현상: `@Table` 자체 없음 → `findByUserId` 매 거래/조회 시 풀스캔
- 영향: 계정 데이터 누적 시 매 거래 생성 latency 상승
- 완화: `@Table(name="account", indexes={@Index("user_id"), @Index("user_id, account_name")})`

### 🟠 Medium (한 줄 요약)

- **D4. GroupPurchase 인덱스 전무** — `creator_id`, `(category_id, status)`, `deadline` 추가. `domain/grouppruchase/domain/GroupPurchase.java:20`
- **D5. Budget `@SQLRestriction` 부재** — native workaround 로 우회 중, `Budget.java` 에 `@SQLRestriction("deleted_at IS NULL")` 추가로 일관성 확보 (Comment 처럼 트리 요구사항 없음)
- **D6. Budget category null 방어** — `BudgetRepository.java:39-41` FETCH JOIN 후 Service 매핑 시 null 체크 미흡
- **D7. Compare 캐시 evict 누락** — `TransactionService.create/update/delete` 시 `compare:expense`, `compare:budget` `@CacheEvict(allEntries=true)` 배선 필요
- **D8. InterestCategoryService.deleteAllByUserId / AccountService fixedTx forEach delete** — D1 과 동일 패턴, `@Modifying DELETE` 치환
- **D9. TransactionCategory 인덱스** — `(user_id, name, type)` 복합 고려
- **D10. Bookmark/Follow raw Long 참조** — FK 없음. 스냅샷 목적이면 문서화, 아니면 `@ManyToOne` 전환 검토

### 공통 안티패턴

1. **findAll → forEach delete** (Notification / InterestCategory / Account.fixedTx) — 3 도메인 반복
2. **인덱스 불균형** — Board/Comment/Like/Bookmark 완비, Account/GroupPurchase/UserDevice 전무
3. **raw `Long userId` vs `@ManyToOne` 혼재** — Board/Comment/Like/Bookmark/GroupPurchase 5 도메인. 스냅샷 보존 의도 명시 필요
4. **`@Cacheable` 대비 `@CacheEvict` 배선 누락** — compare 계열

### 마이그레이션 SQL 초안 (prod 배포 시)

```sql
CREATE INDEX idx_userdevice_user      ON user_device (user_id);
CREATE INDEX idx_account_user         ON account (user_id);
CREATE INDEX idx_account_user_name    ON account (user_id, account_name);
CREATE INDEX idx_gp_creator           ON group_purchase (creator_id);
CREATE INDEX idx_gp_category_status   ON group_purchase (category_id, status);
CREATE INDEX idx_gp_deadline          ON group_purchase (deadline);
```

---

## 후속 작업 우선순위

### P0 — 배포 전 필수 (요금 폭탄 / 데이터 손실 직결)

**운영 설정 정비 (Option B)**
- **[P0-1] Profile 분리** — `application.yml` 을 공통/local/prod 3분할. prod에 `ddl-auto: update`/`show-sql: true` 가 그대로 가면 자동 ALTER로 데이터 손실 / 로그 IO 폭증
  - 공통: `spring.application.name`, `datasource.driver`, `spring.config.import: application-secret.yml`, `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`
  - `application-local.yml`: 현재 동작 그대로 (`ddl-auto: update`, `show-sql: true`)
  - `application-prod.yml`: `ddl-auto: validate`, `show-sql: false`, Hibernate SQL log WARN
- **[P0-2] Prod DDL 전략 = validate** — 스키마 변경은 PR에 `docs/db/migration/` SQL 스냅샷 동봉, 배포 전 수동 실행. Flyway 도입 전 임시 가드
- **[P0-3] 검증** — local/prod profile 부팅 확인, prod + 로컬 DB 로 validate 통과 확인

**보안 (요금 폭탄 방어)**
- **[P0-4] 이메일 인증 발송 rate limit 강화** (S1) — IP·이메일별 하루 5회, 5분 lock
- **[P0-5] 모든 Pageable `@Max(100)` 검증** (S2) — 특히 ES 검색, Transaction list
- **[P0-6] AsyncConfig TaskExecutor 빈 정의** (S3) — 스레드 폭발 방지 (기존 최적화 백로그 항목이기도 함)

**인프라 방어**
- **[P0-7] Cloudflare 무료 플랜 프론트/API 앞단 배치**
- **[P0-8] AWS/Kakao/FCM/Gmail Budget Alert 설정**

---

### P1 — 배포 후 1주일 내

**운영 설정 정비 (Option B 잔여)**
- **[P1-1] HikariCP 튜닝** — prod: `maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 3000`, `idle-timeout: 300000`, `max-lifetime: 1200000`, `leak-detection-threshold: 60000`. `max-lifetime` 은 MySQL `wait_timeout` 보다 짧게 설정해 broken pipe 방지
- **[P1-2] JDBC Batch** — `hibernate.jdbc.batch_size: 20`, `batch_versioned_data: true`, `order_inserts/updates: true`. ⚠️ IDENTITY PK 라 insert batch 자동 비활성화, 현 효과는 update/delete 한정

**보안**
- **[P1-3] FCM 토큰 등록 검증** (S4) — format 검증, 1사용자당 최대 5기기
- **[P1-4] Redis GEO 위치 갱신 debounce** (S5) — 최소 1시간
- **[P1-5] Kakao Geocoding 결과 Redis 캐싱 확인/구현** (S6) — 24시간 TTL
- **[P1-6] 외부 API 호출 카운터 로깅** — SMTP/FCM/Kakao

---

### P2 — 배포 후 1개월 내

**보안**
- **[P2-1] Transaction Export 크기 제한** (S7) — 월별 단위 강제 또는 비동기 처리
- **[P2-2] Compare 위치 기반 N+1 해소** (S8) — 그룹 결과 Redis 1시간 사전 계산
- **[P2-3] ES Search-After 커서 pagination** (S9)
- **[P2-4] 로그인 실패 IP rate limit** (S10) — 5회 실패 후 5분 block
- **[P2-5] 회원가입 IP 제한** (S11) — 하루 5계정

**게시판 검색 (#26) 마무리**
- **[P2-6] ES 인덱싱 실패 처리 강화** — Spring Retry 또는 outbox 패턴으로 영구 유실 방지
- **[P2-7] docker-compose 정비** — ES 8.x + nori plugin 포함 컨테이너 정의
- **[P2-8] ES 통합 테스트** — Testcontainers Elasticsearch로 nori 검색 동작 검증

**기능 미구현**
- **[P2-9] Budget CRUD 구현** — `BudgetService` 부재. Budget 등록 API 없음. 구현 시 `compare:budget` 캐시에 `@CacheEvict(allEntries=true)` 필수
- **[P2-10] 이미지 S3 presigned URL 인프라** — `POST /api/v1/images/presigned-url` + 클라이언트 직접 업로드
- **[P2-11] Follow 상대 알림** — 팔로우 시 Notification 도메인 이벤트 발행

**최적화 백로그**
- **[P2-12] 댓글 캐시 (Redis)** — `CommentService.list` 에 `@Cacheable`/`@CacheEvict` 적용. 의사결정 #5 와 현 코드 불일치
- **[P2-13] BudgetCompareService.compareByCategory 중복 쿼리** — `averageCategoryBudget()` + `findMyCategoryBudget()` 를 단일 쿼리로 통합
- **[P2-14] Expense/IncomeCompareService.compareByCategory 중복 쿼리** — 그룹 평균 결과에 본인 포함되도록 쿼리 변경
- **[P2-15] User↔UserSetting fetch 전략** — 기본 EAGER 상태. 명시적 LAZY + 필요 시점 fetch join

---

### P3 — 기술 부채 (분기 이상)

**Option B 종료 후 잔여 빚**
- **[P3-1] Flyway 도입** — 스키마 변경의 코드/DB 동기 관리. `shedlock` 테이블 DDL 도 V*.sql 로 이전
- **[P3-2] OSIV 비활성화** (`spring.jpa.open-in-view: false`) — Lazy 노출 사냥 필요, 회귀 위험 큼
- **[P3-3] IDENTITY PK → SEQUENCE/pooled-lo 전환 검토** — batch insert 효과 회수
- **[P3-4] Prod 비밀 관리** — `application-secret.yml` 을 환경변수/Secrets Manager 로 이전

**아키텍처 개선**
- **[P3-5] HOT 랭킹 실시간성** — Redis ZSET 기반 실시간 랭킹, like 이벤트마다 ZINCRBY. 현재는 5분 stale 허용

---

## 완료된 작업 이력 (참고용)

### 게시판(Q&A / 노하우) 프론트 연동 관련
프론트(`fix/ACC-102-add-feature` 및 `feat/ACC-33-board-feature-link`)에서 Board/Comment CRUD·검색·수정·삭제·닉네임 노출까지 연동 완료.

#### 1차 구현 완료
- **게시판 카테고리 도메인** (`domain/boardcategory/`): `BoardCategory` 엔티티 + `GET /api/v1/board-categories?type=QNA|KNOWHOW`. `BoardCategorySeeder`(ApplicationRunner)로 시드 데이터 자동 삽입.
- **좋아요 도메인** (`domain/like/`): `PostLike` 단일 테이블 + `targetType`(BOARD/COMMENT). `POST /api/v1/boards/{id}/like`, `POST /api/v1/comments/{id}/like` 토글. `BoardResponse`/`CommentResponse` 에 `likeCount`, `liked` 포함.
- **북마크 도메인** (`domain/bookmark/`): `Bookmark`(user_id, board_id) + `POST /api/v1/boards/{id}/bookmark`. `BoardResponse.bookmarked` 포함.
- **Q&A 상태 필드**: `Board.isResolved`, `Board.isUrgent` + 작성자 전용 토글 (`PATCH /api/v1/boards/{id}/resolved|urgent`). `Comment.accepted` + Q&A 작성자만 채택 (`PATCH /api/v1/comments/{id}/accept`, 채택 시 board.resolved=true 자동 전환).
- **/users/me 응답에 id 추가**.

#### 2차 구현 완료
- **Tag 도메인** (`domain/tag/`): `Tag` + `BoardTag` 조인 테이블, `TagService.setTagsForBoard/tagsOfBoard/tagsByBoards/boardIdsWithTag`, `GET /api/v1/tags`. `BoardCreateRequest/BoardUpdateRequest.tags`. `GET /api/v1/boards?tag=` 필터.
- **Image 도메인** (`domain/image/`): `Image` 통합 테이블(reference_type, reference_id, sort_order). `POST /api/v1/boards/{postId}/images`(URL 저장), `GET /api/v1/boards/{postId}/images`, `DELETE /api/v1/images/{imageId}`. `BoardResponse.imageUrls`.
- **HOT 랭킹**: `GET /api/v1/boards/hot?type=KNOWHOW&days=7&limit=3`. Score = views + likeCount * 3.
- **댓글 페이지네이션**: `GET /api/v1/comments/{postId}/threads` — `Page<CommentThreadResponse>`. 기존 flat `GET /comments/{postId}` 유지.
- **Follow 도메인** (`domain/follow/`): `Follow(follower_id, following_id)`. `POST /api/v1/users/{userId}/follow` 토글, `GET /followers|following`.
- **User 프로필 통계** (`domain/userstats/`): `GET /api/v1/users/{userId}/stats`.
- **`UserProfileResponse.role`** 노출.
- **관리자 UI 프론트**: `AdminView.tsx` + Sidebar에 role=ROLE_ADMIN 시 노출. `DELETE /api/v1/admin/boards/{id}` 연동.
- **서버 페이지네이션 프론트**: QnaListView/KnowhowListView.

#### 3차 구현 완료 — ES 태그 통합
- `BoardDocument.tags: List<String>` (`FieldType.Keyword`)
- `BoardDocument.from(board, tags)` 오버로드, `BoardIndexEventListener` 색인 시 태그 주입
- `BoardSearchQueryRepository.search` bool query: `title^2 / content` (multi_match) OR `tags` (term), minimum_should_match=1
- `BoardSearchResponse.tags` 필드 + 프론트 렌더링

#### 4차 구현 완료 — ES 재색인 + HOT 캐싱
- **ES 초기 재색인 배치**: `BoardReindexService.reindexAll()` 200건 페이지 로드 → `tagService.tagsByBoards` 벌크 → `saveAll` 배치 upsert. `POST /api/v1/admin/boards/reindex` (ROLE_ADMIN). `BoardReindexRunner` — `app.board.reindex-on-startup=true` 부팅 시 자동 재색인.
- **HOT 랭킹 Redis 캐싱**: `BoardService.hot` 에 `@Cacheable(cacheNames="board:hot", key="type:days:limit")`. `RedisConfig.CACHE_BOARD_HOT` 5분 TTL. `BoardHotWarmupScheduler` 4분 주기 warm-up + ShedLock. `app.cache.warmup.board-hot.enabled` 로 on/off. 좋아요 토글/삭제 시 명시적 evict 없음 (5분 TTL + warm-up 로 stale 흡수).

#### 5차 구현 완료 — 좋아요/조회수 카운터
- **좋아요 카운터 Redis 캐싱** (Cache-aside + INCR/DECR 하이브리드):
  - `LikeCountCacheService` — 키 `like:count:{BOARD|COMMENT}:{id}`, TTL 30분.
  - `getCount`: GET → 미스 시 DB `countByTargetIdAndTargetType` 후 SET.
  - `getCounts`: MGET → 미스 ID 벌크 DB COUNT → MSET.
  - `applyDelta(±1)`: `hasKey` 확인 후에만 INCR/DECR. 캐시 미스 상태 INCR-from-null 로 인한 drift 방지.
  - `evict`: DEL.
  - `PostLikeService.count/countByTargets` 캐시 경유. `toggle` 은 DB mutation 후 `applyDelta`.
  - `BoardService.delete` / `CommentService.delete` 에서 `likeService.evictCount(...)`.
- **조회수 Redis 캐싱 완비**: `BoardViewCountService.evict(boardId)` 신설 (dirty set SREM + counter DEL). 삭제된 게시물의 delta 유실 로그 해소.

#### 6차 구현 완료 — 관리자 API
- **관리자 게시물/댓글 리스트 API**:
  - `GET /api/v1/admin/boards?type=&includeDeleted=` — `AdminBoardResponse`(원문 무마스킹 + `adminDeleted`/`userDeleted` + `deletedAt`). `includeDeleted=true` 시 `@SQLRestriction` 우회 위해 `AdminBoardRepository` native query (created_at DESC 하드코딩).
  - `GET /api/v1/admin/comments?referenceType=&referenceId=` — `findAll(pageable)` 로 소프트 삭제 포함.
  - 둘 다 `/api/v1/admin/**` ROLE_ADMIN 강제.
- **좋아요 카운터 정합성 검증 배치**:
  - `LikeCountReconcileService.reconcile(maxKeys)` — SCAN `like:count:*` → 타입별 `countByTargets` 벌크 → drift 시 WARN + DB 정본으로 SET. 손상 값 DEL. `ReconcileReport(scanned, mismatched, corrected)`.
  - `LikeCountReconcileScheduler` — 매시 7분 (`0 7 * * * *`), ShedLock, 500 키/회. `app.like.reconcile.enabled=true`.
  - `POST /api/v1/admin/likes/reconcile?limit=` 수동 트리거.

#### 7차 구현 완료 — 관리자 프론트
- **관리자 프론트 UI 확장** (`AdminView.tsx`): 3 탭 (게시물/댓글/운영 작업).
  - 게시물 탭: `GET /admin/boards?type=&includeDeleted=`, `adminDeleted`/`userDeleted`/정상 3단 배지.
  - 댓글 탭: `GET /admin/comments?referenceType=&referenceId=`, `parentId` 대댓글 표시, 유저 소프트 삭제 dim.
  - 운영 작업 탭: ES 재색인 (`POST /admin/boards/reindex`) + 좋아요 정합성 (`POST /admin/likes/reconcile?limit=500`).
  - 프론트 `boardApi.ts` 에 `adminListBoards`, `adminListComments`, `adminReindexBoards`, `adminReconcileLikes` + 타입.

### 이전 세션 결정 사항
- `GET /api/v1/boards?type=QNA|KNOWHOW` 필터 (BoardRepository.findByType)
- `BoardResponse` / `CommentResponse` 에 `authorNickname` 포함, list 경로 `UserRepository.findAllById` 일괄 조회로 N+1 방지
- 게시물/댓글 수정(PATCH) 프론트 상세 화면 인라인 편집 연결

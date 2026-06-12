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

## 후속 작업 (TODO)

### 위치 기반 비교 기능 (미착수)
설계상 "위치 기반 비교는 별도 페이지로 분리" 명시되어 있으나 구현 0%. `UserLocationService`(Redis GEO) 와 4종 Compare 서비스(Budget/Expense/Income/Portfolio) 가 연결되어 있지 않음.

- **Compare enum 확장**: 4개 도메인 `*CompareType` 에 `LOCATION` 값 추가 (현재 `AGE`/`AMOUNT`/`CATEGORY` 만 존재)
- **요청 DTO 확장**: `radiusKm` 파라미터 추가 (`*CompareRequest`)
- **그룹 산출 흐름**:
  1. `UserLocationService.findNearbyUsers(userId, radiusKm)` → 반경 내 userId 집합
  2. 공개 설정(`UserSetting.isPublic`) 사용자로 한 번 더 필터
  3. 해당 userId IN (...) 절을 그룹 평균 쿼리에 주입
- **응답 메타**: 반경, 표본 수, 위치 미등록 시 에러 코드(`LOCATION_NOT_REGISTERED`) 처리
- **캐시 키 설계**: 위치 기반은 사용자별 반경마다 결과가 달라 캐시 히트율 낮음. Redis 캐시 적용 시 `geohash prefix + radius bucket` 단위로 키 설계 고려
- **인덱스 의존성**: 우선순위 #1 인덱스(Budget·FixedTransaction 의 user_id 기반) 선행되어야 IN 절 성능 확보

### Compare 캐싱 Phase 2 — 의사결정 #6 완전 정합화
Phase 1(우선순위 #3)에서 `@Cacheable` 을 user 단위 응답에 적용 → 같은 사용자의 동일 파라미터 재조회만 hit. 의사결정 #6("나이대별/카테고리별 평균 배치 집계 후 Redis 저장")의 본래 의도는 **그룹 평균 자체를 공용 캐시**로 재사용하는 것. 현재 구조는 user 결합 응답 캐시라 cross-user hit 0%.

- **`compareWithGroup` 분해**: 내부에서 (1) 그룹 평균 산출 / (2) 본인 amount 산출 / (3) diff 계산을 분리
- **그룹 평균 캐시**: 키 = `(type, yearMonth, categoryId, ageBucket, amountBucket)` — userId 제외. 같은 demographic 사용자는 cache hit
- **본인 amount 는 캐시 X**: mutation 빈도/정합성 부담 vs 비용 trade-off. 직접 쿼리 유지
- **@Scheduled 배치 추가**: 매일/매시간 그룹 평균을 미리 계산해 캐시에 warm-up (대량 트래픽 대비). ShedLock 으로 다중 서버 중복 방지 (의사결정 #13 인프라 재사용)
- **eviction 단순화**: 현재 `allEntries=true` 는 본인 mutation 시 모든 캐시 폭파. 그룹 캐시 분리되면 user-mutation 은 그룹 캐시에 영향 없음 → evict 불필요(자체 TTL + 배치 갱신). 단, 캐시 정확성 vs 비용 trade-off 재평가 필요

### 운영 설정 정비 — Option B (Profile 분리 + Batch/HikariCP)
최적화 백로그 우선순위 #4 의 안전 범위만 우선 진행. **Flyway 도입 및 OSIV 비활성화는 별도 이슈로 분리**.

#### 4-1. Profile 분리
- `application.yml` 을 공통/local/prod 3분할
- 공통: `spring.application.name`, `datasource.driver`, `spring.config.import: application-secret.yml`, `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`
- `application-local.yml`: 현재 동작 그대로 보존 (`ddl-auto: update`, `show-sql: true`, `format_sql: true`)
- `application-prod.yml`: `ddl-auto: validate`, `show-sql: false`, `format_sql: false`, Hibernate SQL log WARN
- **Why**: prod에 `ddl-auto: update`/`show-sql: true` 가 그대로 가면 자동 ALTER로 데이터 손실 / 로그 IO 폭증 위험

#### 4-2. Prod DDL 전략 — validate
- prod ddl-auto = `validate` 채택. 스키마 변경은 PR에 SQL 스냅샷(`docs/db/migration/`) 동봉 후 배포 전 DBA가 수동 실행
- **Why**: Flyway 도입 전 임시 가드. drift 발견은 부팅 시점에 빠르게. Flyway 도입 시 같은 디렉터리를 `db/migration/V*.sql` 로 자연 이전 가능

#### 4-3. HikariCP 튜닝
- prod: `maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 3000`, `idle-timeout: 300000`, `max-lifetime: 1200000`, `leak-detection-threshold: 60000`, `pool-name: jointliving-pool`
- local: `maximum-pool-size: 5`, `leak-detection-threshold: 60000`
- **Why**: 기본값(pool=10, connection-timeout=30s)은 트래픽 증가 시 30초 멈춤 발생. leak detection 꺼져 있어 누수 진단 불가. `max-lifetime` 은 MySQL `wait_timeout` 보다 짧게 설정해 broken pipe 방지

#### 4-4. JDBC Batch
- `hibernate.jdbc.batch_size: 20`, `hibernate.jdbc.batch_versioned_data: true`, `hibernate.order_inserts: true`, `hibernate.order_updates: true`
- **Why**: `saveAll(N)` 호출 시 N번 라운드트립을 1/20 로. 특히 #69 고정거래 스케줄러처럼 대량 insert 경로에 효과적
- **⚠️ 제한**: 모든 엔티티 PK가 `GenerationType.IDENTITY` 라 Hibernate가 insert batch를 자동 비활성화. **현 batch 효과는 update/delete 한정.** Insert batch 효과까지 원하면 PK 전략 재검토 별도 이슈 필요

#### 4-5. 검증
- local profile 부팅 확인
- prod profile + 로컬 DB 부팅 → validate 통과 확인
- 일시적으로 show_sql 켜고 batch insert 시나리오 → 묶이는지 확인

#### Option B 종료 후 잔여 빚 (별도 이슈로 분리)
- **Flyway 도입** — 스키마 변경의 코드/DB 동기 관리
- **OSIV 비활성화** (`spring.jpa.open-in-view: false`) — Lazy 노출 사냥 필요해 회귀 위험 큼
- **IDENTITY PK → SEQUENCE/pooled-lo** 전환 검토 — batch insert 효과 회수
- **prod 비밀 관리** — `application-secret.yml` 을 환경변수/Secrets Manager 로 이전

### Budget CRUD 미구현
`BudgetService` 가 없어 Budget 엔티티 생성/수정/삭제 경로가 없음. 비교 기능은 동작하지만 사용자가 예산을 직접 등록할 API 미존재. 구현 시:
- Budget CRUD 추가 시 `compare:budget` 캐시에 `@CacheEvict(allEntries=true)` 추가 필수

### 게시판 검색 (#26) 마무리
초기 비동기 ES 인덱싱 구현 완료. 추가로 진행할 항목:
- **인덱싱 실패 처리 강화**: 현재 ES 호출 실패 시 로그만 남김. Spring Retry 또는 outbox 패턴으로 영구 유실 방지
- **초기 reindex 배치**: 기존 DB 데이터를 ES로 일괄 색인하는 admin/배치 job
- **docker-compose 정비**: ES 8.x + nori plugin 포함 컨테이너 정의
- **통합 테스트**: Testcontainers Elasticsearch로 실제 nori 검색 동작 검증

### 최적화 백로그 (2026-06-11 점검 결과)

CLAUDE.md 의 기술 의사결정 중 일부가 코드에 반영되지 않았고, 비교 API 의 쿼리 효율도 개선 여지가 큼.

#### 인덱스 누락 (단기 효과 큼)
- **Budget** (`domain/budget/entity/Budget.java`): unique constraint(user_id, year_month, category_id)만 존재. `findByUserIdAndYearMonth`, `sumTotalBudgetByUserIdAndYearMonth` 가 비교 경로 전반에서 호출되므로 `@Index(columnList="user_id, year_month")` 추가 필요.
- **Board** (`domain/board/entity/Board.java`): 인덱스 0개. `list(Pageable)` 가 `created_at DESC` 정렬, soft delete 로 `deleted_at IS NULL` 항상 필터 → `@Index(columnList="created_at")`, `@Index(columnList="type")`.
- **FixedTransaction** (`domain/asset/entity/FixedTransaction.java`): 인덱스 전무. 비교 호출마다 user_id+기간 풀스캔 → `@Index(columnList="user_id")` 최소.

> 현재 인덱스가 있는 곳: Transaction(`idx_transaction_user_account_date`), Comment(`idx_comment_reference`) 뿐.

#### 중복 / N+1 쿼리
- **`BudgetCompareService.compareByCategory`**: `averageCategoryBudget()`(AVG+COUNT) + `findMyCategoryBudget()` 가 같은 (year_month, category_id) 키로 두 번 왕복. 단일 쿼리로 합칠 수 있음.
- **`Expense/IncomeCompareService.compareByCategory`**: 그룹 평균 후 본인 합계를 `sumByUserAndTypeAndCategory` 로 fixed+variable 별도 재호출(=2쿼리). 그룹 합계 결과에 본인이 포함되도록 쿼리 변경하거나 한 번에 처리.
- **`PortfolioService.getPublicPortfolios`**: 한 번 호출에 fixed/variable × income/expense/budget = **6쿼리**. 사용자 합집합 메모리 병합 중. user_id GROUP BY 단일 쿼리(또는 UNION ALL 후 집계)로 축소 가능.

#### Redis 캐싱 미적용 (CLAUDE.md 설계 미구현)
- **댓글 캐시**: `CommentService.list` 에 `@Cacheable`/`@CacheEvict` 전무. 의사결정 #5 와 불일치.
- **조회수 Redis INCR**: `Board.increaseViews` 가 DB 직접 update. 의사결정 #4 의 "INCR + Spring Scheduler DB 동기화" 미구현.
- **그룹 평균 비교 캐시**: 매 요청마다 공개 사용자 전수 집계. 나이대별/카테고리별 평균은 TTL 캐시 후보. 의사결정 #6 의 배치 집계도 아직 없음.
- 코드베이스 전역 `@Cacheable`/`@CacheEvict`/`@EnableCaching` 0건. Redis 는 현재 RefreshToken 외 사용 미확인.

#### 비동기 / 스케줄러
- `global/config/AsyncConfig.java`: `@EnableAsync` 만 있고 `TaskExecutor` 빈 없음 → `SimpleAsyncTaskExecutor`(매 호출 새 스레드)로 동작. ES 인덱싱·알림 발송이 메인 풀 점유 가능. `ThreadPoolTaskExecutor` 빈 추가 필요.
- **`@Scheduled` 전무**: 의사결정 #13("고정 지출/수입 자동화 + ShedLock"), 조회수 동기화, 통계 배치 모두 미구현.

#### 설정 / 운영
- `application.yml`: `ddl-auto: update`, `show-sql: true`, `format_sql: true` 가 단일 profile. `application-local.yml`, `application-prod.yml` 로 분리하고 prod 는 `validate`/Flyway, show-sql off.
- `spring.jpa.properties.hibernate.jdbc.batch_size` 미설정 → bulk insert 1행 1쿼리. `batch_size=20`, `order_inserts/order_updates=true` 권장.
- HikariCP, OSIV(`spring.jpa.open-in-view` 기본 true) 설정 누락.

#### Fetch 전략
- `User`↔`UserSetting` 가 `@OneToOne(cascade)` 인데 fetch 미지정 → 기본 EAGER. 비교 쿼리마다 setting JOIN/즉시 로딩 발생. 명시적 LAZY + 필요 시점 fetch join 권장.

#### 우선순위 (효과/비용)
1. 인덱스 3개 추가 (Budget · Board · FixedTransaction) — 1~2h, 즉시 효과
2. `PortfolioService.getPublicPortfolios` 6쿼리 → 단일 GROUP BY — 가장 자주 호출될 엔드포인트
3. 그룹 평균 compare 응답 Redis 캐시 (`@Cacheable` + TTL, 변경 시 evict) — 의사결정 #6 정합화
4. profile 분리 + JDBC batch / HikariCP / OSIV 설정 — 한 번에 정리
5. `AsyncConfig` 에 `ThreadPoolTaskExecutor` 빈 추가
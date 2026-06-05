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

### 게시판 검색 (#26) 마무리
초기 비동기 ES 인덱싱 구현 완료. 추가로 진행할 항목:
- **인덱싱 실패 처리 강화**: 현재 ES 호출 실패 시 로그만 남김. Spring Retry 또는 outbox 패턴으로 영구 유실 방지
- **초기 reindex 배치**: 기존 DB 데이터를 ES로 일괄 색인하는 admin/배치 job
- **docker-compose 정비**: ES 8.x + nori plugin 포함 컨테이너 정의
- **통합 테스트**: Testcontainers Elasticsearch로 실제 nori 검색 동작 검증
# Joint Living / 공동구매
주부를 위한 가계부 포트폴리오 및 공동구매 커뮤니티

> **"주부를 위한 스마트한 가계부 포트폴리오 및 공동구매 커뮤니티"**
> 
> MODI는 체계적인 자산 관리부터 주변 이웃과의 예산 비교, 그리고 합리적인 소비를 위한 공동구매까지 지원하는 종합 가계부 플랫폼입니다.

## 🚀 프로젝트 핵심 기능 (Core Features)

### 🔐 1. 회원 및 인증 관리 (Identity & Access)
* **인증 인가:** Spring Security + JWT, OAuth2 Client를 활용한 간편 로그인
* **알림 시스템:** FCM (Firebase Cloud Messaging) + Spring Application Event를 활용한 비동기 알림 처리 (예산 소진, 관심 공동구매 알림 등)
* **계정/권한:** 이메일(Google SMTP) 기반 인증 및 관리자/일반 유저 권한 제어
* **맞춤 설정:** 정기 고정 수입/지출 스케줄링 및 개인 프로필 관리
### 📊 2. 스마트 가계부 포트폴리오 (Account Book)
* **자산 추적:** 계좌 초기 잔고 설정 및 실시간 변동 내역 반영 (DB Indexing 활용)
* **소비 분석:** 커스텀 카테고리 지원, 월별/기간별 지출 통계 (Redis Caching으로 조회 성능 극대화)
* **예산 관리:** 이번 달/다음 달 예산 설정 및 추가 지출 추적
* **자동화 및 내보내기:** Spring `@Scheduled` + `ShedLock`을 이용한 분산 스케줄링 제어, Apache POI를 활용한 엑셀/CSV 데이터 내보내기
### 🗺️ 3. 위치 기반 예산/지출 비교 (Location-based Comparison)
* **초개인화 분석:** 나와 타 사용자의 연령대별, 카테고리별, 단순 금액별 예산/수입/지출 비교
* **지리적 데이터 연동:** Kakao Geocoding API와 Redis GEO를 결합하여 내 주변(반경 N km) 이웃들의 평균 소비 포트폴리오를 도출 및 비교
### 🤝 4. 커뮤니티 및 노하우 공유 (Community Q&A)
* **게시판 구성:** 자유로운 Q&A 및 주부 노하우 공유 특화 게시판
* **빠른 검색:** Elasticsearch 연동을 통한 대용량 게시글/제목 초고속 검색 지원
* **계층형 댓글:** JPA + QueryDSL을 이용한 복잡한 대댓글 쿼리 최적화
### 🛒 5. 실시간 공동구매 시스템 (Group Purchase)
* **모집 및 매칭:** 구매할 물품과 목표 인원을 등록하여 모집
* **동시성 제어:** 다수의 사용자가 단기간에 몰릴 때 발생하는 동시성 이슈를 RDB Lock 및 Redis를 통해 안전하게 방어
* **상태 관리 및 알림:** Spring Scheduler + EventListener로 제한시간 내 목표 인원 도달 여부를 판별하여 상태를 자동 업데이트하고 참여자들에게 결과 알림 전송
* **결제 안내:** 목표 달성 시 1인당 결제 금액 및 입금 계좌 고지
---
## 🛠 기술 스택 (Tech Stack)
### Backend Ecosystem
- **Framework:** Java, Spring Boot, Spring Security, Spring Data JPA, QueryDSL
- **Database:** MySQL (RDB), Redis (Cache, GEO, Concurrency), Elasticsearch (Search Engine)
- **Messaging & Event:** FCM (Push Notification), Spring Application Event, Google SMTP
- **Task Scheduling:** Spring `@Scheduled`, ShedLock (다중 서버 스케줄링 동기화)
- **External API:** Kakao Geocoding API
- **Utils:** Apache POI (Excel Export)


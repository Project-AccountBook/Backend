# Joint Living / 공동구매
주부를 위한 가계부 포트폴리오 및 공동구매 커뮤니티

# 기능

## 회원 관리
### 기술: Spring Security + JWT + OAuth2 Client / Redis / FCM + Spring Application Event / Spring Boot Starter Mail + Google SMTP
### Entity: User, UserSetting, Notification, InterestCategory
- **인증 및 로그인**: 일반 회원가입/로그인, 소셜 간편 로그인 연동, 로그인 유지 기능
- **계정 관리**: 이메일 인증을 통한 계정 찾기, 로그아웃, 회원 탈퇴
- **권한 및 프로필**: 일반/관리자 권한 접근 제어, 마이페이지 프로필 설정
- **사용자 맞춤 설정**: 정기 고정 수입/지출 스케줄 설정
- **알림 설정**: 예산 소진 알림 조건 설정, 공동구매 관심 카테고리 알림 설정

## 가계부 포트폴리오
### 기술: RDB(DB Indexing) / Redis(Caching) / Spring @Scheduled + ShedLock / Apache POI
### Entity: Account, Transaction, FixedTransaction, Category, Budget
- **자산 및 내역 관리**: 계좌 초기 잔고 설정 및 실시간 잔고 업데이트, 수입/지출 내역
- **카테고리 관리**: 시스템 기본 카테고리 제공 및 사용자 맞춤형 커스텀 카테고리 기능
- **소비 분석**: 월별 및 사용자 지정 기간별 수입/지출 총액 및 상세 내역 조회
- **예산 관리**: 다가오는 달 및 이번 달 총 예산 설정, 추가 예상 지출 지정
- **데이터 관리 및 공유**: 가계부 내역 파일 내보내기(CSV/Excel 다운로드), 포트폴리오 타인 공개 여부 설정

## 다른 사용자와의 예산 및 지출 비교
### 기술: DB Indexing / Spring Scheduler / Kakao Geocoding API / Redis GEO
### Entity: -
- 타 사용자와의 예산, 지출, 수입 비교
- 나이대, 단순 금액, 카테고리 별 비교
- 위치 기반으로 사용자 근처 사용자들의 평균 수입 포트폴리오와의 나이대, 단순 금액, 카테고리 별 비교

## Q&A 게시판 및 노하우 공유 게시판
### 기술: RDB / JPA / QueryDSL / Elasticsearch / Redis Cache
### Entity: Board, Comment, Image
- 특정 주제에 대해 Q&A를 진행 가능한 게시판
- 특정 주제에 대한 노하우 공유 게시판
- 게시물 목록 조회, 작성, 수정, 삭제, 조회, 검색
- 댓글 작성, 댓글 수정, 댓글 삭제, 대댓글 작성, 대댓글 수정, 대댓글 삭제 

## 공동구매
### 기술: RDB 동시성 제어 / Redis / Spring Schedular / EventListener
### Entity: 공동구매게시글, 공동구매참여인원정보
- 공동구매를 원하는 물품, 인원 수 등록
- 관심 카테고리로 설정해 둔 사용자에게 알림 전송
- 제한시간 내 인원이 모이지 않는다면
    - 지원한 사용자들에게 해당 알림 전송
- 제한시간 내 인원이 모인다면
    - 참여 인원 저장
        - 1인당 결제 금액 고지
        - 입금 계좌 고지
        - 여유 되면 PG 붙여보기

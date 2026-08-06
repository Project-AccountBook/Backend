-- 2026-08-05: Account / Notification / UserDevice 에 user_id 기반 인덱스 추가
--
-- 대상 쿼리:
--   Account:      findByUserId (사용자 계정 목록, 매 거래 생성 경로에서 반복)
--   Notification: softDeleteByUserId (계정 삭제 시 벌크 UPDATE) + findByUser 목록
--   UserDevice:   findByUserId (알림 발송 시 FCM 토큰 조회)
--
-- 실행 순서: prod ddl-auto=validate 이므로 배포 전에 아래 문장들을 수동 실행.
-- 롤백: DROP INDEX ...

CREATE INDEX idx_account_user          ON account       (user_id);
CREATE INDEX idx_account_user_name     ON account       (user_id, account_name);
CREATE INDEX idx_notification_user     ON notification  (user_id);
CREATE INDEX idx_userdevice_user       ON user_device   (user_id);

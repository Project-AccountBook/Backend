-- 2026-08-05: Board / Comment 에 user_id 기반 인덱스 추가
--
-- 대상 쿼리:
--   Board:   findByUserIdAndType, findByUserId (사용자 프로필 게시글 목록)
--   Comment: 관리자 사용자별 댓글 조회, 사용자 활동 이력
--
-- 실행 순서: prod ddl-auto=validate 이므로 배포 전에 아래 두 문장을 수동 실행.
-- 롤백: DROP INDEX ...
--
-- 참고: 두 테이블 모두 IDENTITY PK + BaseEntity(created_at) 상속.
--       Board.user_id 는 (user_id, type) 복합으로 잡아 findByUserId 단일 조건에도 커버됨.

CREATE INDEX idx_board_user_type ON board (user_id, type);
CREATE INDEX idx_comment_user ON comment (user_id, created_at);

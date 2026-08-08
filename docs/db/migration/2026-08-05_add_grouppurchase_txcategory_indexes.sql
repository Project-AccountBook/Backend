-- 2026-08-05: GroupPurchase / TransactionCategory 조회 인덱스 추가
--
-- 대상 쿼리:
--   GroupPurchase:       findActiveGroupPurchases (WHERE status=? AND category_id=? ORDER BY deadline)
--   TransactionCategory: existsByUserIdAndNameAndType, findByUserIdAndNameAndTypeIncludingDeleted,
--                        findAllByUserOrSystem (user_id 필터)
--
-- 실행 순서: prod ddl-auto=validate 이므로 배포 전에 아래 문장들을 수동 실행.
-- 롤백: DROP INDEX ...
--
-- 참고: category_id 가 null 로 넘어오는 케이스는 status prefix 만 사용됨 (leftmost). 필터 걸린 경우
--       (status, category_id, deadline) 3열 모두 활용해 filter + sort 를 인덱스로 흡수.

CREATE INDEX idx_gp_status_category_deadline
    ON group_purchase (status, category_id, deadline);

CREATE INDEX idx_txcategory_user_name_type
    ON transaction_category (user_id, name, type);

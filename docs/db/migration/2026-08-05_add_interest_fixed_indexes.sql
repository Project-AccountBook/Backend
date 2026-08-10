-- 2026-08-05: InterestCategory / FixedTransaction 벌크 삭제 관련 인덱스
--
-- 대상 쿼리:
--   InterestCategory:  findByUserId (관심 카테고리 목록), softDeleteByUserId (계정 삭제 시 벌크 UPDATE)
--   FixedTransaction:  softDeleteByAccountId (계정 삭제 시), softDeleteByTransactionCategoryId (카테고리 삭제 시)
--
-- 실행 순서: prod ddl-auto=validate 이므로 배포 전에 아래 문장들을 수동 실행.
-- 롤백: DROP INDEX ...

CREATE INDEX idx_interest_user              ON interest_category (user_id);
CREATE INDEX idx_fixed_transaction_account  ON fixed_transaction (account_id);
CREATE INDEX idx_fixed_transaction_category ON fixed_transaction (category_id);

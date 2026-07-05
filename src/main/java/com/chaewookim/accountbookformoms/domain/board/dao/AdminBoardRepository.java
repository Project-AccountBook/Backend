package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 관리자 조회 전용 — {@link Board} 의 {@code @SQLRestriction("deleted_at IS NULL")} 를 우회하기 위해
 * 네이티브 쿼리로 소프트 삭제(user-deleted) 행까지 함께 반환한다.
 * <p>Pageable 의 Sort 는 네이티브 쿼리와 정합성 이슈가 있어 정렬은 하드코딩(created_at DESC)한다.</p>
 */
@Repository
public interface AdminBoardRepository extends JpaRepository<Board, Long> {

    @Query(value = "SELECT * FROM board ORDER BY created_at DESC LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<Board> findAllIncludingDeleted(@Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM board", nativeQuery = true)
    long countIncludingDeleted();

    @Query(value = "SELECT * FROM board WHERE type = :type " +
            "ORDER BY created_at DESC LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<Board> findAllIncludingDeletedByType(@Param("type") String type,
                                              @Param("size") int size,
                                              @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM board WHERE type = :type", nativeQuery = true)
    long countIncludingDeletedByType(@Param("type") String type);
}

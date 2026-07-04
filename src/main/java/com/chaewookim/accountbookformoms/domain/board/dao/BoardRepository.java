package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Board b SET b.views = b.views + :delta WHERE b.id = :id")
    int increaseViews(@Param("id") Long id, @Param("delta") long delta);

    Page<Board> findByType(BOARD_TYPE type, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT b FROM Board b WHERE b.type = :type AND b.createdAt >= :since")
    List<Board> findRecentByType(@Param("type") BOARD_TYPE type, @Param("since") java.time.LocalDateTime since);
}

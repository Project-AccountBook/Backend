package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Board b SET b.views = b.views + :delta WHERE b.id = :id")
    int increaseViews(@Param("id") Long id, @Param("delta") long delta);
}

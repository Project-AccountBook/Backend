package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // TODO: Elasticsearch nori 전환 예정 — 현재는 JPA LIKE fallback (CLAUDE.md #3)
    @Query("""
            SELECT b FROM Board b
            WHERE b.title LIKE CONCAT('%', :keyword, '%')
               OR b.content LIKE CONCAT('%', :keyword, '%')
            """)
    Page<Board> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

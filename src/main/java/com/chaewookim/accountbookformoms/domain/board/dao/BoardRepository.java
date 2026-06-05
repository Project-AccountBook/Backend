package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
}

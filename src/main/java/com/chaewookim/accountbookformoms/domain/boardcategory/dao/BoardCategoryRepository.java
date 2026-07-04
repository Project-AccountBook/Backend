package com.chaewookim.accountbookformoms.domain.boardcategory.dao;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.boardcategory.entity.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    List<BoardCategory> findByBoardTypeOrderByDisplayOrderAsc(BOARD_TYPE boardType);

    boolean existsByBoardTypeAndName(BOARD_TYPE boardType, String name);
}

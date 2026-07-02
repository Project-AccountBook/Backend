package com.chaewookim.accountbookformoms.domain.boardcategory.application;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.boardcategory.dao.BoardCategoryRepository;
import com.chaewookim.accountbookformoms.domain.boardcategory.dto.response.BoardCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCategoryService {

    private final BoardCategoryRepository repository;

    public List<BoardCategoryResponse> list(BOARD_TYPE boardType) {
        List<com.chaewookim.accountbookformoms.domain.boardcategory.entity.BoardCategory> found = boardType == null
                ? repository.findAll()
                : repository.findByBoardTypeOrderByDisplayOrderAsc(boardType);
        return found.stream().map(BoardCategoryResponse::from).toList();
    }
}

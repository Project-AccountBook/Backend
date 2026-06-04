package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBoardService {

    private final BoardRepository boardRepository;

    public Long deleteByAdmin(Long postId) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
        board.markAsAdminDeleted();
        return board.getId();
    }
}

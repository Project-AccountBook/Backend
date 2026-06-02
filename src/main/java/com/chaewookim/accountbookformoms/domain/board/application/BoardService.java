package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardCreateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardUpdateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardCreateResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardUpdateResponse;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    public Page<BoardResponse> list(Pageable pageable) {
        return boardRepository.findAll(pageable).map(BoardResponse::from);
    }

    @Transactional
    public BoardCreateResponse create(BoardCreateRequest request, Long userId) {
        Board board = Board.builder()
                .userId(userId)
                .categoryId(request.categoryId())
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .build();

        Board saved = boardRepository.save(board);
        return new BoardCreateResponse(saved.getId(), saved.getTitle());
    }

    public BoardResponse get(Long postId) {
        Board board = findBoardOrThrow(postId);
        return BoardResponse.from(board);
    }

    @Transactional
    public BoardUpdateResponse update(Long postId, BoardUpdateRequest request, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        board.update(request.title(), request.content(), request.type());
        return new BoardUpdateResponse(board.getId(), board.getTitle());
    }

    @Transactional
    public Long delete(Long postId, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        boardRepository.delete(board);
        return board.getId();
    }

    // TODO: Elasticsearch nori 전환 예정 — 현재는 JPA LIKE fallback
    public Page<BoardResponse> search(String keyword, Pageable pageable) {
        return boardRepository.searchByKeyword(keyword, pageable).map(BoardResponse::from);
    }

    private Board findBoardOrThrow(Long postId) {
        return boardRepository.findById(postId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
    }

    private void validateOwner(Board board, Long userId) {
        if (!board.getUserId().equals(userId)) {
            throw new CustomException(BoardErrorCode.BOARD_ACCESS_DENIED);
        }
    }
}

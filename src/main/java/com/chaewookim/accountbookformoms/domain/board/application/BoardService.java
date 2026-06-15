package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchQueryRepository;
import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardCreateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardUpdateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardCreateResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardSearchResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardUpdateResponse;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardSearchQueryRepository boardSearchQueryRepository;
    private final BoardViewCountService viewCountService;
    private final ApplicationEventPublisher eventPublisher;

    public Page<BoardResponse> list(Pageable pageable) {
        Page<Board> page = boardRepository.findAll(pageable);
        List<Long> ids = page.getContent().stream().map(Board::getId).toList();
        Map<Long, Long> pending = viewCountService.getPendingDeltas(ids);
        return page.map(b -> BoardResponse.from(b, pending.getOrDefault(b.getId(), 0L)));
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
        eventPublisher.publishEvent(BoardChangedEvent.upsert(saved.getId()));
        return new BoardCreateResponse(saved.getId(), saved.getTitle());
    }

    public BoardResponse get(Long postId) {
        Board board = findBoardOrThrow(postId);
        long pending = viewCountService.increment(postId);
        return BoardResponse.from(board, pending);
    }

    @Transactional
    public BoardUpdateResponse update(Long postId, BoardUpdateRequest request, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        board.update(request.title(), request.content(), request.type());
        eventPublisher.publishEvent(BoardChangedEvent.upsert(board.getId()));
        return new BoardUpdateResponse(board.getId(), board.getTitle());
    }

    @Transactional
    public Long delete(Long postId, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        boardRepository.delete(board);
        eventPublisher.publishEvent(BoardChangedEvent.delete(board.getId()));
        return board.getId();
    }

    public Page<BoardSearchResponse> search(String keyword, Pageable pageable) {
        Page<BoardDocument> hits = boardSearchQueryRepository.search(keyword, pageable);
        return hits.map(BoardSearchResponse::from);
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

package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.AdminBoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dto.response.AdminBoardResponse;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBoardService {

    private final BoardRepository boardRepository;
    private final AdminBoardRepository adminBoardRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Long deleteByAdmin(Long postId) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
        board.markAsAdminDeleted();
        eventPublisher.publishEvent(BoardChangedEvent.delete(board.getId()));
        return board.getId();
    }

    /**
     * 관리자 게시물 목록 조회.
     *
     * @param type            null 이면 전체
     * @param includeDeleted  true 면 유저가 소프트 삭제한 게시물까지 포함 (native query)
     * @param pageable        Pageable (Sort 는 native 경로에서 무시되며 created_at DESC 고정)
     */
    @Transactional(readOnly = true)
    public Page<AdminBoardResponse> list(BOARD_TYPE type, boolean includeDeleted, Pageable pageable) {
        int size = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        Page<Board> page;
        if (includeDeleted) {
            if (type == null) {
                List<Board> rows = adminBoardRepository.findAllIncludingDeleted(size, offset);
                long total = adminBoardRepository.countIncludingDeleted();
                page = new PageImpl<>(rows, pageable, total);
            } else {
                List<Board> rows = adminBoardRepository
                        .findAllIncludingDeletedByType(type.name(), size, offset);
                long total = adminBoardRepository.countIncludingDeletedByType(type.name());
                page = new PageImpl<>(rows, pageable, total);
            }
        } else {
            page = (type == null)
                    ? boardRepository.findAll(pageable)
                    : boardRepository.findByType(type, pageable);
        }
        return page.map(AdminBoardResponse::from);
    }
}

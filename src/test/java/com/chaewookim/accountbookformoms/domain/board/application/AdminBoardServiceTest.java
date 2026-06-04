package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminBoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private AdminBoardService adminBoardService;

    private static final Long POST_ID = 100L;
    private static final Long OWNER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    private Board buildBoard(Long id, Long userId) {
        Board board = Board.builder()
                .userId(userId)
                .categoryId(CATEGORY_ID)
                .title("제목")
                .content("본문")
                .type(BOARD_TYPE.QNA)
                .build();
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    @Test
    @DisplayName("성공 — 관리자가 게시물을 admin-deleted로 표시")
    void deleteByAdmin_success() {
        // given
        Board board = buildBoard(POST_ID, OWNER_ID);
        given(boardRepository.findById(POST_ID)).willReturn(Optional.of(board));

        // when
        Long deletedId = adminBoardService.deleteByAdmin(POST_ID);

        // then
        assertThat(deletedId).isEqualTo(POST_ID);
        assertThat(board.isAdminDeleted()).isTrue();
    }

    @Test
    @DisplayName("실패 — 존재하지 않는 게시물이면 BOARD_NOT_FOUND")
    void deleteByAdmin_fail_not_found() {
        // given
        given(boardRepository.findById(POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminBoardService.deleteByAdmin(POST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);
    }
}

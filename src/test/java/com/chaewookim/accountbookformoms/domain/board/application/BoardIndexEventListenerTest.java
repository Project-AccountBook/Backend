package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchRepository;
import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.tag.application.TagService;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardIndexEventListenerTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardSearchRepository boardSearchRepository;

    @Mock
    private TagService tagService;

    @InjectMocks
    private BoardIndexEventListener listener;

    private static final Long BOARD_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    private Board buildBoard() {
        Board board = Board.builder()
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .title("절약 노하우")
                .content("이번 달 절약법")
                .type(BOARD_TYPE.QNA)
                .build();
        ReflectionTestUtils.setField(board, "id", BOARD_ID);
        return board;
    }

    @Test
    @DisplayName("UPSERT — 게시물이 존재하면 태그와 함께 ES에 색인")
    void handle_upsert_indexes_when_board_exists() {
        Board board = buildBoard();
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.of(board));
        given(tagService.tagsOfBoard(BOARD_ID)).willReturn(java.util.List.of("절약", "재테크"));

        listener.handle(BoardChangedEvent.upsert(BOARD_ID));

        ArgumentCaptor<BoardDocument> captor = ArgumentCaptor.forClass(BoardDocument.class);
        verify(boardSearchRepository).save(captor.capture());
        BoardDocument indexed = captor.getValue();
        assertThat(indexed.getId()).isEqualTo(BOARD_ID);
        assertThat(indexed.getTitle()).isEqualTo("절약 노하우");
        assertThat(indexed.getContent()).isEqualTo("이번 달 절약법");
        assertThat(indexed.getType()).isEqualTo(BOARD_TYPE.QNA.name());
        assertThat(indexed.getUserId()).isEqualTo(USER_ID);
        assertThat(indexed.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(indexed.getTags()).containsExactly("절약", "재테크");
    }

    @Test
    @DisplayName("UPSERT — 게시물이 없으면 색인 호출 없음")
    void handle_upsert_skips_when_board_missing() {
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.empty());

        listener.handle(BoardChangedEvent.upsert(BOARD_ID));

        verify(boardSearchRepository, never()).save(any(BoardDocument.class));
    }

    @Test
    @DisplayName("DELETE — ES 인덱스에서 삭제")
    void handle_delete_removes_from_index() {
        listener.handle(BoardChangedEvent.delete(BOARD_ID));

        verify(boardSearchRepository).deleteById(BOARD_ID);
        verify(boardRepository, never()).findById(any());
    }

    @Test
    @DisplayName("ES 호출 실패해도 예외 전파 없이 로깅만")
    void handle_swallows_es_exception() {
        Board board = buildBoard();
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.of(board));
        given(tagService.tagsOfBoard(BOARD_ID)).willReturn(java.util.List.of());
        willThrow(new RuntimeException("ES down"))
                .given(boardSearchRepository).save(any(BoardDocument.class));

        assertThatCode(() -> listener.handle(BoardChangedEvent.upsert(BOARD_ID)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DELETE — ES 삭제 실패해도 예외 전파 없음")
    void handle_swallows_es_delete_exception() {
        willThrow(new RuntimeException("ES down"))
                .given(boardSearchRepository).deleteById(BOARD_ID);

        assertThatCode(() -> listener.handle(BoardChangedEvent.delete(BOARD_ID)))
                .doesNotThrowAnyException();
    }
}

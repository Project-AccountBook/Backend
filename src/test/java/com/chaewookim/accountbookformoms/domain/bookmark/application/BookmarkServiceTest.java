package com.chaewookim.accountbookformoms.domain.bookmark.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.bookmark.dao.BookmarkRepository;
import com.chaewookim.accountbookformoms.domain.bookmark.dto.response.BookmarkToggleResponse;
import com.chaewookim.accountbookformoms.domain.bookmark.entity.Bookmark;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private BoardRepository boardRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    private static final Long USER_ID = 1L;
    private static final Long BOARD_ID = 100L;

    @Test
    @DisplayName("toggle — 북마크 없던 상태에서 저장 후 true 반환")
    void toggle_add() {
        given(boardRepository.existsById(BOARD_ID)).willReturn(true);
        given(bookmarkRepository.findByUserIdAndBoardId(USER_ID, BOARD_ID)).willReturn(Optional.empty());

        BookmarkToggleResponse response = bookmarkService.toggle(BOARD_ID, USER_ID);

        assertThat(response.bookmarked()).isTrue();
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("toggle — 북마크 있던 상태에서 삭제 후 false 반환")
    void toggle_remove() {
        Bookmark existing = Bookmark.builder().userId(USER_ID).boardId(BOARD_ID).build();
        given(boardRepository.existsById(BOARD_ID)).willReturn(true);
        given(bookmarkRepository.findByUserIdAndBoardId(USER_ID, BOARD_ID)).willReturn(Optional.of(existing));

        BookmarkToggleResponse response = bookmarkService.toggle(BOARD_ID, USER_ID);

        assertThat(response.bookmarked()).isFalse();
        verify(bookmarkRepository).delete(existing);
    }

    @Test
    @DisplayName("toggle 실패 — 게시물이 존재하지 않으면 BOARD_NOT_FOUND")
    void toggle_fail_board_not_found() {
        given(boardRepository.existsById(BOARD_ID)).willReturn(false);

        assertThatThrownBy(() -> bookmarkService.toggle(BOARD_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    @DisplayName("isBookmarked — viewerId 가 null 이면 항상 false")
    void isBookmarked_null_viewer() {
        assertThat(bookmarkService.isBookmarked(BOARD_ID, null)).isFalse();
        verify(bookmarkRepository, never()).findByUserIdAndBoardId(any(), any());
    }

    @Test
    @DisplayName("bookmarkedBoards — viewerId null 또는 빈 리스트면 빈 Set")
    void bookmarkedBoards_empty_cases() {
        assertThat(bookmarkService.bookmarkedBoards(List.of(1L, 2L), null)).isEmpty();
        assertThat(bookmarkService.bookmarkedBoards(List.of(), USER_ID)).isEmpty();
        verify(bookmarkRepository, never()).findByUserIdAndBoardIdIn(any(), any());
    }

    @Test
    @DisplayName("bookmarkedBoards — DB에서 조회한 board_id 집합 반환")
    void bookmarkedBoards_returns_set() {
        Bookmark b1 = Bookmark.builder().userId(USER_ID).boardId(1L).build();
        Bookmark b2 = Bookmark.builder().userId(USER_ID).boardId(3L).build();
        given(bookmarkRepository.findByUserIdAndBoardIdIn(USER_ID, List.of(1L, 2L, 3L)))
                .willReturn(List.of(b1, b2));

        Set<Long> result = bookmarkService.bookmarkedBoards(List.of(1L, 2L, 3L), USER_ID);

        assertThat(result).containsExactlyInAnyOrder(1L, 3L);
    }
}

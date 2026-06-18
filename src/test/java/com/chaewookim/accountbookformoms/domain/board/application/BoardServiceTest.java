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
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardSearchQueryRepository boardSearchQueryRepository;

    @Mock
    private BoardViewCountService viewCountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BoardService boardService;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 100L;
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

    @Nested
    @DisplayName("게시물 목록 조회")
    class List_ {

        @Test
        @DisplayName("성공 — 페이지 단위로 BoardResponse 반환")
        void list_success() {
            Pageable pageable = PageRequest.of(0, 10);
            Board board = buildBoard(POST_ID, OWNER_ID);
            given(boardRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(board), pageable, 1));
            given(viewCountService.getPendingDeltas(anyCollection())).willReturn(Map.of());

            Page<BoardResponse> result = boardService.list(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(POST_ID);
            assertThat(result.getContent().get(0).userId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("성공 — 응답 views 는 DB views + Redis pending delta")
        void list_addsPendingViews() {
            Pageable pageable = PageRequest.of(0, 10);
            Board board = buildBoard(POST_ID, OWNER_ID);
            ReflectionTestUtils.setField(board, "views", 10);
            given(boardRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(board), pageable, 1));
            given(viewCountService.getPendingDeltas(anyCollection()))
                    .willReturn(Map.of(POST_ID, 7L));

            Page<BoardResponse> result = boardService.list(pageable);

            assertThat(result.getContent().get(0).views()).isEqualTo(17);
        }
    }

    @Nested
    @DisplayName("게시물 생성")
    class Create_ {

        @Test
        @DisplayName("성공 — 저장 후 UPSERT 이벤트 발행")
        void create_success() {
            BoardCreateRequest request = new BoardCreateRequest(
                    CATEGORY_ID, "새 제목", "새 본문", BOARD_TYPE.QNA);
            Board saved = buildBoard(POST_ID, OWNER_ID);
            given(boardRepository.save(any(Board.class))).willReturn(saved);

            BoardCreateResponse response = boardService.create(request, OWNER_ID);

            assertThat(response.id()).isEqualTo(POST_ID);
            assertThat(response.title()).isEqualTo(saved.getTitle());
            verify(boardRepository).save(any(Board.class));

            ArgumentCaptor<BoardChangedEvent> captor = ArgumentCaptor.forClass(BoardChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().boardId()).isEqualTo(POST_ID);
            assertThat(captor.getValue().type()).isEqualTo(BoardChangedEvent.Type.UPSERT);
        }
    }

    @Nested
    @DisplayName("게시물 단건 조회")
    class Get_ {

        @Test
        @DisplayName("성공 — id로 게시물 조회 시 Redis INCR + 응답에 합산")
        void get_success() {
            Board board = buildBoard(POST_ID, OWNER_ID);
            ReflectionTestUtils.setField(board, "views", 5);
            given(boardRepository.findById(POST_ID)).willReturn(Optional.of(board));
            given(viewCountService.increment(POST_ID)).willReturn(3L);

            BoardResponse response = boardService.get(POST_ID);

            assertThat(response.id()).isEqualTo(POST_ID);
            assertThat(response.title()).isEqualTo(board.getTitle());
            assertThat(response.type()).isEqualTo(BOARD_TYPE.QNA);
            assertThat(response.views()).isEqualTo(8);
            verify(viewCountService).increment(eq(POST_ID));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시물이면 BOARD_NOT_FOUND")
        void get_fail_not_found() {
            given(boardRepository.findById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> boardService.get(POST_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("게시물 수정")
    class Update_ {

        @Test
        @DisplayName("성공 — 작성자가 자기 게시물 수정 후 UPSERT 이벤트 발행")
        void update_success() {
            Board board = buildBoard(POST_ID, OWNER_ID);
            BoardUpdateRequest request = new BoardUpdateRequest(
                    "수정된 제목", "수정된 본문", BOARD_TYPE.KNOWHOW);
            given(boardRepository.findById(POST_ID)).willReturn(Optional.of(board));

            BoardUpdateResponse response = boardService.update(POST_ID, request, OWNER_ID);

            assertThat(response.id()).isEqualTo(POST_ID);
            assertThat(response.title()).isEqualTo("수정된 제목");
            assertThat(board.getTitle()).isEqualTo("수정된 제목");
            assertThat(board.getContent()).isEqualTo("수정된 본문");
            assertThat(board.getType()).isEqualTo(BOARD_TYPE.KNOWHOW);

            ArgumentCaptor<BoardChangedEvent> captor = ArgumentCaptor.forClass(BoardChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(BoardChangedEvent.Type.UPSERT);
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시물이면 BOARD_NOT_FOUND")
        void update_fail_not_found() {
            BoardUpdateRequest request = new BoardUpdateRequest("t", "c", BOARD_TYPE.QNA);
            given(boardRepository.findById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> boardService.update(POST_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("실패 — 작성자가 아니면 BOARD_ACCESS_DENIED 발생 및 내용 미변경")
        void update_fail_not_owner() {
            Board board = buildBoard(POST_ID, OWNER_ID);
            String originalTitle = board.getTitle();
            BoardUpdateRequest request = new BoardUpdateRequest("hacked", "hacked", BOARD_TYPE.QNA);
            given(boardRepository.findById(POST_ID)).willReturn(Optional.of(board));

            assertThatThrownBy(() -> boardService.update(POST_ID, request, OTHER_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_ACCESS_DENIED);

            assertThat(board.getTitle()).isEqualTo(originalTitle);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("게시물 삭제")
    class Delete_ {

        @Test
        @DisplayName("성공 — 작성자 삭제 후 DELETE 이벤트 발행")
        void delete_success() {
            Board board = buildBoard(POST_ID, OWNER_ID);
            given(boardRepository.findById(POST_ID)).willReturn(Optional.of(board));

            Long deletedId = boardService.delete(POST_ID, OWNER_ID);

            assertThat(deletedId).isEqualTo(POST_ID);
            verify(boardRepository).delete(board);

            ArgumentCaptor<BoardChangedEvent> captor = ArgumentCaptor.forClass(BoardChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().boardId()).isEqualTo(POST_ID);
            assertThat(captor.getValue().type()).isEqualTo(BoardChangedEvent.Type.DELETE);
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시물이면 BOARD_NOT_FOUND")
        void delete_fail_not_found() {
            given(boardRepository.findById(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> boardService.delete(POST_ID, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);

            verify(boardRepository, never()).delete(any(Board.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("실패 — 작성자가 아니면 BOARD_ACCESS_DENIED 발생 및 삭제 미호출")
        void delete_fail_not_owner() {
            Board board = buildBoard(POST_ID, OWNER_ID);
            given(boardRepository.findById(POST_ID)).willReturn(Optional.of(board));

            assertThatThrownBy(() -> boardService.delete(POST_ID, OTHER_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_ACCESS_DENIED);

            verify(boardRepository, never()).delete(any(Board.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("게시물 검색 (Elasticsearch)")
    class Search_ {

        @Test
        @DisplayName("성공 — keyword로 ES 검색 후 BoardSearchResponse 매핑")
        void search_success() {
            String keyword = "절약";
            Pageable pageable = PageRequest.of(0, 10);
            BoardDocument doc = BoardDocument.builder()
                    .id(POST_ID)
                    .userId(OWNER_ID)
                    .categoryId(CATEGORY_ID)
                    .title("절약 노하우")
                    .content("절약 본문")
                    .type(BOARD_TYPE.QNA.name())
                    .build();
            given(boardSearchQueryRepository.search(keyword, pageable))
                    .willReturn(new PageImpl<>(List.of(doc), pageable, 1));

            Page<BoardSearchResponse> result = boardService.search(keyword, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(POST_ID);
            assertThat(result.getContent().get(0).title()).isEqualTo("절약 노하우");
            verify(boardSearchQueryRepository).search(keyword, pageable);
        }

        @Test
        @DisplayName("성공 — 일치 결과 없으면 빈 페이지 반환")
        void search_empty() {
            String keyword = "없는키워드";
            Pageable pageable = PageRequest.of(0, 10);
            given(boardSearchQueryRepository.search(keyword, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<BoardSearchResponse> result = boardService.search(keyword, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }
}

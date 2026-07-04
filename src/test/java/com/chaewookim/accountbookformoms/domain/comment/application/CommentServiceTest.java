package com.chaewookim.accountbookformoms.domain.comment.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentCreateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentUpdateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.like.application.PostLikeService;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private GroupPurchaseRepository groupPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeService likeService;

    @InjectMocks
    private CommentService commentService;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 1000L;
    private static final Long PARENT_ID = 999L;
    private static final Long REPLY_ID = 1001L;

    private Comment buildComment(Long id, Long userId, Long referenceId, ReferenceType type, Long parentId) {
        Comment comment = Comment.builder()
                .userId(userId)
                .referenceId(referenceId)
                .referenceType(type)
                .parentId(parentId)
                .content("본문")
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Nested
    @DisplayName("댓글 생성")
    class Create_ {

        @Test
        @DisplayName("성공 — 댓글이 저장되고 id 반환")
        void create_success() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "새 댓글");
            Comment saved = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            given(boardRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.save(any(Comment.class))).willReturn(saved);

            // when
            Long resultId = commentService.create(POST_ID, request, OWNER_ID);

            // then
            assertThat(resultId).isEqualTo(COMMENT_ID);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 대상 QNA 게시글이 존재하지 않으면 BOARD_NOT_FOUND")
        void create_fail_board_not_found() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "새 댓글");
            given(boardRepository.existsById(POST_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentService.create(POST_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 대상 공동구매 글이 존재하지 않으면 GROUP_PURCHASE_NOT_FOUND")
        void create_fail_group_purchase_not_found() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.GROUPPURCHASE, "새 댓글");
            given(groupPurchaseRepository.existsById(POST_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentService.create(POST_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_PURCHASE_NOT_FOUND);

            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("대댓글 생성")
    class Reply_ {

        @Test
        @DisplayName("성공 — 1depth 댓글에 대댓글 작성")
        void reply_success() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "대댓글");
            Comment parent = buildComment(PARENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            Comment saved = buildComment(REPLY_ID, OWNER_ID, POST_ID, ReferenceType.QNA, PARENT_ID);
            given(boardRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));
            given(commentRepository.save(any(Comment.class))).willReturn(saved);

            // when
            Long resultId = commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID);

            // then
            assertThat(resultId).isEqualTo(REPLY_ID);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 대댓글 작성 시 대상 QNA 게시글이 존재하지 않으면 BOARD_NOT_FOUND")
        void reply_fail_board_not_found() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "대댓글");
            given(boardRepository.existsById(POST_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 대댓글 작성 시 대상 공동구매 글이 존재하지 않으면 GROUP_PURCHASE_NOT_FOUND")
        void reply_fail_group_purchase_not_found() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.GROUPPURCHASE, "대댓글");
            given(groupPurchaseRepository.existsById(POST_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_PURCHASE_NOT_FOUND);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 부모 댓글이 존재하지 않으면 COMMENT_NOT_FOUND")
        void reply_fail_parent_not_found() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "대댓글");
            given(boardRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findById(PARENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 부모가 이미 대댓글이면 COMMENT_REPLY_DEPTH_EXCEEDED")
        void reply_fail_depth_exceeded() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "대대댓글");
            Comment parentReply = buildComment(PARENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, 998L);
            given(boardRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findById(PARENT_ID)).willReturn(Optional.of(parentReply));

            // when & then
            assertThatThrownBy(() -> commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 부모의 referenceId가 다르면 COMMENT_REFERENCE_MISMATCH")
        void reply_fail_reference_id_mismatch() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "대댓글");
            Comment parent = buildComment(PARENT_ID, OWNER_ID, 999L, ReferenceType.QNA, null);
            given(boardRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));

            // when & then
            assertThatThrownBy(() -> commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_REFERENCE_MISMATCH);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 부모의 referenceType이 다르면 COMMENT_REFERENCE_MISMATCH")
        void reply_fail_reference_type_mismatch() {
            // given
            CommentCreateRequest request = new CommentCreateRequest(ReferenceType.QNA, "대댓글");
            Comment parent = buildComment(PARENT_ID, OWNER_ID, POST_ID, ReferenceType.GROUPPURCHASE, null);
            given(boardRepository.existsById(POST_ID)).willReturn(true);
            given(commentRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));

            // when & then
            assertThatThrownBy(() -> commentService.reply(POST_ID, PARENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_REFERENCE_MISMATCH);

            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class Update_ {

        @Test
        @DisplayName("성공 — 작성자가 본인 댓글 수정")
        void update_success() {
            // given
            Comment comment = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when
            Long resultId = commentService.update(COMMENT_ID, request, OWNER_ID);

            // then
            assertThat(resultId).isEqualTo(COMMENT_ID);
            assertThat(comment.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("실패 — 존재하지 않으면 COMMENT_NOT_FOUND")
        void update_fail_not_found() {
            // given
            CommentUpdateRequest request = new CommentUpdateRequest("수정");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.update(COMMENT_ID, request, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 — 작성자가 아니면 COMMENT_ACCESS_DENIED 및 내용 미변경")
        void update_fail_not_owner() {
            // given
            Comment comment = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            String originalContent = comment.getContent();
            CommentUpdateRequest request = new CommentUpdateRequest("hacked");
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.update(COMMENT_ID, request, OTHER_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_ACCESS_DENIED);

            assertThat(comment.getContent()).isEqualTo(originalContent);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class Delete_ {

        @Test
        @DisplayName("성공 — 작성자가 본인 댓글 삭제")
        void delete_success() {
            // given
            Comment comment = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when
            Long resultId = commentService.delete(COMMENT_ID, OWNER_ID);

            // then
            assertThat(resultId).isEqualTo(COMMENT_ID);
            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("실패 — 존재하지 않으면 COMMENT_NOT_FOUND")
        void delete_fail_not_found() {
            // given
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.delete(COMMENT_ID, OWNER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);

            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 작성자가 아니면 COMMENT_ACCESS_DENIED 및 삭제 미호출")
        void delete_fail_not_owner() {
            // given
            Comment comment = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.delete(COMMENT_ID, OTHER_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_ACCESS_DENIED);

            verify(commentRepository, never()).delete(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class List_ {

        @Test
        @DisplayName("성공 — 게시물의 댓글 목록을 CommentResponse로 매핑")
        void list_success() {
            // given
            Comment comment = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            given(commentRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(POST_ID, ReferenceType.QNA))
                    .willReturn(List.of(comment));

            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(OWNER_ID);
            given(mockUser.getUsername()).willReturn("홍길동");
            given(userRepository.findAllById(List.of(OWNER_ID))).willReturn(List.of(mockUser));

            // when
            List<CommentResponse> result = commentService.list(POST_ID, ReferenceType.QNA, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(COMMENT_ID);
            assertThat(result.get(0).content()).isEqualTo("본문");
            assertThat(result.get(0).authorNickname()).isEqualTo("홍길동");
            assertThat(result.get(0).deleted()).isFalse();
        }

        @Test
        @DisplayName("성공 — 빈 결과면 빈 리스트 반환")
        void list_empty() {
            // given
            given(commentRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(POST_ID, ReferenceType.QNA))
                    .willReturn(List.of());

            // when
            List<CommentResponse> result = commentService.list(POST_ID, ReferenceType.QNA, null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공 — soft-delete된 댓글은 안내 문구로 치환")
        void list_soft_deleted_shows_placeholder() {
            // given
            Comment deleted = buildComment(COMMENT_ID, OWNER_ID, POST_ID, ReferenceType.QNA, null);
            ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.now());
            given(commentRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtAsc(POST_ID, ReferenceType.QNA))
                    .willReturn(List.of(deleted));

            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(OWNER_ID);
            given(mockUser.getUsername()).willReturn("홍길동");
            given(userRepository.findAllById(List.of(OWNER_ID))).willReturn(List.of(mockUser));

            // when
            List<CommentResponse> result = commentService.list(POST_ID, ReferenceType.QNA, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).deleted()).isTrue();
            assertThat(result.get(0).content()).isEqualTo("삭제된 댓글입니다.");
            assertThat(result.get(0).authorNickname()).isEqualTo("홍길동");
        }
    }
}

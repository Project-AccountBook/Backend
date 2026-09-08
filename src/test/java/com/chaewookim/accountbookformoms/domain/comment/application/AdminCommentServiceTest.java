package com.chaewookim.accountbookformoms.domain.comment.application;

import com.chaewookim.accountbookformoms.domain.comment.dao.CommentRepository;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.AdminCommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.entity.Comment;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.domain.comment.error.CommentErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminCommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private AdminCommentService adminCommentService;

    private static final Long COMMENT_ID = 1000L;
    private static final Long OWNER_ID = 1L;
    private static final Long POST_ID = 100L;

    private Comment buildComment(Long id, Long userId) {
        Comment comment = Comment.builder()
                .userId(userId)
                .referenceId(POST_ID)
                .referenceType(ReferenceType.QNA)
                .parentId(null)
                .content("내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    @DisplayName("성공 — 관리자가 댓글을 admin-deleted로 표시")
    void deleteByAdmin_success() {
        // given
        Comment comment = buildComment(COMMENT_ID, OWNER_ID);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        // when
        Long deletedId = adminCommentService.deleteByAdmin(COMMENT_ID);

        // then
        assertThat(deletedId).isEqualTo(COMMENT_ID);
        assertThat(comment.isAdminDeleted()).isTrue();
    }

    @Test
    @DisplayName("실패 — 존재하지 않는 댓글이면 COMMENT_NOT_FOUND")
    void deleteByAdmin_fail_not_found() {
        // given
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminCommentService.deleteByAdmin(COMMENT_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("list — 필터 없으면 findAll(pageable) 위임")
    void list_no_filter() {
        Pageable pageable = PageRequest.of(0, 10);
        Comment c = buildComment(COMMENT_ID, OWNER_ID);
        given(commentRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(c), pageable, 1));

        Page<AdminCommentResponse> result = adminCommentService.list(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(COMMENT_ID);
    }

    @Test
    @DisplayName("list — referenceType 만 지정 시 typed 조회")
    void list_by_type() {
        Pageable pageable = PageRequest.of(0, 10);
        Comment c = buildComment(COMMENT_ID, OWNER_ID);
        given(commentRepository.findByReferenceType(ReferenceType.QNA, pageable))
                .willReturn(new PageImpl<>(List.of(c), pageable, 1));

        Page<AdminCommentResponse> result = adminCommentService.list(ReferenceType.QNA, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("list — type + refId 둘 다 지정 시 좁힌 조회")
    void list_by_type_and_ref() {
        Pageable pageable = PageRequest.of(0, 10);
        Comment c = buildComment(COMMENT_ID, OWNER_ID);
        given(commentRepository.findByReferenceIdAndReferenceType(POST_ID, ReferenceType.QNA, pageable))
                .willReturn(new PageImpl<>(List.of(c), pageable, 1));

        Page<AdminCommentResponse> result = adminCommentService.list(ReferenceType.QNA, POST_ID, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}

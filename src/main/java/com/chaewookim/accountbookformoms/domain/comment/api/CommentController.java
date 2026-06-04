package com.chaewookim.accountbookformoms.domain.comment.api;

import com.chaewookim.accountbookformoms.domain.comment.application.CommentService;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentCreateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.request.CommentUpdateRequest;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.CommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Comment API")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 추가", description = "게시물에 댓글을 추가합니다.")
    @PostMapping("/{post-id}")
    public ResponseEntity<ApiResponse<Long>> create(
            @PathVariable("post-id") Long postId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(commentService.create(postId, request, userId)));
    }

    @Operation(summary = "대댓글 추가", description = "댓글에 대댓글을 추가합니다.")
    @PostMapping("/{post-id}/{comment-id}")
    public ResponseEntity<ApiResponse<Long>> reply(
            @PathVariable("post-id") Long postId,
            @PathVariable("comment-id") Long parentCommentId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(commentService.reply(postId, parentCommentId, request, userId)));
    }

    @Operation(summary = "댓글 수정", description = "본인의 댓글을 수정합니다.")
    @PatchMapping("/{comment-id}")
    public ResponseEntity<ApiResponse<Long>> update(
            @PathVariable("comment-id") Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(commentService.update(commentId, request, userId)));
    }

    @Operation(summary = "댓글 삭제", description = "본인의 댓글을 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{comment-id}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable("comment-id") Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(commentService.delete(commentId, userId)));
    }

    @Operation(summary = "댓글 조회", description = "게시물에 달린 댓글 목록을 조회합니다.")
    @GetMapping("/{post-id}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> list(
            @PathVariable("post-id") Long postId,
            @RequestParam ReferenceType referenceType
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.list(postId, referenceType)));
    }
}

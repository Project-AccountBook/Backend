package com.chaewookim.accountbookformoms.domain.comment.api;

import com.chaewookim.accountbookformoms.domain.comment.application.AdminCommentService;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Comment API")
@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @Operation(summary = "관리자 댓글 삭제", description = "관리자 권한으로 댓글을 삭제 표시합니다.")
    @DeleteMapping("/{comment-id}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable("comment-id") Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminCommentService.deleteByAdmin(commentId)));
    }
}

package com.chaewookim.accountbookformoms.domain.comment.api;

import com.chaewookim.accountbookformoms.domain.comment.application.AdminCommentService;
import com.chaewookim.accountbookformoms.domain.comment.dto.response.AdminCommentResponse;
import com.chaewookim.accountbookformoms.domain.comment.enums.ReferenceType;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Comment API")
@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @Operation(summary = "관리자 댓글 목록",
            description = "댓글을 admin_deleted / user-deleted 포함 원문 그대로 반환합니다. " +
                    "referenceType/referenceId 로 필터 가능.")
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCommentResponse>>> list(
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) Long referenceId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminCommentService.list(referenceType, referenceId, pageable)));
    }

    @Operation(summary = "관리자 댓글 삭제", description = "관리자 권한으로 댓글을 삭제 표시합니다.")
    @DeleteMapping("/{comment-id}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable("comment-id") Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminCommentService.deleteByAdmin(commentId)));
    }

    @Operation(summary = "관리자 댓글 일괄 삭제", description = "관리자 권한으로 여러 댓글을 일괄 삭제 표시합니다.")
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<java.util.List<Long>>> deleteBulk(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<Long> commentIds
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminCommentService.deleteBulkByAdmin(commentIds)));
    }
}

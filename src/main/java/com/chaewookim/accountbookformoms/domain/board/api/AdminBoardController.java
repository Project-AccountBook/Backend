package com.chaewookim.accountbookformoms.domain.board.api;

import com.chaewookim.accountbookformoms.domain.board.application.AdminBoardService;
import com.chaewookim.accountbookformoms.domain.board.application.BoardReindexService;
import com.chaewookim.accountbookformoms.domain.board.dto.response.AdminBoardResponse;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Board API")
@RestController
@RequestMapping("/api/v1/admin/boards")
@RequiredArgsConstructor
public class AdminBoardController {

    private final AdminBoardService adminBoardService;
    private final BoardReindexService boardReindexService;

    @Operation(summary = "관리자 게시물 목록",
            description = "게시물을 admin_deleted 포함 원문 그대로 반환합니다. " +
                    "includeDeleted=true 이면 유저가 소프트 삭제한 게시물도 포함.")
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminBoardResponse>>> list(
            @RequestParam(required = false) BOARD_TYPE type,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @Parameter(hidden = true)
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminBoardService.list(type, includeDeleted, pageable)));
    }

    @Operation(summary = "관리자 게시물 삭제", description = "관리자 권한으로 게시물을 삭제 표시합니다.")
    @DeleteMapping("/{post-id}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable("post-id") Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminBoardService.deleteByAdmin(postId)));
    }

    @Operation(summary = "관리자 게시물 일괄 삭제", description = "관리자 권한으로 여러 게시물을 일괄 삭제 표시합니다.")
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<java.util.List<Long>>> deleteBulk(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<Long> postIds
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminBoardService.deleteBulkByAdmin(postIds)));
    }

    @Operation(summary = "게시물 ES 전체 재색인",
            description = "모든 게시물을 태그와 함께 Elasticsearch 에 다시 색인합니다. " +
                    "BoardDocument 스키마 변경(예: tags 추가) 이후 최초 1회 실행하세요.")
    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Long>> reindexAll() {
        return ResponseEntity.ok(ApiResponse.success(boardReindexService.reindexAll()));
    }
}

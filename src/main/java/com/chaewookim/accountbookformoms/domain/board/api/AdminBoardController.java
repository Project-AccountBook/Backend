package com.chaewookim.accountbookformoms.domain.board.api;

import com.chaewookim.accountbookformoms.domain.board.application.AdminBoardService;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Board API")
@RestController
@RequestMapping("/api/v1/admin/boards")
@RequiredArgsConstructor
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    @Operation(summary = "관리자 게시물 삭제", description = "관리자 권한으로 게시물을 삭제 표시합니다.")
    @DeleteMapping("/{post-id}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable("post-id") Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminBoardService.deleteByAdmin(postId)));
    }
}

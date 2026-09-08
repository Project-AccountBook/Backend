package com.chaewookim.accountbookformoms.domain.user.api;

import com.chaewookim.accountbookformoms.domain.user.application.InterestCategoryService;
import com.chaewookim.accountbookformoms.domain.user.dto.response.InterestCategoryResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관심 카테고리(Interest Category)", description = "사용자 관심 카테고리 관리 API")
@RestController
@RequestMapping("/api/v1/interest-categories")
@RequiredArgsConstructor
public class InterestCategoryController {

    private final InterestCategoryService interestCategoryService;

    @Operation(summary = "관심 카테고리 등록", description = "특정 공동구매 카테고리를 관심 카테고리로 등록")
    @PostMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<InterestCategoryResponse>> register(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interestCategoryService.registerCategory(principal.getUserId(), categoryId)));
    }

    @Operation(summary = "관심 카테고리 목록 조회", description = "사용자가 등록한 모든 관심 카테고리 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InterestCategoryResponse>>> getMyCategories(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interestCategoryService.getUserCategories(principal.getUserId())));
    }

    @Operation(summary = "알림 설정 변경", description = "특정 관심 카테고리의 알림 ON/OFF 변경")
    @PatchMapping("/{id}/alarm")
    public ResponseEntity<ApiResponse<Void>> updateAlarm(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam boolean isAlarmEnabled
    ) {
        interestCategoryService.updateAlarmStatus(id, principal.getUserId(), isAlarmEnabled);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "관심 카테고리 삭제", description = "관심 카테고리 목록에서 제거")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        interestCategoryService.deleteCategory(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

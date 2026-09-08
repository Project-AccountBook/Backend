package com.chaewookim.accountbookformoms.domain.budget.api;

import com.chaewookim.accountbookformoms.domain.budget.application.BudgetService;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCopyResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "예산(Budget)", description = "사용자 예산 관리 API")
@RestController
@RequestMapping("/api/v1/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "예산 등록", description = "특정 월과 카테고리에 대한 예산 및 예상 지출을 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createBudget(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody BudgetRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.createBudget(user.getUserId(), request)));
    }

    @Operation(summary = "월별 예산 현황 조회", description = "특정 월의 카테고리별 예산, 실제 지출, 잔액 및 진척도 조회")
    @GetMapping("/{yearMonth}/status")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getMonthlyBudgetStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getMonthlyBudgetStatus(user.getUserId(), yearMonth)));
    }

    @Operation(summary = "월별 예산 요약 조회", description = "특정 월의 전체 예산 합계 및 지출 요약 정보 조회")
    @GetMapping("/{yearMonth}/summary")
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> getMonthlyBudgetSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getMonthlyBudgetSummary(user.getUserId(), yearMonth)));
    }

    @Operation(summary = "예산 수정", description = "등록된 예산의 총 예산 및 예상 지출 금액 수정")
    @PatchMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> updateBudget(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequest request
    ) {
        budgetService.updateBudget(user.getUserId(), budgetId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "예산 삭제", description = "특정 예산 항목 삭제")
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long budgetId
    ) {
        budgetService.deleteBudget(user.getUserId(), budgetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "최근 예산 불러오기 미리보기", description = "가장 최근에 등록한 예산 월을 기준으로 복사 미리보기")
    @GetMapping("/{targetYearMonth}/copy-from-latest/preview")
    public ResponseEntity<ApiResponse<BudgetCopyResponse>> previewCopyFromLatest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetYearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                budgetService.previewCopyFromLatest(user.getUserId(), targetYearMonth)
        ));
    }

    @Operation(summary = "최근 예산 불러오기", description = "가장 최근에 등록한 예산 월 설정을 복사")
    @PostMapping("/{targetYearMonth}/copy-from-latest")
    public ResponseEntity<ApiResponse<BudgetCopyResponse>> copyFromLatest(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String targetYearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                budgetService.copyFromLatest(user.getUserId(), targetYearMonth)
        ));
    }
}

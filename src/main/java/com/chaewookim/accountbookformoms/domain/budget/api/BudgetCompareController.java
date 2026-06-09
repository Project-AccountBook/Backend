package com.chaewookim.accountbookformoms.domain.budget.api;

import com.chaewookim.accountbookformoms.domain.budget.application.BudgetCompareService;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetCompareRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.PublicBudgetFilterRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.MyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PairBudgetDetailResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.PublicMonthlyBudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.UserBudgetCompareResponse;
import com.chaewookim.accountbookformoms.domain.budget.enums.BudgetCompareType;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "예산 비교(Budget Compare)", description = "내 예산 조회 및 공개 사용자 예산 비교 API")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetCompareController {

    private final BudgetCompareService budgetCompareService;

    @Operation(summary = "내 월별 예산 조회", description = "월 총 예산과 카테고리별 예산 목록")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyBudgetResponse>> getMyMonthlyBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetCompareService.getMyMonthlyBudget(principal.getUserId(), yearMonth)));
    }

    @Operation(summary = "공개 사용자 월별 총 예산 목록", description = "포트폴리오 공개 사용자들의 월별 총 예산. 연/월/금액 구간 필터 지원")
    @GetMapping("/compare/users")
    public ResponseEntity<ApiResponse<List<PublicMonthlyBudgetResponse>>> getPublicMonthlyBudgets(
            @ParameterObject @Valid @ModelAttribute PublicBudgetFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetCompareService.getPublicMonthlyBudgets(filter)));
    }

    @Operation(summary = "그룹 평균과 내 예산 비교",
            description = "type: AGE(본인 나이대) / AMOUNT(금액 구간) / CATEGORY(특정 카테고리)")
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<BudgetCompareResponse>> compare(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @Valid @ModelAttribute BudgetCompareRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(budgetCompareService.compareWithGroup(principal.getUserId(), request)));
    }

    @Operation(summary = "선택 사용자와 본인 예산 세부 조회",
            description = "본인과 선택한 공개 사용자의 월 총 예산 및 카테고리별 예산을 함께 반환")
    @GetMapping("/compare/users/{targetUserId}/details")
    public ResponseEntity<ApiResponse<PairBudgetDetailResponse>> getPairBudgetDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                budgetCompareService.getPairBudgetDetails(principal.getUserId(), targetUserId, yearMonth)));
    }

    @Operation(summary = "선택 사용자와 본인 예산 비교",
            description = "type: AGE(나이대 라벨) / AMOUNT(월 총 예산) / CATEGORY(특정 카테고리). CATEGORY는 categoryId 필수")
    @GetMapping("/compare/users/{targetUserId}")
    public ResponseEntity<ApiResponse<UserBudgetCompareResponse>> compareWithUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam BudgetCompareType type,
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                budgetCompareService.compareWithUser(principal.getUserId(), targetUserId, type, yearMonth, categoryId)));
    }
}

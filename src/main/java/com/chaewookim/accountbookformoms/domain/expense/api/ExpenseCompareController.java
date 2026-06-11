package com.chaewookim.accountbookformoms.domain.expense.api;

import com.chaewookim.accountbookformoms.domain.expense.application.ExpenseCompareService;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.ExpenseCompareRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.request.PublicExpenseFilterRequest;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.ExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.MyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PairExpenseDetailResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.PublicMonthlyExpenseResponse;
import com.chaewookim.accountbookformoms.domain.expense.dto.response.UserExpenseCompareResponse;
import com.chaewookim.accountbookformoms.domain.expense.enums.ExpenseCompareType;
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

@Tag(name = "지출 비교(Expense Compare)", description = "내 지출 조회 및 공개 사용자 지출 비교 API (고정/변동 구분)")
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseCompareController {

    private final ExpenseCompareService expenseCompareService;

    @Operation(summary = "내 월별 지출 조회", description = "월 총 지출과 고정/변동별 카테고리 합계")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyExpenseResponse>> getMyMonthlyExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseCompareService.getMyMonthlyExpense(principal.getUserId(), yearMonth)));
    }

    @Operation(summary = "공개 사용자 월별 총 지출 목록",
            description = "포트폴리오 공개 사용자들의 월별 총 지출. 연/월/금액 구간 필터 지원")
    @GetMapping("/compare/users")
    public ResponseEntity<ApiResponse<List<PublicMonthlyExpenseResponse>>> getPublicMonthlyExpenses(
            @ParameterObject @Valid @ModelAttribute PublicExpenseFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResponse.success(expenseCompareService.getPublicMonthlyExpenses(filter)));
    }

    @Operation(summary = "그룹 평균과 내 지출 비교",
            description = "type: AGE(본인 나이대) / AMOUNT(금액 구간) / CATEGORY(특정 카테고리). 고정/변동 분리 반환")
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<ExpenseCompareResponse>> compare(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @Valid @ModelAttribute ExpenseCompareRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseCompareService.compareWithGroup(principal.getUserId(), request)));
    }

    @Operation(summary = "선택 사용자와 본인 지출 세부 조회",
            description = "본인과 선택한 공개 사용자의 월 지출(고정/변동/카테고리별)을 함께 반환")
    @GetMapping("/compare/users/{targetUserId}/details")
    public ResponseEntity<ApiResponse<PairExpenseDetailResponse>> getPairExpenseDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseCompareService.getPairExpenseDetails(principal.getUserId(), targetUserId, yearMonth)));
    }

    @Operation(summary = "선택 사용자와 본인 지출 비교",
            description = "type: AGE / AMOUNT / CATEGORY. CATEGORY는 categoryId 필수. 고정/변동 분리 반환")
    @GetMapping("/compare/users/{targetUserId}")
    public ResponseEntity<ApiResponse<UserExpenseCompareResponse>> compareWithUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam ExpenseCompareType type,
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseCompareService.compareWithUser(principal.getUserId(), targetUserId, type, yearMonth, categoryId)));
    }
}

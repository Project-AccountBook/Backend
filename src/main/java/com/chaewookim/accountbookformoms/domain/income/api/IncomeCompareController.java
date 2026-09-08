package com.chaewookim.accountbookformoms.domain.income.api;

import com.chaewookim.accountbookformoms.domain.income.application.IncomeCompareService;
import com.chaewookim.accountbookformoms.domain.income.dto.request.IncomeCompareRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.request.PublicIncomeFilterRequest;
import com.chaewookim.accountbookformoms.domain.income.dto.response.IncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.MyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PairIncomeDetailResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.PublicMonthlyIncomeResponse;
import com.chaewookim.accountbookformoms.domain.income.dto.response.UserIncomeCompareResponse;
import com.chaewookim.accountbookformoms.domain.income.enums.IncomeCompareType;
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

@Tag(name = "수입 비교(Income Compare)", description = "내 수입 조회 및 공개 사용자 수입 비교 API (고정/변동 구분)")
@RestController
@RequestMapping("/api/v1/incomes")
@RequiredArgsConstructor
public class IncomeCompareController {

    private final IncomeCompareService incomeCompareService;

    @Operation(summary = "내 월별 수입 조회", description = "월 총 수입과 고정/변동별 카테고리 합계")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyIncomeResponse>> getMyMonthlyIncome(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                incomeCompareService.getMyMonthlyIncome(principal.getUserId(), yearMonth)));
    }

    @Operation(summary = "공개 사용자 월별 총 수입 목록",
            description = "포트폴리오 공개 사용자들의 월별 총 수입. 연/월/금액 구간 필터 지원")
    @GetMapping("/compare/users")
    public ResponseEntity<ApiResponse<List<PublicMonthlyIncomeResponse>>> getPublicMonthlyIncomes(
            @ParameterObject @Valid @ModelAttribute PublicIncomeFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResponse.success(incomeCompareService.getPublicMonthlyIncomes(filter)));
    }

    @Operation(summary = "그룹 평균과 내 수입 비교",
            description = "type: AGE(본인 나이대) / AMOUNT(금액 구간) / CATEGORY(특정 카테고리). 고정/변동 분리 반환")
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<IncomeCompareResponse>> compare(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @Valid @ModelAttribute IncomeCompareRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                incomeCompareService.compareWithGroup(principal.getUserId(), request)));
    }

    @Operation(summary = "선택 사용자와 본인 수입 세부 조회",
            description = "본인과 선택한 공개 사용자의 월 수입(고정/변동/카테고리별)을 함께 반환")
    @GetMapping("/compare/users/{targetUserId}/details")
    public ResponseEntity<ApiResponse<PairIncomeDetailResponse>> getPairIncomeDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                incomeCompareService.getPairIncomeDetails(principal.getUserId(), targetUserId, yearMonth)));
    }

    @Operation(summary = "선택 사용자와 본인 수입 비교",
            description = "type: AGE / AMOUNT / CATEGORY. CATEGORY는 categoryId 필수. 고정/변동 분리 반환")
    @GetMapping("/compare/users/{targetUserId}")
    public ResponseEntity<ApiResponse<UserIncomeCompareResponse>> compareWithUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam IncomeCompareType type,
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                incomeCompareService.compareWithUser(principal.getUserId(), targetUserId, type, yearMonth, categoryId)));
    }
}

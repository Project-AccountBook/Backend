package com.chaewookim.accountbookformoms.domain.portfolio.api;

import com.chaewookim.accountbookformoms.domain.portfolio.application.PortfolioService;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.request.PortfolioCompareRequest;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.request.PortfolioFilterRequest;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.MyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PairPortfolioDetailResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PortfolioCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.PublicMonthlyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.UserPortfolioCompareResponse;
import com.chaewookim.accountbookformoms.domain.portfolio.enums.PortfolioCompareType;
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

@Tag(name = "포트폴리오 비교(Portfolio Compare)",
        description = "내 포트폴리오(수입/지출/예산) 조회 및 공개 사용자와의 비교 API")
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "내 월별 포트폴리오 조회",
            description = "월 총 수입/지출/예산, 잔액, 카테고리별 수입/지출/예산을 함께 반환")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPortfolioResponse>> getMyPortfolio(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                portfolioService.getMyPortfolio(principal.getUserId(), yearMonth)));
    }

    @Operation(summary = "공개 사용자 월별 포트폴리오 목록",
            description = "포트폴리오 공개 사용자들의 월 총 수입/지출/예산/잔액. 연/월/수입/지출/예산 금액 구간 필터 지원")
    @GetMapping("/compare/users")
    public ResponseEntity<ApiResponse<List<PublicMonthlyPortfolioResponse>>> getPublicPortfolios(
            @ParameterObject @Valid @ModelAttribute PortfolioFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResponse.success(portfolioService.getPublicPortfolios(filter)));
    }

    @Operation(summary = "그룹 평균과 내 포트폴리오 비교",
            description = "탭(type): AGE(본인 나이대) / AMOUNT(금액 구간) / CATEGORY(카테고리). " +
                    "선택된 탭의 평균 수입/지출/예산과 본인 값을 비교해 반환")
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<PortfolioCompareResponse>> compare(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @Valid @ModelAttribute PortfolioCompareRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                portfolioService.compareWithGroup(principal.getUserId(), request)));
    }

    @Operation(summary = "선택 사용자와 본인 포트폴리오 세부 조회",
            description = "본인과 선택한 공개 사용자의 월 총/카테고리별 수입·지출·예산 및 잔액을 반환")
    @GetMapping("/compare/users/{targetUserId}/details")
    public ResponseEntity<ApiResponse<PairPortfolioDetailResponse>> getPairPortfolioDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                portfolioService.getPairPortfolioDetails(principal.getUserId(), targetUserId, yearMonth)));
    }

    @Operation(summary = "선택 사용자와 본인 포트폴리오 비교",
            description = "탭(type): AGE / AMOUNT / CATEGORY. CATEGORY는 categoryId 필수. " +
                    "선택된 탭에 대해 본인과 선택 사용자의 수입/지출/예산을 비교해 반환")
    @GetMapping("/compare/users/{targetUserId}")
    public ResponseEntity<ApiResponse<UserPortfolioCompareResponse>> compareWithUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId,
            @RequestParam PortfolioCompareType type,
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                portfolioService.compareWithUser(principal.getUserId(), targetUserId, type, yearMonth, categoryId)));
    }
}

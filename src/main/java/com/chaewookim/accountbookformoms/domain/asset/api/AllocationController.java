package com.chaewookim.accountbookformoms.domain.asset.api;

import com.chaewookim.accountbookformoms.domain.asset.application.MonthlyAllocationService;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationSummaryResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "자산 집계(Allocation)", description = "저축률·투자율 등 월별 자산 집계 API")
@RestController
@RequestMapping("/api/v1/allocation")
@RequiredArgsConstructor
public class AllocationController {

    private final MonthlyAllocationService monthlyAllocationService;

    @Operation(summary = "월별 저축·투자 집계", description = "계좌 역할·이체 내역·카테고리 플래그 기준 월별 저축률·투자율 조회")
    @GetMapping("/{yearMonth}")
    public ResponseEntity<ApiResponse<MonthlyAllocationSummaryResponse>> getMonthlyAllocation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String yearMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlyAllocationService.getMonthlyAllocationSummary(userPrincipal.getUserId(), yearMonth)
        ));
    }
}

package com.chaewookim.accountbookformoms.domain.dashboard.api;

import com.chaewookim.accountbookformoms.domain.dashboard.application.DashboardService;
import com.chaewookim.accountbookformoms.domain.dashboard.dto.response.DashboardResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "대시보드(Dashboard)", description = "대시보드 및 통계 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 조회", description = "카테고리별 통계, 소비 추이, 예산 현황 등 종합 데이터 제공")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String yearMonth
    ) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboard(userPrincipal.getUserId(), yearMonth)
        ));
    }
}

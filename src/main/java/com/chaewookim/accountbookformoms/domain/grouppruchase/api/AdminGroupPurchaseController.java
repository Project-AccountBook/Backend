package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.GroupPurchaseService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "어드민 공동구매 관리", description = "어드민 공동구매 대시보드 및 요약 API")
@RestController
@RequestMapping("/api/v1/admin/group-purchases")
@RequiredArgsConstructor
public class AdminGroupPurchaseController {

    private final GroupPurchaseService groupPurchaseService;

    @Operation(summary = "실시간 현황 요약", description = "당일 개설된 공구 수, 실시간 참여 수, 진행/성공/무산 비율을 요약하여 반환합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<GroupPurchaseDashboardResponse>> getSummary() {
        GroupPurchaseDashboardResponse summary = groupPurchaseService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}

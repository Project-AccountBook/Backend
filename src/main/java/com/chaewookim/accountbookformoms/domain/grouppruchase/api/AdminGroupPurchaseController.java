package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.GroupPurchaseService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseAdminResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;

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

    @Operation(summary = "공동구매 전체 목록 모니터링", description = "모든 공동구매 글을 리스트 형태로 조회하고, 상태별(모집중/성공/무산/신고됨)로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<GroupPurchaseAdminResponse>>> getGroupPurchases(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<GroupPurchaseAdminResponse> response = groupPurchaseService.getGroupPurchasesForAdmin(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 상태 조정 (관리자용)", description = "관리자 권한으로 공동구매 상태를 강제 변경합니다.")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Long>> updateStatus(
            @PathVariable Long id,
            @RequestParam PurchaseStatus status
    ) {
        groupPurchaseService.updateGroupPurchaseStatusByAdmin(id, status);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    @Operation(summary = "공동구매 글 삭제 (관리자용)", description = "관리자 권한으로 공동구매 글을 삭제(Soft Delete)합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteByAdmin(
            @PathVariable Long id
    ) {
        groupPurchaseService.deleteGroupPurchaseByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("공동구매가 성공적으로 삭제되었습니다."));
    }
}

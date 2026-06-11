package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.ReportService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ReportProcessRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.ReportResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "어드민 공동구매 신고 관리", description = "관리자용 공동구매 글/댓글 신고 조회 및 처리 API")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 목록 조회", description = "사용자들의 신고 내역 목록을 조회합니다. 처리 여부와 대상 유형으로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @RequestParam(required = false) Boolean isProcessed,
            @RequestParam(required = false) ReportTargetType targetType,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponse> reports = reportService.getReports(isProcessed, targetType, pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "신고 처리", description = "신고 건에 대해 반려, 경고, 삭제 등의 처리를 내립니다.")
    @PostMapping("/{reportId}/process")
    public ResponseEntity<ApiResponse<Long>> processReport(
            @PathVariable Long reportId,
            @RequestBody @Valid ReportProcessRequest request
    ) {
        Long processedId = reportService.processReport(reportId, request);
        return ResponseEntity.ok(ApiResponse.success(processedId));
    }
}

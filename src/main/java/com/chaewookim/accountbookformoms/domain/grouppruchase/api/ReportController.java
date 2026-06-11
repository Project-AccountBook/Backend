package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.ReportService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ReportCreateRequest;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "어드민 공동구매 신고관리", description = "공동구매 글 및 댓글 신고 API")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 접수", description = "공동구매 글 또는 댓글을 신고합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ReportCreateRequest request
    ) {
        Long reportId = reportService.createReport(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(reportId));
    }
}

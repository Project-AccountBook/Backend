package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.GroupPurchaseApplicationService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ApplicationStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseApplicationCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseApplicationResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 공동구매 신청", description = "공동구매 신청 생성/조회/승인 API")
@RestController
@RequestMapping("/api/v1/group-purchase-applications")
@RequiredArgsConstructor
public class GroupPurchaseApplicationController {

    private final GroupPurchaseApplicationService applicationService;

    @Operation(summary = "공동구매 신청서 등록", description = "새로운 공동구매 글 개설을 관리자에게 신청합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<GroupPurchaseApplicationResponse>> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid GroupPurchaseApplicationCreateRequest request
    ) {
        GroupPurchaseApplicationResponse response = applicationService.createApplication(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "본인의 공동구매 신청 목록 조회", description = "로그인한 사용자가 신청한 신청서 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<GroupPurchaseApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<GroupPurchaseApplicationResponse> response = applicationService.getUserApplications(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 신청서 상세 조회", description = "신청서 상세 내용을 조회합니다. 본인 혹은 관리자만 가능합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupPurchaseApplicationResponse>> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GroupPurchaseApplicationResponse response = applicationService.getApplication(id, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "어드민 공동구매 신청 상태 수정", description = "공동구매 신청을 승인하거나 반려하고 피드백을 남깁니다.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<GroupPurchaseApplicationResponse>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String feedback
    ) {
        // 관리자 권한 직접 체크
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 신청서 상태를 변경할 수 있습니다.");
        }
        GroupPurchaseApplicationResponse response = applicationService.updateApplicationStatus(id, status, feedback);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.chaewookim.accountbookformoms.domain.like.api;

import com.chaewookim.accountbookformoms.domain.like.application.LikeCountReconcileService;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Like API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/likes")
public class AdminLikeController {

    private final LikeCountReconcileService reconcileService;

    @Operation(summary = "좋아요 카운터 정합성 수동 실행",
            description = "Redis like:count:* 를 SCAN 하며 DB COUNT 와 비교, drift 를 정정합니다. " +
                    "limit=0 이면 무제한.")
    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<LikeCountReconcileService.ReconcileReport>> reconcile(
            @RequestParam(defaultValue = "500") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(reconcileService.reconcile(limit)));
    }
}

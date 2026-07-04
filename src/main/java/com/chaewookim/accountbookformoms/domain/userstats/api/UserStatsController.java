package com.chaewookim.accountbookformoms.domain.userstats.api;

import com.chaewookim.accountbookformoms.domain.userstats.application.UserStatsService;
import com.chaewookim.accountbookformoms.domain.userstats.dto.response.UserStatsResponse;
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

@Tag(name = "User Stats API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}")
public class UserStatsController {

    private final UserStatsService userStatsService;

    @Operation(summary = "사용자 프로필 통계")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> stats(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long viewerId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(userStatsService.getStats(userId, viewerId)));
    }
}

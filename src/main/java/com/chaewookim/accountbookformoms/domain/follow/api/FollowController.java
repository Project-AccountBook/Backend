package com.chaewookim.accountbookformoms.domain.follow.api;

import com.chaewookim.accountbookformoms.domain.follow.application.FollowService;
import com.chaewookim.accountbookformoms.domain.follow.dto.response.FollowToggleResponse;
import com.chaewookim.accountbookformoms.domain.follow.dto.response.FollowUserResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Follow API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우 토글")
    @PostMapping("/follow")
    public ResponseEntity<ApiResponse<FollowToggleResponse>> toggle(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                followService.toggle(userId, principal.getUserId())));
    }

    @Operation(summary = "팔로워 목록")
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<List<FollowUserResponse>>> followers(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.listFollowers(userId)));
    }

    @Operation(summary = "팔로잉 목록")
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<FollowUserResponse>>> following(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.listFollowing(userId)));
    }
}

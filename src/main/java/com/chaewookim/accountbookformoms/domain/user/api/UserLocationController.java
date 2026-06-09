package com.chaewookim.accountbookformoms.domain.user.api;

import com.chaewookim.accountbookformoms.domain.user.application.UserLocationService;
import com.chaewookim.accountbookformoms.domain.user.dto.request.LocationUpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.LocationResponse;
import com.chaewookim.accountbookformoms.domain.user.dto.response.NearbyUserResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "회원 위치(UserLocation)", description = "Redis GEO 기반 위치 등록/조회 API")
@RestController
@RequestMapping("/api/v1/users/me/location")
@RequiredArgsConstructor
public class UserLocationController {

    private final UserLocationService userLocationService;

    @Operation(summary = "내 위치 등록/수정", description = "현재 사용자의 위경도를 DB와 Redis GEO에 저장")
    @PutMapping
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid LocationUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(userLocationService.updateLocation(principal.getUserId(), request)));
    }

    @Operation(summary = "내 위치 조회", description = "Redis GEO에 저장된 현재 사용자의 위경도 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<LocationResponse>> getMyLocation(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userLocationService.getMyLocation(principal.getUserId())));
    }

    @Operation(summary = "근처 사용자 조회", description = "Redis GEORADIUSBYMEMBER로 일정 반경 내 사용자 목록 조회 (단위: km)")
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyUserResponse>>> findNearbyUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "3.0") double radiusKm
    ) {
        return ResponseEntity.ok(ApiResponse.success(userLocationService.findNearbyUsers(principal.getUserId(), radiusKm)));
    }
}

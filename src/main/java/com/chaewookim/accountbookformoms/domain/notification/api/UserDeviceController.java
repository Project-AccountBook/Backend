package com.chaewookim.accountbookformoms.domain.notification.api;

import com.chaewookim.accountbookformoms.domain.notification.application.UserDeviceService;
import com.chaewookim.accountbookformoms.domain.notification.dto.request.UserDeviceRequest;
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

@Tag(name = "알림(Notification)", description = "기기 관리 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @Operation(summary = "FCM 토큰 등록 및 수정", description = "로그인한 사용자의 FCM 기기 토큰 저장")
    @PostMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid UserDeviceRequest request
    ) {
        userDeviceService.registerToken(principal.getUser(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

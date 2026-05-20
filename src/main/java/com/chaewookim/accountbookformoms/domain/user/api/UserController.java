package com.chaewookim.accountbookformoms.domain.user.api;

import com.chaewookim.accountbookformoms.domain.user.application.UserService;
import com.chaewookim.accountbookformoms.domain.user.domain.CustomUserDetails;
import com.chaewookim.accountbookformoms.domain.user.dto.request.SignUpRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.UpdateRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.request.WithdrawRequest;
import com.chaewookim.accountbookformoms.domain.user.dto.response.UserResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원(User)", description = "회원가입/정보수정/탈퇴 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 회원 등록")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@RequestBody @Valid SignUpRequest request) {

        return ResponseEntity.ok(ApiResponse.success(userService.signUp(request)));
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(ApiResponse.success(UserResponse
                .from(userService.getUserByUsername(userDetails.getUsername()))));
    }


    @Operation(summary = "정보 수정", description = "회원의 정보 수정")
    @PatchMapping("/update")
    public ResponseEntity<ApiResponse<Long>> update(
            @RequestBody @Valid UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userDetails.getUsername(), request)));
    }

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 회원 탈퇴(Soft Delete) 처리")
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> withdraw(
            @RequestBody @Valid WithdrawRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        userService.withdrawUser(userDetails, request);

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }
}

package com.chaewookim.accountbookformoms.domain.asset.api;

import com.chaewookim.accountbookformoms.domain.asset.application.AccountService;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountGoalRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountUpdateRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "계좌(Account)", description = "자산 계좌 관리 API")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성", description = "사용자의 새로운 자산 계좌 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody AccountRequest dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountService.createAccount(user.getUserId(), dto)));
    }

    @Operation(summary = "계좌 목록 조회", description = "사용자가 보유한 모든 계좌 목록 조회")
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(accountService.getAccounts(user.getUserId()));
    }

    @Operation(summary = "계좌 상세 조회", description = "특정 계좌의 상세 정보 조회")
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccount(user.getUserId(), accountId)));
    }

    @Operation(summary = "계좌 수정", description = "특정 계좌의 정보 수정")
    @PatchMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> updateAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountUpdateRequest dto
    ) {
        accountService.updateAccount(user.getUserId(), accountId, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "계좌 목표 수정", description = "특정 계좌의 목표 금액·목표일 설정")
    @PatchMapping("/{accountId}/goal")
    public ResponseEntity<ApiResponse<Void>> updateAccountGoal(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountGoalRequest dto
    ) {
        accountService.updateAccountGoal(user.getUserId(), accountId, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "계좌 목표 삭제", description = "특정 계좌의 목표 금액·목표일 삭제 (역할은 유지)")
    @DeleteMapping("/{accountId}/goal")
    public ResponseEntity<ApiResponse<Void>> clearAccountGoal(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId
    ) {
        accountService.clearAccountGoal(user.getUserId(), accountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "계좌 삭제", description = "특정 계좌 삭제")
    @DeleteMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId
    ) {
        accountService.deleteAccount(user.getUserId(), accountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

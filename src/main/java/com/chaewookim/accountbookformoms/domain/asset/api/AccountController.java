package com.chaewookim.accountbookformoms.domain.asset.api;

import com.chaewookim.accountbookformoms.domain.asset.application.AccountService;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
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

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(accountService.getAccounts(user.getUserId()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody AccountRequest dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountService.createAccount(user.getUserId(), dto)));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> updateAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountRequest dto
    ) {
        accountService.updateAccount(user.getUserId(), accountId, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long accountId
    ) {
        accountService.deleteAccount(user.getUserId(), accountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

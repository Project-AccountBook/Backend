package com.chaewookim.accountbookformoms.domain.asset.api;

import com.chaewookim.accountbookformoms.domain.asset.application.FixedTransactionService;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.FixedTransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.FixedTransactionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "고정 수입/지출(FixedTransaction)", description = "고정 수입/지출 내역 관리 API")
@RestController
@RequestMapping("/api/v1/fixed-transactions")
@RequiredArgsConstructor
public class FixedTransactionController {

    private final FixedTransactionService fixedTransactionService;

    @Operation(summary = "고정 수입/지출 생성", description = "새로운 고정 수입/지출 내역 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createFixedTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody FixedTransactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(fixedTransactionService.createFixedTransaction(userPrincipal.getUserId(), userPrincipal.getUser(), request)));
    }

    @Operation(summary = "고정 수입/지출 목록 조회", description = "로그인한 사용자의 모든 고정 수입/지출 내역 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FixedTransactionResponse>>> getFixedTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(fixedTransactionService.getFixedTransactions(userPrincipal.getUserId())));
    }

    @Operation(summary = "고정 수입/지출 수정", description = "기존에 등록된 고정 내역의 상세 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateFixedTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody FixedTransactionRequest request
    ) {
        fixedTransactionService.updateFixedTransaction(userPrincipal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "고정 수입/지출 활성/비활성 토글", description = "등록된 고정 내역의 활성화 상태 변경")
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleActiveStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        fixedTransactionService.toggleActiveStatus(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "고정 수입/지출 삭제", description = "등록된 고정 수입/지출 내역 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFixedTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        fixedTransactionService.deleteFixedTransaction(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "실행 실패 고정 거래 재실행", description = "잔액/한도 부족으로 실패한 회차를 즉시 재실행")
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<Void>> retryFailedExecution(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        fixedTransactionService.retryFailedExecution(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "실행 실패 고정 거래 건너뛰기", description = "잔액/한도 부족으로 실패한 회차를 건너뛰고 다음 회차로 진행")
    @PostMapping("/{id}/skip")
    public ResponseEntity<ApiResponse<Void>> skipFailedExecution(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        fixedTransactionService.skipFailedExecution(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

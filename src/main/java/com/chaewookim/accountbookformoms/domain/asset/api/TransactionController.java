package com.chaewookim.accountbookformoms.domain.asset.api;

import com.chaewookim.accountbookformoms.domain.asset.application.TransactionExportService;
import com.chaewookim.accountbookformoms.domain.asset.application.TransactionService;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.TransactionResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "거래 내역(Transaction)", description = "수입/지출/이체 내역 관리 API")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionExportService transactionExportService;

    @Operation(summary = "거래 내역 목록 조회", description = "특정 계좌의 기간별 거래 내역 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransactionResponse> responses = transactionService.getTransactions(userPrincipal.getUserId(), accountId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "전체 거래 내역 조회", description = "삭제된 계좌 거래를 포함한 사용자 전체 거래 내역 조회")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllUserTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 100, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransactionResponse> responses = transactionService.getAllUserTransactions(
                userPrincipal.getUserId(), startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "거래 내역 상세 조회", description = "특정 거래 내역 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransaction(id)));
    }

    @Operation(summary = "거래 내역 등록", description = "수입/지출/이체 내역 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody TransactionRequest request
    ) {
        Long transactionId = transactionService.createTransaction(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(transactionId));
    }

    @Operation(summary = "거래 내역 수정", description = "기존 거래 내역 및 잔액 정보 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request
    ) {
        transactionService.updateTransaction(userPrincipal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "거래 내역 삭제", description = "거래 내역 삭제 및 잔액 원복")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        transactionService.deleteTransaction(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "거래 내역 내보내기", description = "특정 기간 거래 내역 CSV 다운로드")
    @GetMapping("/export")
    public ResponseEntity<Resource> exportTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Resource resource = transactionExportService.exportToCsv(userPrincipal.getUserId(), startDate, endDate);
        String fileName = String.format("account_book_%s_to_%s.xlsx", startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
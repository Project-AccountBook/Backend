package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.GroupPurchaseCategoryService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCategoryCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCategoryUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseCategoryResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공동구매 카테고리 관리", description = "공동구매 카테고리 생성/수정/삭제/조회 API")
@RestController
@RequestMapping("/api/v1/group-purchase-categories")
@RequiredArgsConstructor
public class GroupPurchaseCategoryController {

    private final GroupPurchaseCategoryService categoryService;

    @Operation(summary = "어드민 카테고리 등록", description = "새로운 공동구매 카테고리를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<GroupPurchaseCategoryResponse>> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid GroupPurchaseCategoryCreateRequest request
    ) {
        validateAdmin(userPrincipal);
        GroupPurchaseCategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "카테고리 단건 조회", description = "카테고리 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupPurchaseCategoryResponse>> getOne(
            @PathVariable Long id
    ) {
        GroupPurchaseCategoryResponse response = categoryService.getCategory(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "카테고리 전체 목록 조회", description = "정렬 순서대로 정렬된 카테고리 목록 전체를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupPurchaseCategoryResponse>>> getAll() {
        List<GroupPurchaseCategoryResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "어드민 카테고리 수정", description = "카테고리 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupPurchaseCategoryResponse>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid GroupPurchaseCategoryUpdateRequest request
    ) {
        validateAdmin(userPrincipal);
        GroupPurchaseCategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "어드민 카테고리 삭제", description = "카테고리를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateAdmin(userPrincipal);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("카테고리가 성공적으로 삭제되었습니다."));
    }

    private void validateAdmin(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 카테고리를 관리할 수 있습니다.");
        }
    }
}

package com.chaewookim.accountbookformoms.domain.asset.api;

import com.chaewookim.accountbookformoms.domain.asset.application.CategoryService;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.CategoryRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.CategoryResponse;
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

@Tag(name = "카테고리(Category)", description = "커스텀 카테고리 관리 API")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회", description = "기본 카테고리와 커스텀 카테고리 함께 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategories(userPrincipal.getUserId())));
    }

    @Operation(summary = "커스텀 카테고리 추가", description = "사용자 정의 카테고리 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse response = categoryService.createCustomCategory(userPrincipal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 카테고리 수정", description = "카테고리 이름 및 타입 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        categoryService.updateCategory(id, userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "커스텀 카테고리 삭제", description = "사용자 정의 카테고리 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(id, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

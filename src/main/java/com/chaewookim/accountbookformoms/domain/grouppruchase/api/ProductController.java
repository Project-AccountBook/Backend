package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.ProductService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ProductCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.ProductUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.ProductResponse;
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

@Tag(name = "공동구매 상품(Product)", description = "공동구매 상품 생성/수정/삭제/조회 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 등록 (관리자용)", description = "새로운 공동구매 상품 정보를 등록합니다. 관리자만 가능합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ProductCreateRequest request
    ) {
        validateAdmin(userPrincipal);
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 단건 조회", description = "ID에 해당하는 상품 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getOne(
            @PathVariable Long id
    ) {
        ProductResponse response = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 목록 조회", description = "상품 전체 목록을 조회합니다. 카테고리 ID로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll(
            @RequestParam(required = false) Long categoryId
    ) {
        List<ProductResponse> response = productService.getAllProducts(categoryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 수정 (관리자용)", description = "ID에 해당하는 상품 정보를 수정합니다. 관리자만 가능합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ProductUpdateRequest request
    ) {
        validateAdmin(userPrincipal);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상품 삭제 (관리자용)", description = "ID에 해당하는 상품을 삭제(Soft Delete)합니다. 관리자만 가능합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateAdmin(userPrincipal);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("상품이 성공적으로 삭제되었습니다."));
    }

    private void validateAdmin(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 상품을 관리할 수 있습니다.");
        }
    }
}

package com.chaewookim.accountbookformoms.domain.image.api;

import com.chaewookim.accountbookformoms.domain.image.application.ImageService;
import com.chaewookim.accountbookformoms.domain.image.dto.request.AttachImageRequest;
import com.chaewookim.accountbookformoms.domain.image.dto.response.ImageResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Image API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "게시물 이미지 첨부",
            description = "이미 업로드된 URL(S3 또는 외부)을 게시물에 연결합니다. multipart 업로드는 별도 인프라 도입 후 추가.")
    @PostMapping("/boards/{postId}/images")
    public ResponseEntity<ApiResponse<ImageResponse>> attach(
            @PathVariable Long postId,
            @RequestBody @Valid AttachImageRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                imageService.attachToBoard(postId, request, principal.getUserId())));
    }

    @Operation(summary = "게시물 이미지 목록")
    @GetMapping("/boards/{postId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> list(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(imageService.listBoardImages(postId)));
    }

    @Operation(summary = "이미지 삭제")
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        imageService.deleteImage(imageId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(imageId));
    }
}

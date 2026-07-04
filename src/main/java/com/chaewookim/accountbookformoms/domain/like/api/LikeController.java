package com.chaewookim.accountbookformoms.domain.like.api;

import com.chaewookim.accountbookformoms.domain.like.application.PostLikeService;
import com.chaewookim.accountbookformoms.domain.like.dto.response.LikeToggleResponse;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Like API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LikeController {

    private final PostLikeService likeService;

    @Operation(summary = "게시물 좋아요 토글")
    @PostMapping("/boards/{postId}/like")
    public ResponseEntity<ApiResponse<LikeToggleResponse>> toggleBoardLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                likeService.toggle(LikeTargetType.BOARD, postId, principal.getUserId())));
    }

    @Operation(summary = "댓글 좋아요 토글")
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<LikeToggleResponse>> toggleCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                likeService.toggle(LikeTargetType.COMMENT, commentId, principal.getUserId())));
    }
}

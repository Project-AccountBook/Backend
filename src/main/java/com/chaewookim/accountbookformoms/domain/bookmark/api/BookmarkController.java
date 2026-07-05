package com.chaewookim.accountbookformoms.domain.bookmark.api;

import com.chaewookim.accountbookformoms.domain.bookmark.application.BookmarkService;
import com.chaewookim.accountbookformoms.domain.bookmark.dto.response.BookmarkToggleResponse;
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

@Tag(name = "Bookmark API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "게시물 북마크 토글")
    @PostMapping("/boards/{postId}/bookmark")
    public ResponseEntity<ApiResponse<BookmarkToggleResponse>> toggle(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                bookmarkService.toggle(postId, principal.getUserId())));
    }
}

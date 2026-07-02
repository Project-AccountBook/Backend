package com.chaewookim.accountbookformoms.domain.board.api;

import com.chaewookim.accountbookformoms.domain.board.application.BoardService;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardCreateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardUpdateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardCreateResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardSearchResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardUpdateResponse;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Board API")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "게시물 목록 조회", description = "QNA/KNOWHOW 게시판 게시물 목록 조회. type 파라미터 생략 시 전체 반환.")
    @PageableAsQueryParam
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BoardResponse>>> list(
            @RequestParam(required = false) BOARD_TYPE type,
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(boardService.list(type, pageable)));
    }

    @Operation(summary = "게시물 추가", description = "일반 사용자의 게시물 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<BoardCreateResponse>> create(
            @RequestBody @Valid BoardCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(boardService.create(request, userId)));
    }

    @Operation(summary = "게시물 조회", description = "게시물 단건 조회")
    @GetMapping("/{post-id}")
    public ResponseEntity<ApiResponse<BoardResponse>> get(
            @PathVariable("post-id") Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(boardService.get(postId)));
    }

    @Operation(summary = "게시물 수정", description = "일반 사용자의 게시물 수정")
    @PatchMapping("/{post-id}")
    public ResponseEntity<ApiResponse<BoardUpdateResponse>> update(
            @PathVariable("post-id") Long postId,
            @RequestBody @Valid BoardUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(boardService.update(postId, request, userId)));
    }

    @Operation(summary = "게시물 삭제", description = "일반 사용자의 게시물 삭제 (Soft Delete)")
    @DeleteMapping("/{post-id}")
    public ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable("post-id") Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(ApiResponse.success(boardService.delete(postId, userId)));
    }

    @Operation(summary = "게시물 검색", description = "Elasticsearch nori 기반 게시물 검색")
    @PageableAsQueryParam
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<BoardSearchResponse>>> search(
            @RequestParam String keyword,
            @Parameter(hidden = true)
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(boardService.search(keyword, pageable)));
    }
}

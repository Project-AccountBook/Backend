package com.chaewookim.accountbookformoms.domain.boardcategory.api;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.boardcategory.application.BoardCategoryService;
import com.chaewookim.accountbookformoms.domain.boardcategory.dto.response.BoardCategoryResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Board Category API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board-categories")
public class BoardCategoryController {

    private final BoardCategoryService service;

    @Operation(summary = "게시판 카테고리 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardCategoryResponse>>> list(
            @RequestParam(required = false) BOARD_TYPE type
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.list(type)));
    }
}

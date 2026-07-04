package com.chaewookim.accountbookformoms.domain.tag.api;

import com.chaewookim.accountbookformoms.domain.tag.application.TagService;
import com.chaewookim.accountbookformoms.domain.tag.dto.response.TagResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tag API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "전체 태그 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(tagService.listAll()));
    }
}

package com.chaewookim.accountbookformoms.domain.grouppruchase.api;

import com.chaewookim.accountbookformoms.domain.grouppruchase.application.GroupPurchaseService;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseJoinResponse;
import com.chaewookim.accountbookformoms.global.common.ApiResponse;
import com.chaewookim.accountbookformoms.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공동구매(GroupPurchase)", description = "공동구매 생성/수정/삭제/조회 API")
@RestController
@RequestMapping("/api/v1/group-purchases")
@RequiredArgsConstructor
public class GroupPurchaseController {

    private final GroupPurchaseService groupPurchaseService;

    @Operation(summary = "공동구매 개설", description = "새로운 공동구매 글을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<GroupPurchaseResponse>> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid GroupPurchaseCreateRequest request
    ) {
        GroupPurchaseResponse response = groupPurchaseService.createGroupPurchase(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 단건 조회", description = "ID에 해당하는 공동구매 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupPurchaseResponse>> getOne(
            @PathVariable Long id
    ) {
        GroupPurchaseResponse response = groupPurchaseService.getGroupPurchase(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 목록 조회", description = "진행 중인 공동구매 목록을 조회합니다. 정렬, 지역, 카테고리 필터링 및 동네 필터링(nearMe)이 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupPurchaseResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean nearMe,
            @RequestParam(required = false, defaultValue = "latest") String sortBy
    ) {
        Long userId = (userPrincipal != null) ? userPrincipal.getUserId() : null;
        List<GroupPurchaseResponse> response = groupPurchaseService.getAllGroupPurchases(region, categoryId, nearMe, userId, sortBy);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 수정", description = "ID에 해당하는 공동구매 상세 정보를 수정합니다. 개설자만 수정 가능합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupPurchaseResponse>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid GroupPurchaseUpdateRequest request
    ) {
        GroupPurchaseResponse response = groupPurchaseService.updateGroupPurchase(id, userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 삭제", description = "ID에 해당하는 공동구매 정보를 삭제(Soft Delete)합니다. 개설자만 삭제 가능합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        groupPurchaseService.deleteGroupPurchase(id, userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("공동구매가 성공적으로 삭제되었습니다."));
    }

    @Operation(summary = "공동구매 찜하기 토글", description = "공동구매 글을 찜하거나 찜 해제합니다.")
    @PostMapping("/{id}/wish")
    public ResponseEntity<ApiResponse<Boolean>> toggleWish(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        boolean result = groupPurchaseService.toggleWish(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "찜한 공동구매 목록 조회", description = "내가 찜한 공동구매 목록을 페이징하여 조회합니다.")
    @GetMapping("/wishes")
    public ResponseEntity<ApiResponse<Page<GroupPurchaseResponse>>> getWishes(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<GroupPurchaseResponse> response = groupPurchaseService.getWishedGroupPurchases(userPrincipal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 참여", description = "공동구매 글에 참여 신청합니다.")
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<GroupPurchaseJoinResponse>> join(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GroupPurchaseJoinResponse response = groupPurchaseService.joinGroupPurchase(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공동구매 참여 취소", description = "공동구매 참여 신청을 취소합니다.")
    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<GroupPurchaseResponse>> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        GroupPurchaseResponse response = groupPurchaseService.leaveGroupPurchase(userPrincipal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

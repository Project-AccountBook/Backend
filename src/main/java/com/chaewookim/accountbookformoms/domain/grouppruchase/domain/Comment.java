package com.chaewookim.accountbookformoms.domain.grouppruchase.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    private Long id;
    private Long groupPurchaseId;
    private Long userId;
    private Long parentId;        // 상위 댓글 ID (대댓글용 셀프 참조 필드)
    private String content;       // 댓글 내용
    private boolean isDeleted;    // 삭제 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Comment(Long id, Long groupPurchaseId, Long userId, Long parentId, String content) {
        this.id = id;
        this.groupPurchaseId = groupPurchaseId;
        this.userId = userId;
        this.parentId = parentId;
        this.content = content;
        this.isDeleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
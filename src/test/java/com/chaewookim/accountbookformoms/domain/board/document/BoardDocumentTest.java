package com.chaewookim.accountbookformoms.domain.board.document;

import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BoardDocumentTest {

    @Test
    @DisplayName("from — Board 엔티티의 필드를 그대로 매핑")
    void from_maps_all_fields() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        Board board = Board.builder()
                .userId(1L)
                .categoryId(10L)
                .title("절약 노하우")
                .content("본문")
                .type(BOARD_TYPE.KNOWHOW)
                .build();
        ReflectionTestUtils.setField(board, "id", 100L);
        ReflectionTestUtils.setField(board, "createdAt", now);

        BoardDocument doc = BoardDocument.from(board);

        assertThat(doc.getId()).isEqualTo(100L);
        assertThat(doc.getUserId()).isEqualTo(1L);
        assertThat(doc.getCategoryId()).isEqualTo(10L);
        assertThat(doc.getTitle()).isEqualTo("절약 노하우");
        assertThat(doc.getContent()).isEqualTo("본문");
        assertThat(doc.getType()).isEqualTo(BOARD_TYPE.KNOWHOW.name());
        assertThat(doc.getCreatedAt()).isEqualTo(now);
    }
}

package com.chaewookim.accountbookformoms.domain.boardcategory.application;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.boardcategory.dao.BoardCategoryRepository;
import com.chaewookim.accountbookformoms.domain.boardcategory.dto.response.BoardCategoryResponse;
import com.chaewookim.accountbookformoms.domain.boardcategory.entity.BoardCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardCategoryServiceTest {

    @Mock private BoardCategoryRepository repository;

    @InjectMocks
    private BoardCategoryService boardCategoryService;

    @Test
    @DisplayName("list(null) — 전체 조회 (findAll)")
    void list_all() {
        BoardCategory c1 = BoardCategory.builder().name("재테크").boardType(BOARD_TYPE.QNA).displayOrder(0).build();
        given(repository.findAll()).willReturn(List.of(c1));

        List<BoardCategoryResponse> result = boardCategoryService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("재테크");
    }

    @Test
    @DisplayName("list(QNA) — boardType 필터로 orderByDisplayOrder 조회")
    void list_by_type() {
        BoardCategory c1 = BoardCategory.builder().name("재테크").boardType(BOARD_TYPE.QNA).displayOrder(0).build();
        BoardCategory c2 = BoardCategory.builder().name("가계부").boardType(BOARD_TYPE.QNA).displayOrder(1).build();
        given(repository.findByBoardTypeOrderByDisplayOrderAsc(BOARD_TYPE.QNA)).willReturn(List.of(c1, c2));

        List<BoardCategoryResponse> result = boardCategoryService.list(BOARD_TYPE.QNA);

        assertThat(result).extracting(BoardCategoryResponse::name).containsExactly("재테크", "가계부");
        verify(repository).findByBoardTypeOrderByDisplayOrderAsc(BOARD_TYPE.QNA);
    }
}

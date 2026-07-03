package com.chaewookim.accountbookformoms.domain.tag.application;

import com.chaewookim.accountbookformoms.domain.tag.dao.BoardTagRepository;
import com.chaewookim.accountbookformoms.domain.tag.dao.TagRepository;
import com.chaewookim.accountbookformoms.domain.tag.entity.BoardTag;
import com.chaewookim.accountbookformoms.domain.tag.entity.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private BoardTagRepository boardTagRepository;

    @InjectMocks
    private TagService tagService;

    private static final Long BOARD_ID = 100L;

    private Tag tagWithId(Long id, String name) {
        Tag t = Tag.builder().name(name).build();
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    @Test
    @DisplayName("setTagsForBoard — 기존 태그 재사용 + 신규 태그 생성 후 BoardTag 저장")
    void set_tags_reuses_existing_and_creates_new() {
        Tag existing = tagWithId(1L, "재테크");
        given(tagRepository.findByNameIn(List.of("재테크", "신규"))).willReturn(List.of(existing));
        Tag newSaved = tagWithId(2L, "신규");
        given(tagRepository.save(any(Tag.class))).willReturn(newSaved);

        tagService.setTagsForBoard(BOARD_ID, List.of("재테크", "신규"));

        verify(boardTagRepository).deleteByBoardId(BOARD_ID);
        verify(tagRepository, times(1)).save(any(Tag.class));  // "신규" 만 저장
        verify(boardTagRepository, times(2)).save(any(BoardTag.class));
    }

    @Test
    @DisplayName("setTagsForBoard — null/빈 태그는 필터, 중복 제거")
    void set_tags_filters_blanks_and_dedupes() {
        Tag existing = tagWithId(1L, "절약");
        given(tagRepository.findByNameIn(List.of("절약"))).willReturn(List.of(existing));

        tagService.setTagsForBoard(BOARD_ID, java.util.Arrays.asList("절약", "  ", null, "절약"));

        verify(boardTagRepository).deleteByBoardId(BOARD_ID);
        verify(boardTagRepository, times(1)).save(any(BoardTag.class));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("setTagsForBoard(null/empty) — 삭제만 하고 종료")
    void set_tags_null_only_deletes() {
        tagService.setTagsForBoard(BOARD_ID, null);
        verify(boardTagRepository).deleteByBoardId(BOARD_ID);
        verify(boardTagRepository, never()).save(any());
    }

    @Test
    @DisplayName("tagsOfBoard — link 순서를 유지하며 태그 이름 반환")
    void tags_of_board_preserves_order() {
        BoardTag bt1 = BoardTag.builder().boardId(BOARD_ID).tagId(1L).build();
        BoardTag bt2 = BoardTag.builder().boardId(BOARD_ID).tagId(2L).build();
        given(boardTagRepository.findByBoardId(BOARD_ID)).willReturn(List.of(bt1, bt2));
        given(tagRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(tagWithId(1L, "재테크"), tagWithId(2L, "절약")));

        List<String> result = tagService.tagsOfBoard(BOARD_ID);

        assertThat(result).containsExactly("재테크", "절약");
    }

    @Test
    @DisplayName("tagsByBoards — board_id → 태그 이름 리스트 매핑")
    void tags_by_boards_map() {
        BoardTag bt1 = BoardTag.builder().boardId(10L).tagId(1L).build();
        BoardTag bt2 = BoardTag.builder().boardId(10L).tagId(2L).build();
        BoardTag bt3 = BoardTag.builder().boardId(20L).tagId(1L).build();
        given(boardTagRepository.findByBoardIdIn(List.of(10L, 20L))).willReturn(List.of(bt1, bt2, bt3));
        given(tagRepository.findAllById(any()))
                .willReturn(List.of(tagWithId(1L, "재테크"), tagWithId(2L, "절약")));

        Map<Long, List<String>> result = tagService.tagsByBoards(List.of(10L, 20L));

        assertThat(result.get(10L)).containsExactlyInAnyOrder("재테크", "절약");
        assertThat(result.get(20L)).containsExactly("재테크");
    }

    @Test
    @DisplayName("boardIdsWithTag — 태그 이름 없으면 빈 리스트")
    void board_ids_with_unknown_tag() {
        given(tagRepository.findByName("없는태그")).willReturn(Optional.empty());

        assertThat(tagService.boardIdsWithTag("없는태그")).isEmpty();
    }

    @Test
    @DisplayName("boardIdsWithTag — 태그 존재 시 boardTagRepository 위임")
    void board_ids_with_existing_tag() {
        Tag t = tagWithId(1L, "재테크");
        given(tagRepository.findByName("재테크")).willReturn(Optional.of(t));
        given(boardTagRepository.findBoardIdsByTagId(1L)).willReturn(List.of(10L, 20L));

        List<Long> result = tagService.boardIdsWithTag("재테크");

        assertThat(result).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("listAll — 저장된 태그 전체 반환")
    void list_all() {
        given(tagRepository.findAll()).willReturn(List.of(tagWithId(1L, "재테크")));

        assertThat(tagService.listAll()).hasSize(1);
    }

    @Test
    @DisplayName("setTagsForBoard 저장 인자 검증 — 각 BoardTag 가 올바른 boardId/tagId 조합")
    void set_tags_saves_correct_pairs() {
        Tag existing = tagWithId(1L, "재테크");
        given(tagRepository.findByNameIn(List.of("재테크"))).willReturn(List.of(existing));

        tagService.setTagsForBoard(BOARD_ID, List.of("재테크"));

        ArgumentCaptor<BoardTag> captor = ArgumentCaptor.forClass(BoardTag.class);
        verify(boardTagRepository).save(captor.capture());
        assertThat(captor.getValue().getBoardId()).isEqualTo(BOARD_ID);
        assertThat(captor.getValue().getTagId()).isEqualTo(1L);
    }
}

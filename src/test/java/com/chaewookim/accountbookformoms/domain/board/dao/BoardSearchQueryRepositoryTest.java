package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardSearchQueryRepositoryTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchHits<BoardDocument> searchHits;

    @Mock
    private SearchHit<BoardDocument> hit;

    @InjectMocks
    private BoardSearchQueryRepository repository;

    @Test
    @DisplayName("성공 — ES NativeQuery 호출 결과를 Page로 변환")
    void search_returns_page_from_hits() {
        String keyword = "절약";
        Pageable pageable = PageRequest.of(0, 10);
        BoardDocument doc = BoardDocument.builder()
                .id(100L)
                .title("절약 노하우")
                .content("이번 달 절약")
                .type("QNA")
                .userId(1L)
                .categoryId(10L)
                .build();

        given(elasticsearchOperations.search(any(NativeQuery.class), eq(BoardDocument.class)))
                .willReturn(searchHits);
        given(searchHits.stream()).willReturn(Stream.of(hit));
        given(searchHits.getTotalHits()).willReturn(1L);
        given(hit.getContent()).willReturn(doc);

        Page<BoardDocument> result = repository.search(keyword, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isSameAs(doc);
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(BoardDocument.class));
    }

    @Test
    @DisplayName("성공 — 일치 결과 없으면 빈 Page 반환")
    void search_returns_empty_page_when_no_hits() {
        String keyword = "없는키워드";
        Pageable pageable = PageRequest.of(0, 10);

        given(elasticsearchOperations.search(any(NativeQuery.class), eq(BoardDocument.class)))
                .willReturn(searchHits);
        given(searchHits.stream()).willReturn(Stream.empty());
        given(searchHits.getTotalHits()).willReturn(0L);

        Page<BoardDocument> result = repository.search(keyword, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}

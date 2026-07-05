package com.chaewookim.accountbookformoms.domain.board.dao;

import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BoardSearchQueryRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<BoardDocument> search(String keyword, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .should(s -> s.multiMatch(mm -> mm
                                .query(keyword)
                                .fields("title^2", "content")))
                        .should(s -> s.term(t -> t
                                .field("tags")
                                .value(keyword)))
                        .minimumShouldMatch("1")))
                .withPageable(pageable)
                .build();

        SearchHits<BoardDocument> hits = elasticsearchOperations.search(query, BoardDocument.class);
        List<BoardDocument> content = hits.stream().map(SearchHit::getContent).toList();
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}

package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchRepository;
import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.tag.application.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 게시물 → Elasticsearch 초기(또는 태그 스키마 변경 후) 재색인 배치.
 * <p>페이지 단위로 Board 를 로딩해 태그를 함께 벌크 색인. 관리자 API 및 부팅 옵션에서 호출.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardReindexService {

    private static final int PAGE_SIZE = 200;

    private final BoardRepository boardRepository;
    private final BoardSearchRepository boardSearchRepository;
    private final TagService tagService;

    @Transactional(readOnly = true)
    public long reindexAll() {
        long start = System.currentTimeMillis();
        long indexed = 0;
        int pageNumber = 0;
        while (true) {
            Page<Board> page = boardRepository.findAll(
                    PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            List<Board> boards = page.getContent();
            if (boards.isEmpty()) break;

            List<Long> ids = boards.stream().map(Board::getId).toList();
            Map<Long, List<String>> tagsByBoard = tagService.tagsByBoards(ids);

            List<BoardDocument> docs = new ArrayList<>(boards.size());
            for (Board b : boards) {
                docs.add(BoardDocument.from(b, tagsByBoard.getOrDefault(b.getId(), List.of())));
            }
            boardSearchRepository.saveAll(docs);
            indexed += docs.size();

            if (!page.hasNext()) break;
            pageNumber++;
        }

        log.info("Board ES 초기 재색인 완료: {} 문서 / {}ms",
                indexed, System.currentTimeMillis() - start);
        return indexed;
    }
}

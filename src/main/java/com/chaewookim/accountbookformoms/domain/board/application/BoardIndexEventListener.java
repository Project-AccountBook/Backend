package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchRepository;
import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardIndexEventListener {

    private final BoardRepository boardRepository;
    private final BoardSearchRepository boardSearchRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BoardChangedEvent event) {
        try {
            switch (event.type()) {
                case UPSERT -> boardRepository.findById(event.boardId())
                        .ifPresent(board -> boardSearchRepository.save(BoardDocument.from(board)));
                case DELETE -> boardSearchRepository.deleteById(event.boardId());
            }
        } catch (Exception e) {
            log.error("Board ES indexing failed: boardId={}, type={}", event.boardId(), event.type(), e);
        }
    }
}

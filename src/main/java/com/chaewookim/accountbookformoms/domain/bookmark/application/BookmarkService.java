package com.chaewookim.accountbookformoms.domain.bookmark.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.bookmark.dao.BookmarkRepository;
import com.chaewookim.accountbookformoms.domain.bookmark.dto.response.BookmarkToggleResponse;
import com.chaewookim.accountbookformoms.domain.bookmark.entity.Bookmark;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public BookmarkToggleResponse toggle(Long boardId, Long userId) {
        if (!boardRepository.existsById(boardId)) {
            throw new CustomException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndBoardId(userId, boardId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return new BookmarkToggleResponse(false);
        }
        bookmarkRepository.save(Bookmark.builder().userId(userId).boardId(boardId).build());
        return new BookmarkToggleResponse(true);
    }

    public boolean isBookmarked(Long boardId, Long userId) {
        if (userId == null) return false;
        return bookmarkRepository.findByUserIdAndBoardId(userId, boardId).isPresent();
    }

    public Set<Long> bookmarkedBoards(Collection<Long> boardIds, Long userId) {
        if (userId == null || boardIds.isEmpty()) return Collections.emptySet();
        return bookmarkRepository.findByUserIdAndBoardIdIn(userId, boardIds).stream()
                .map(Bookmark::getBoardId)
                .collect(Collectors.toSet());
    }

    public List<Long> listBoardIdsByUser(Long userId) {
        return bookmarkRepository.findByUserId(userId).stream()
                .map(Bookmark::getBoardId)
                .toList();
    }
}

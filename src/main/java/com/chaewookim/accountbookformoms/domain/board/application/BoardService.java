package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchQueryRepository;
import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardCreateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardUpdateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardCreateResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardSearchResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardUpdateResponse;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.bookmark.application.BookmarkService;
import com.chaewookim.accountbookformoms.domain.like.application.PostLikeService;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardSearchQueryRepository boardSearchQueryRepository;
    private final BoardViewCountService viewCountService;
    private final UserRepository userRepository;
    private final PostLikeService likeService;
    private final BookmarkService bookmarkService;
    private final ApplicationEventPublisher eventPublisher;

    public Page<BoardResponse> list(BOARD_TYPE type, Pageable pageable, Long viewerId) {
        Page<Board> page = (type == null)
                ? boardRepository.findAll(pageable)
                : boardRepository.findByType(type, pageable);
        List<Board> boards = page.getContent();
        List<Long> ids = boards.stream().map(Board::getId).toList();
        Map<Long, Long> pending = viewCountService.getPendingDeltas(ids);
        Map<Long, String> nicknames = loadNicknames(boards.stream().map(Board::getUserId).toList());
        Map<Long, Long> likeCounts = likeService.countByTargets(LikeTargetType.BOARD, ids);
        Set<Long> liked = likeService.likedTargets(LikeTargetType.BOARD, ids, viewerId);
        Set<Long> bookmarked = bookmarkService.bookmarkedBoards(ids, viewerId);
        return page.map(b -> BoardResponse.from(
                b,
                pending.getOrDefault(b.getId(), 0L),
                nicknames.get(b.getUserId()),
                likeCounts.getOrDefault(b.getId(), 0L),
                liked.contains(b.getId()),
                bookmarked.contains(b.getId())));
    }

    @Transactional
    public BoardCreateResponse create(BoardCreateRequest request, Long userId) {
        Board board = Board.builder()
                .userId(userId)
                .categoryId(request.categoryId())
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .build();

        Board saved = boardRepository.save(board);
        eventPublisher.publishEvent(BoardChangedEvent.upsert(saved.getId()));
        return new BoardCreateResponse(saved.getId(), saved.getTitle());
    }

    public BoardResponse get(Long postId, Long viewerId) {
        Board board = findBoardOrThrow(postId);
        long pending = viewCountService.increment(postId);
        String nickname = userRepository.findById(board.getUserId())
                .map(User::getUsername)
                .orElse(null);
        long likeCount = likeService.count(LikeTargetType.BOARD, postId);
        boolean liked = likeService.isLiked(LikeTargetType.BOARD, postId, viewerId);
        boolean bookmarked = bookmarkService.isBookmarked(postId, viewerId);
        return BoardResponse.from(board, pending, nickname, likeCount, liked, bookmarked);
    }

    @Transactional
    public BoardUpdateResponse update(Long postId, BoardUpdateRequest request, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        board.update(request.title(), request.content(), request.type());
        eventPublisher.publishEvent(BoardChangedEvent.upsert(board.getId()));
        return new BoardUpdateResponse(board.getId(), board.getTitle());
    }

    @Transactional
    public Long delete(Long postId, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        boardRepository.delete(board);
        eventPublisher.publishEvent(BoardChangedEvent.delete(board.getId()));
        return board.getId();
    }

    @Transactional
    public boolean setResolved(Long postId, boolean value, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);
        board.setResolved(value);
        return board.isResolved();
    }

    @Transactional
    public boolean setUrgent(Long postId, boolean value, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);
        board.setUrgent(value);
        return board.isUrgent();
    }

    public Page<BoardSearchResponse> search(String keyword, Pageable pageable) {
        Page<BoardDocument> hits = boardSearchQueryRepository.search(keyword, pageable);
        return hits.map(BoardSearchResponse::from);
    }

    private Map<Long, String> loadNicknames(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private Board findBoardOrThrow(Long postId) {
        return boardRepository.findById(postId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
    }

    private void validateOwner(Board board, Long userId) {
        if (!board.getUserId().equals(userId)) {
            throw new CustomException(BoardErrorCode.BOARD_ACCESS_DENIED);
        }
    }
}

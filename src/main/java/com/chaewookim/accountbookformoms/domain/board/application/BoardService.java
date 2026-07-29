package com.chaewookim.accountbookformoms.domain.board.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchQueryRepository;
import com.chaewookim.accountbookformoms.domain.board.document.BoardDocument;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardCreateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.request.BoardUpdateRequest;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardCreateResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardHotResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardSearchResponse;
import com.chaewookim.accountbookformoms.domain.board.dto.response.BoardUpdateResponse;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.bookmark.application.BookmarkService;
import com.chaewookim.accountbookformoms.domain.image.application.ImageService;
import com.chaewookim.accountbookformoms.domain.like.application.PostLikeService;
import com.chaewookim.accountbookformoms.domain.like.enums.LikeTargetType;
import com.chaewookim.accountbookformoms.domain.tag.application.TagService;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.config.RedisConfig;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.event.BoardChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final TagService tagService;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;

    public Page<BoardResponse> list(BOARD_TYPE type, String tag, Pageable pageable, Long viewerId) {
        Page<Board> page;
        if (tag != null && !tag.isBlank()) {
            List<Long> ids = tagService.boardIdsWithTag(tag);
            if (ids.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            List<Board> boards = boardRepository.findAllById(ids);
            if (type != null) {
                boards = boards.stream().filter(b -> b.getType() == type).toList();
            }
            page = new PageImpl<>(boards, pageable, boards.size());
        } else {
            page = (type == null)
                    ? boardRepository.findAll(pageable)
                    : boardRepository.findByType(type, pageable);
        }
        return enrichPage(page, viewerId);
    }

    public List<BoardResponse> listByIds(List<Long> ids, Long viewerId) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Board> boards = boardRepository.findAllById(ids);
        Map<Long, Board> byId = boards.stream()
                .collect(Collectors.toMap(Board::getId, b -> b, (a, b) -> a));
        List<Long> orderedIds = ids.stream().filter(byId::containsKey).toList();
        List<Board> ordered = orderedIds.stream().map(byId::get).toList();
        Map<Long, Long> pending = viewCountService.getPendingDeltas(orderedIds);
        Map<Long, String> nicknames = loadNicknames(ordered.stream().map(Board::getUserId).toList());
        Map<Long, Long> likeCounts = likeService.countByTargets(LikeTargetType.BOARD, orderedIds);
        Set<Long> liked = likeService.likedTargets(LikeTargetType.BOARD, orderedIds, viewerId);
        Set<Long> bookmarked = bookmarkService.bookmarkedBoards(orderedIds, viewerId);
        Map<Long, List<String>> tagsByBoard = tagService.tagsByBoards(orderedIds);
        Map<Long, List<String>> imagesByBoard = imageService.urlsByBoards(orderedIds);
        return ordered.stream()
                .map(b -> BoardResponse.from(
                        b,
                        pending.getOrDefault(b.getId(), 0L),
                        nicknames.get(b.getUserId()),
                        likeCounts.getOrDefault(b.getId(), 0L),
                        liked.contains(b.getId()),
                        bookmarked.contains(b.getId()),
                        tagsByBoard.getOrDefault(b.getId(), List.of()),
                        imagesByBoard.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    private Page<BoardResponse> enrichPage(Page<Board> page, Long viewerId) {
        List<Board> boards = page.getContent();
        List<Long> ids = boards.stream().map(Board::getId).toList();
        Map<Long, Long> pending = viewCountService.getPendingDeltas(ids);
        Map<Long, String> nicknames = loadNicknames(boards.stream().map(Board::getUserId).toList());
        Map<Long, Long> likeCounts = likeService.countByTargets(LikeTargetType.BOARD, ids);
        Set<Long> liked = likeService.likedTargets(LikeTargetType.BOARD, ids, viewerId);
        Set<Long> bookmarked = bookmarkService.bookmarkedBoards(ids, viewerId);
        Map<Long, List<String>> tagsByBoard = tagService.tagsByBoards(ids);
        Map<Long, List<String>> imagesByBoard = imageService.urlsByBoards(ids);
        return page.map(b -> BoardResponse.from(
                b,
                pending.getOrDefault(b.getId(), 0L),
                nicknames.get(b.getUserId()),
                likeCounts.getOrDefault(b.getId(), 0L),
                liked.contains(b.getId()),
                bookmarked.contains(b.getId()),
                tagsByBoard.getOrDefault(b.getId(), List.of()),
                imagesByBoard.getOrDefault(b.getId(), List.of())));
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
        tagService.setTagsForBoard(saved.getId(), request.tags());
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
        List<String> tags = tagService.tagsOfBoard(postId);
        List<String> imageUrls = imageService.urlsOfBoard(postId);
        return BoardResponse.from(board, pending, nickname, likeCount, liked, bookmarked, tags, imageUrls);
    }

    @Transactional
    public BoardUpdateResponse update(Long postId, BoardUpdateRequest request, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        board.update(request.title(), request.content(), request.type());
        if (request.tags() != null) {
            tagService.setTagsForBoard(postId, request.tags());
        }
        eventPublisher.publishEvent(BoardChangedEvent.upsert(board.getId()));
        return new BoardUpdateResponse(board.getId(), board.getTitle());
    }

    @Transactional
    public Long delete(Long postId, Long userId) {
        Board board = findBoardOrThrow(postId);
        validateOwner(board, userId);

        boardRepository.delete(board);
        likeService.evictCount(LikeTargetType.BOARD, board.getId());
        viewCountService.evict(board.getId());
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

    // TTL 5분(RedisConfig.CACHE_BOARD_HOT). 좋아요/조회수 변화는 5분 내에서 stale 허용.
    // 캐시 미스 시에만 전체 최근 게시물을 로드 + like count 조회 + in-memory 정렬 수행.
    @Cacheable(cacheNames = RedisConfig.CACHE_BOARD_HOT,
            key = "T(java.util.Objects).toString(#type) + ':' + #days + ':' + #limit")
    public List<BoardHotResponse> hot(BOARD_TYPE type, int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Board> recent = boardRepository.findRecentByType(type, since);
        List<Long> ids = recent.stream().map(Board::getId).toList();
        Map<Long, Long> likeCounts = likeService.countByTargets(LikeTargetType.BOARD, ids);
        return new ArrayList<>(recent.stream()
                .map(b -> BoardHotResponse.from(b, likeCounts.getOrDefault(b.getId(), 0L)))
                .sorted((a, b) -> Long.compare(b.score(), a.score()))
                .limit(limit)
                .toList());
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

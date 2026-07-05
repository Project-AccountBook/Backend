package com.chaewookim.accountbookformoms.domain.image.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.image.dao.ImageRepository;
import com.chaewookim.accountbookformoms.domain.image.dto.request.AttachImageRequest;
import com.chaewookim.accountbookformoms.domain.image.dto.response.ImageResponse;
import com.chaewookim.accountbookformoms.domain.image.entity.Image;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final ImageRepository imageRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public ImageResponse attachToBoard(Long boardId, AttachImageRequest request, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
        if (!board.getUserId().equals(userId)) {
            throw new CustomException(BoardErrorCode.BOARD_ACCESS_DENIED);
        }
        int nextOrder = imageRepository
                .findByReferenceTypeAndReferenceIdOrderBySortOrderAsc(Image.ReferenceType.BOARD, boardId)
                .size();
        Image saved = imageRepository.save(Image.builder()
                .imageUrl(request.imageUrl())
                .s3Key(request.s3Key())
                .referenceType(Image.ReferenceType.BOARD)
                .referenceId(boardId)
                .sortOrder(nextOrder)
                .build());
        return ImageResponse.from(saved);
    }

    public List<ImageResponse> listBoardImages(Long boardId) {
        return imageRepository
                .findByReferenceTypeAndReferenceIdOrderBySortOrderAsc(Image.ReferenceType.BOARD, boardId)
                .stream().map(ImageResponse::from).toList();
    }

    public List<String> urlsOfBoard(Long boardId) {
        return imageRepository
                .findByReferenceTypeAndReferenceIdOrderBySortOrderAsc(Image.ReferenceType.BOARD, boardId)
                .stream().map(Image::getImageUrl).toList();
    }

    public Map<Long, List<String>> urlsByBoards(Collection<Long> boardIds) {
        if (boardIds.isEmpty()) return Map.of();
        List<Image> images = imageRepository
                .findByReferenceTypeAndReferenceIdInOrderBySortOrderAsc(Image.ReferenceType.BOARD, boardIds);
        Map<Long, List<String>> result = new HashMap<>();
        images.forEach(img -> result
                .computeIfAbsent(img.getReferenceId(), k -> new ArrayList<>())
                .add(img.getImageUrl()));
        return result;
    }

    @Transactional
    public void deleteImage(Long imageId, Long userId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
        if (image.getReferenceType() == Image.ReferenceType.BOARD) {
            Board board = boardRepository.findById(image.getReferenceId())
                    .orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_NOT_FOUND));
            if (!board.getUserId().equals(userId)) {
                throw new CustomException(BoardErrorCode.BOARD_ACCESS_DENIED);
            }
        }
        imageRepository.delete(image);
    }
}

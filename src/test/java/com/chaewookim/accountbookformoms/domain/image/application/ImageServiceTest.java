package com.chaewookim.accountbookformoms.domain.image.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.board.entity.Board;
import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.board.error.BoardErrorCode;
import com.chaewookim.accountbookformoms.domain.image.dao.ImageRepository;
import com.chaewookim.accountbookformoms.domain.image.dto.request.AttachImageRequest;
import com.chaewookim.accountbookformoms.domain.image.dto.response.ImageResponse;
import com.chaewookim.accountbookformoms.domain.image.entity.Image;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock private ImageRepository imageRepository;
    @Mock private BoardRepository boardRepository;

    @InjectMocks
    private ImageService imageService;

    private static final Long BOARD_ID = 100L;
    private static final Long OWNER = 1L;
    private static final Long INTRUDER = 2L;

    private Board board(Long id, Long userId) {
        Board b = Board.builder().userId(userId).categoryId(1L).title("t").content("c").type(BOARD_TYPE.QNA).build();
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private Image image(Long id, String url, int order) {
        Image img = Image.builder()
                .imageUrl(url).s3Key(null)
                .referenceType(Image.ReferenceType.BOARD).referenceId(BOARD_ID).sortOrder(order)
                .build();
        ReflectionTestUtils.setField(img, "id", id);
        return img;
    }

    @Test
    @DisplayName("attachToBoard — 소유자 요청 시 sortOrder=기존 갯수로 저장")
    void attach_success() {
        Board board = board(BOARD_ID, OWNER);
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.of(board));
        given(imageRepository.findByReferenceTypeAndReferenceIdOrderBySortOrderAsc(Image.ReferenceType.BOARD, BOARD_ID))
                .willReturn(List.of(image(1L, "a", 0)));  // 이미 1장 → 다음 sortOrder=1
        given(imageRepository.save(any(Image.class))).willAnswer(inv -> {
            Image img = inv.getArgument(0);
            ReflectionTestUtils.setField(img, "id", 2L);
            return img;
        });

        ImageResponse response = imageService.attachToBoard(BOARD_ID, new AttachImageRequest("https://x", null), OWNER);

        assertThat(response.imageUrl()).isEqualTo("https://x");
        assertThat(response.sortOrder()).isEqualTo(1);

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceType()).isEqualTo(Image.ReferenceType.BOARD);
        assertThat(captor.getValue().getReferenceId()).isEqualTo(BOARD_ID);
    }

    @Test
    @DisplayName("attachToBoard 실패 — 소유자 아닌 사용자면 BOARD_ACCESS_DENIED")
    void attach_fail_not_owner() {
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.of(board(BOARD_ID, OWNER)));

        assertThatThrownBy(() -> imageService.attachToBoard(BOARD_ID, new AttachImageRequest("https://x", null), INTRUDER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_ACCESS_DENIED);
        verify(imageRepository, never()).save(any());
    }

    @Test
    @DisplayName("attachToBoard 실패 — 게시물 없으면 BOARD_NOT_FOUND")
    void attach_fail_board_not_found() {
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.attachToBoard(BOARD_ID, new AttachImageRequest("https://x", null), OWNER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteImage 실패 — 게시물 소유자가 아니면 BOARD_ACCESS_DENIED")
    void delete_fail_not_owner() {
        Image img = image(9L, "u", 0);
        given(imageRepository.findById(9L)).willReturn(Optional.of(img));
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.of(board(BOARD_ID, OWNER)));

        assertThatThrownBy(() -> imageService.deleteImage(9L, INTRUDER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", BoardErrorCode.BOARD_ACCESS_DENIED);
        verify(imageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteImage 성공 — 소유자 요청 시 DB delete")
    void delete_success() {
        Image img = image(9L, "u", 0);
        given(imageRepository.findById(9L)).willReturn(Optional.of(img));
        given(boardRepository.findById(BOARD_ID)).willReturn(Optional.of(board(BOARD_ID, OWNER)));

        imageService.deleteImage(9L, OWNER);

        verify(imageRepository).delete(img);
    }
}

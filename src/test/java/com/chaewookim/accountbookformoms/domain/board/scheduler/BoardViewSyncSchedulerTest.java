package com.chaewookim.accountbookformoms.domain.board.scheduler;

import com.chaewookim.accountbookformoms.domain.board.application.BoardViewCountService;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardViewSyncSchedulerTest {

    @Mock
    private BoardViewCountService viewCountService;

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardViewSyncScheduler scheduler;

    @Test
    @DisplayName("sync — dirty set 의 각 id 에 대해 readAndReset → increaseViews 호출")
    void sync_appliesDeltas() {
        given(viewCountService.snapshotDirtyBoardIds()).willReturn(Set.of(1L, 2L));
        given(viewCountService.readAndReset(1L)).willReturn(3L);
        given(viewCountService.readAndReset(2L)).willReturn(7L);
        given(boardRepository.increaseViews(anyLong(), anyLong())).willReturn(1);

        scheduler.sync();

        verify(boardRepository).increaseViews(1L, 3L);
        verify(boardRepository).increaseViews(2L, 7L);
    }

    @Test
    @DisplayName("sync — dirty set 이 비어 있으면 repository 호출 없음")
    void sync_emptyDirty() {
        given(viewCountService.snapshotDirtyBoardIds()).willReturn(Set.of());

        scheduler.sync();

        verify(viewCountService, never()).readAndReset(anyLong());
        verify(boardRepository, never()).increaseViews(anyLong(), anyLong());
    }

    @Test
    @DisplayName("sync — delta 가 0 이면 increaseViews 호출 없음 (race 후 잔여 멤버 흡수)")
    void sync_skipsZeroDelta() {
        given(viewCountService.snapshotDirtyBoardIds()).willReturn(Set.of(99L));
        given(viewCountService.readAndReset(99L)).willReturn(0L);

        scheduler.sync();

        verify(boardRepository, never()).increaseViews(anyLong(), anyLong());
    }

    @Test
    @DisplayName("sync — increaseViews 가 0 (board 없음) 이어도 다음 id 진행")
    void sync_continuesOnMissingBoard() {
        given(viewCountService.snapshotDirtyBoardIds()).willReturn(Set.of(1L, 2L));
        given(viewCountService.readAndReset(1L)).willReturn(5L);
        given(viewCountService.readAndReset(2L)).willReturn(4L);
        given(boardRepository.increaseViews(1L, 5L)).willReturn(0);
        given(boardRepository.increaseViews(2L, 4L)).willReturn(1);

        scheduler.sync();

        verify(boardRepository).increaseViews(1L, 5L);
        verify(boardRepository).increaseViews(2L, 4L);
    }
}

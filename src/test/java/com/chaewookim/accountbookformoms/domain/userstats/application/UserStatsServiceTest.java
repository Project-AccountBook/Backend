package com.chaewookim.accountbookformoms.domain.userstats.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.follow.application.FollowService;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.domain.userstats.dto.response.UserStatsResponse;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private FollowService followService;

    @InjectMocks
    private UserStatsService userStatsService;

    private static final Long TARGET = 10L;
    private static final Long VIEWER = 1L;

    @Test
    @DisplayName("getStats — 게시글/팔로워/팔로잉/팔로우 여부 집계")
    void get_stats_aggregates() {
        User target = mock(User.class);
        given(target.getId()).willReturn(TARGET);
        given(target.getUsername()).willReturn("타겟");
        given(userRepository.findById(TARGET)).willReturn(Optional.of(target));
        given(boardRepository.countByUserId(TARGET)).willReturn(12L);
        given(followService.followerCount(TARGET)).willReturn(5L);
        given(followService.followingCount(TARGET)).willReturn(3L);
        given(followService.isFollowing(TARGET, VIEWER)).willReturn(true);

        UserStatsResponse response = userStatsService.getStats(TARGET, VIEWER);

        assertThat(response.userId()).isEqualTo(TARGET);
        assertThat(response.nickname()).isEqualTo("타겟");
        assertThat(response.postCount()).isEqualTo(12L);
        assertThat(response.followerCount()).isEqualTo(5L);
        assertThat(response.followingCount()).isEqualTo(3L);
        assertThat(response.following()).isTrue();
    }

    @Test
    @DisplayName("getStats — 대상 유저 없으면 USER_NOT_FOUND")
    void get_stats_user_not_found() {
        given(userRepository.findById(TARGET)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userStatsService.getStats(TARGET, VIEWER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }
}

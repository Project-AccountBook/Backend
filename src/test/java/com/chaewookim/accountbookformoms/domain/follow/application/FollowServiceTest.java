package com.chaewookim.accountbookformoms.domain.follow.application;

import com.chaewookim.accountbookformoms.domain.follow.dao.FollowRepository;
import com.chaewookim.accountbookformoms.domain.follow.dto.response.FollowToggleResponse;
import com.chaewookim.accountbookformoms.domain.follow.dto.response.FollowUserResponse;
import com.chaewookim.accountbookformoms.domain.follow.entity.Follow;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    private static final Long FOLLOWER = 1L;
    private static final Long TARGET = 2L;

    @Test
    @DisplayName("toggle — 팔로우 없던 상태에서 저장 후 following=true")
    void toggle_add() {
        given(userRepository.existsById(TARGET)).willReturn(true);
        given(followRepository.findByFollowerIdAndFollowingId(FOLLOWER, TARGET)).willReturn(Optional.empty());
        given(followRepository.countByFollowingId(TARGET)).willReturn(1L);

        FollowToggleResponse response = followService.toggle(TARGET, FOLLOWER);

        assertThat(response.following()).isTrue();
        assertThat(response.followerCount()).isEqualTo(1L);
        verify(followRepository).save(any(Follow.class));
    }

    @Test
    @DisplayName("toggle — 이미 팔로우 상태였다면 삭제 후 following=false")
    void toggle_remove() {
        Follow existing = Follow.builder().followerId(FOLLOWER).followingId(TARGET).build();
        given(userRepository.existsById(TARGET)).willReturn(true);
        given(followRepository.findByFollowerIdAndFollowingId(FOLLOWER, TARGET)).willReturn(Optional.of(existing));
        given(followRepository.countByFollowingId(TARGET)).willReturn(0L);

        FollowToggleResponse response = followService.toggle(TARGET, FOLLOWER);

        assertThat(response.following()).isFalse();
        verify(followRepository).delete(existing);
    }

    @Test
    @DisplayName("toggle 실패 — 자기 자신 팔로우는 USER_NOT_FOUND (self-follow 차단)")
    void toggle_self_forbidden() {
        assertThatThrownBy(() -> followService.toggle(FOLLOWER, FOLLOWER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle 실패 — 대상 유저가 없으면 USER_NOT_FOUND")
    void toggle_target_missing() {
        given(userRepository.existsById(TARGET)).willReturn(false);

        assertThatThrownBy(() -> followService.toggle(TARGET, FOLLOWER))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("isFollowing — viewerId null 이면 항상 false")
    void isFollowing_null_viewer() {
        assertThat(followService.isFollowing(TARGET, null)).isFalse();
        verify(followRepository, never()).findByFollowerIdAndFollowingId(any(), any());
    }

    @Test
    @DisplayName("followerCount / followingCount — repository 위임")
    void counts_delegated() {
        given(followRepository.countByFollowingId(TARGET)).willReturn(5L);
        given(followRepository.countByFollowerId(TARGET)).willReturn(3L);

        assertThat(followService.followerCount(TARGET)).isEqualTo(5L);
        assertThat(followService.followingCount(TARGET)).isEqualTo(3L);
    }

    @Test
    @DisplayName("listFollowers — 팔로워 id 기반으로 닉네임 매칭, 없는 id는 '탈퇴한 사용자'")
    void list_followers_resolves_unknown() {
        Follow f1 = Follow.builder().followerId(10L).followingId(TARGET).build();
        Follow f2 = Follow.builder().followerId(11L).followingId(TARGET).build();
        given(followRepository.findByFollowingId(TARGET)).willReturn(List.of(f1, f2));

        User u10 = mock(User.class);
        given(u10.getId()).willReturn(10L);
        given(u10.getUsername()).willReturn("한사람");
        given(userRepository.findAllById(List.of(10L, 11L))).willReturn(List.of(u10));

        List<FollowUserResponse> result = followService.listFollowers(TARGET);

        assertThat(result).extracting(FollowUserResponse::userId).containsExactly(10L, 11L);
        assertThat(result).extracting(FollowUserResponse::nickname).containsExactly("한사람", "탈퇴한 사용자");
    }
}

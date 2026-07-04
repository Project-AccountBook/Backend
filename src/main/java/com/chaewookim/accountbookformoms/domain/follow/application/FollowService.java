package com.chaewookim.accountbookformoms.domain.follow.application;

import com.chaewookim.accountbookformoms.domain.follow.dao.FollowRepository;
import com.chaewookim.accountbookformoms.domain.follow.dto.response.FollowToggleResponse;
import com.chaewookim.accountbookformoms.domain.follow.dto.response.FollowUserResponse;
import com.chaewookim.accountbookformoms.domain.follow.entity.Follow;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public FollowToggleResponse toggle(Long targetUserId, Long followerId) {
        if (targetUserId.equals(followerId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        Optional<Follow> existing = followRepository.findByFollowerIdAndFollowingId(followerId, targetUserId);
        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            return new FollowToggleResponse(false, followRepository.countByFollowingId(targetUserId));
        }
        followRepository.save(Follow.builder()
                .followerId(followerId)
                .followingId(targetUserId)
                .build());
        return new FollowToggleResponse(true, followRepository.countByFollowingId(targetUserId));
    }

    public boolean isFollowing(Long targetUserId, Long followerId) {
        if (followerId == null) return false;
        return followRepository.findByFollowerIdAndFollowingId(followerId, targetUserId).isPresent();
    }

    public long followerCount(Long userId) {
        return followRepository.countByFollowingId(userId);
    }

    public long followingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    public List<FollowUserResponse> listFollowers(Long userId) {
        List<Long> ids = followRepository.findByFollowingId(userId).stream()
                .map(Follow::getFollowerId).toList();
        return resolveUsers(ids);
    }

    public List<FollowUserResponse> listFollowing(Long userId) {
        List<Long> ids = followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFollowingId).toList();
        return resolveUsers(ids);
    }

    private List<FollowUserResponse> resolveUsers(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, String> nicknames = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        return ids.stream()
                .map(id -> new FollowUserResponse(id, nicknames.getOrDefault(id, "탈퇴한 사용자")))
                .toList();
    }
}

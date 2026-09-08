package com.chaewookim.accountbookformoms.domain.userstats.application;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardRepository;
import com.chaewookim.accountbookformoms.domain.follow.application.FollowService;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.domain.userstats.dto.response.UserStatsResponse;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatsService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final FollowService followService;

    public UserStatsResponse getStats(Long targetUserId, Long viewerId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        long postCount = boardRepository.countByUserId(targetUserId);
        long followerCount = followService.followerCount(targetUserId);
        long followingCount = followService.followingCount(targetUserId);
        boolean following = followService.isFollowing(targetUserId, viewerId);
        return new UserStatsResponse(
                user.getId(),
                user.getUsername(),
                postCount,
                followerCount,
                followingCount,
                following
        );
    }
}

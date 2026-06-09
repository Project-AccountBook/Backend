package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseApplicationRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchaseApplication;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ApplicationStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseApplicationCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseApplicationResponse;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.enums.UserRole;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPurchaseApplicationService {

    private final GroupPurchaseApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupPurchaseApplicationResponse createApplication(Long userId, GroupPurchaseApplicationCreateRequest request) {
        GroupPurchaseApplication application = GroupPurchaseApplication.builder()
                .userId(userId)
                .title(request.title())
                .content(request.content())
                .productUrl(request.productUrl())
                .expectedPrice(request.expectedPrice())
                .build();

        GroupPurchaseApplication saved = applicationRepository.save(application);
        return GroupPurchaseApplicationResponse.from(saved);
    }

    public GroupPurchaseApplicationResponse getApplication(Long id, Long currentUserId) {
        GroupPurchaseApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 본인 또는 관리자만 조회 가능
        if (!application.getUserId().equals(currentUserId) && !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_APPLICATION);
        }

        return GroupPurchaseApplicationResponse.from(application);
    }

    public List<GroupPurchaseApplicationResponse> getUserApplications(Long userId) {
        return applicationRepository.findByUserId(userId).stream()
                .map(GroupPurchaseApplicationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupPurchaseApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status, String feedback) {
        GroupPurchaseApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        application.updateStatus(status, feedback);
        return GroupPurchaseApplicationResponse.from(application);
    }
}

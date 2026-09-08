package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.user.dao.InterestCategoryRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.response.InterestCategoryResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.InterestCategory;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
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
public class InterestCategoryService {

    private final InterestCategoryRepository repository;
    private final UserRepository userRepository;
    private final GroupPurchaseCategoryRepository categoryRepository;

    @Transactional
    public InterestCategoryResponse registerCategory(Long userId, Long categoryId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        if (repository.existsByUserAndCategory(user, category)) {
            throw new CustomException(UserErrorCode.ALREADY_EXIST_CATEGORY);
        }

        InterestCategory interest = InterestCategory.builder()
                .user(user)
                .category(category)
                .isAlarmEnabled(true)
                .build();
        return InterestCategoryResponse.from(repository.save(interest));
    }

    public List<InterestCategoryResponse> getUserCategories(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(InterestCategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateAlarmStatus(Long id, Long userId, boolean isAlarmEnabled) {
        InterestCategory category = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        category.updateAlarmStatus(isAlarmEnabled);
    }

    @Transactional
    public void deleteCategory(Long id, Long userId) {
        InterestCategory category = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        repository.delete(category);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        repository.softDeleteByUserId(userId);
    }
}

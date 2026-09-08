package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.user.dao.InterestCategoryRepository;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.dto.response.InterestCategoryResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.InterestCategory;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestCategoryServiceTest {

    @Mock
    private InterestCategoryRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupPurchaseCategoryRepository categoryRepository;

    @InjectMocks
    private InterestCategoryService interestCategoryService;

    @Test
    @DisplayName("관심 카테고리 등록 - 성공")
    void registerCategory_Success() {

        // given
        Long userId = 1L, categoryId = 10L;
        User user = mock(User.class);
        Category category = Category.builder().id(categoryId).name("밀키트").build();
        InterestCategory interest = InterestCategory.builder().user(user).category(category).isAlarmEnabled(true).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(repository.existsByUserAndCategory(user, category)).willReturn(false);
        given(repository.save(any(InterestCategory.class))).willReturn(interest);

        // when
        InterestCategoryResponse response = interestCategoryService.registerCategory(userId, categoryId);

        // then
        assertThat(response.categoryName()).isEqualTo("밀키트");
        verify(repository, times(1)).save(any(InterestCategory.class));
    }

    @Test
    @DisplayName("이미 등록된 카테고리 등록 시 예외 발생")
    void registerCategory_AlreadyExist_ThrowException() {

        // given
        Long userId = 1L, categoryId = 10L;
        User user = mock(User.class);
        Category category = mock(Category.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(repository.existsByUserAndCategory(user, category)).willReturn(true);

        // when & then
        assertThrows(CustomException.class, () -> interestCategoryService.registerCategory(userId, categoryId));
    }

    @Test
    @DisplayName("관심 카테고리 목록 조회 - 성공")
    void getUserCategories_Success() {

        // given
        Long userId = 1L;
        Category category = Category.builder().name("밀키트").build();
        InterestCategory interest = InterestCategory.builder().category(category).build();
        given(repository.findByUserId(userId)).willReturn(List.of(interest));

        // when
        List<InterestCategoryResponse> responses = interestCategoryService.getUserCategories(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).categoryName()).isEqualTo("밀키트");
    }

    @Test
    @DisplayName("알림 설정 상태 변경 - 성공")
    void updateAlarmStatus_Success() {

        // given
        Long id = 1L, userId = 1L;
        InterestCategory interest = mock(InterestCategory.class);
        given(repository.findByIdAndUserId(id, userId)).willReturn(Optional.of(interest));

        // when
        interestCategoryService.updateAlarmStatus(id, userId, false);

        // then
        verify(interest, times(1)).updateAlarmStatus(false);
    }

    @Test
    @DisplayName("관심 카테고리 삭제 - 성공")
    void deleteCategory_Success() {

        // given
        Long id = 1L, userId = 1L;
        InterestCategory interest = mock(InterestCategory.class);
        given(repository.findByIdAndUserId(id, userId)).willReturn(Optional.of(interest));

        // when
        interestCategoryService.deleteCategory(id, userId);

        // then
        verify(repository, times(1)).delete(interest);
    }

    @Test
    @DisplayName("사용자 관심 카테고리 전체 삭제 - 성공")
    void deleteAllByUserId_Success() {

        // given
        Long userId = 1L;
        given(repository.softDeleteByUserId(userId)).willReturn(3);

        // when
        interestCategoryService.deleteAllByUserId(userId);

        // then
        verify(repository).softDeleteByUserId(userId);
    }
}
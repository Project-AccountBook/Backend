package com.chaewookim.accountbookformoms.domain.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.CategoryRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.CategoryResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FixedTransactionRepository fixedTransactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 목록 조회 - 성공")
    void getCategories_Success() {

        // given
        Long userId = 1L;
        TransactionCategory category = TransactionCategory.builder().name("식비").type(TransactionType.EXPENSE).build();
        given(categoryRepository.findAllByUserOrSystem(userId)).willReturn(List.of(category));

        // when
        List<CategoryResponse> result = categoryService.getCategories(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("식비");
    }

    @Test
    @DisplayName("카테고리 생성 - 성공")
    void createCustomCategory_Success() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        CategoryRequest request = new CategoryRequest("쇼핑", TransactionType.EXPENSE, null, null);
        TransactionCategory savedCategory = TransactionCategory.builder().user(user).name("쇼핑").type(TransactionType.EXPENSE).build();
        given(categoryRepository.findByUserIdAndNameAndTypeIncludingDeleted(1L, "쇼핑", "EXPENSE"))
                .willReturn(Optional.empty());
        given(categoryRepository.existsByUserIsNullAndNameAndType("쇼핑", TransactionType.EXPENSE)).willReturn(false);
        given(categoryRepository.existsByUserIdAndNameAndType(1L, "쇼핑", TransactionType.EXPENSE)).willReturn(false);
        given(categoryRepository.save(any())).willReturn(savedCategory);

        // when
        CategoryResponse response = categoryService.createCustomCategory(user, request);

        // then
        assertThat(response.name()).isEqualTo("쇼핑");
    }

    @Test
    @DisplayName("카테고리 수정 - 성공")
    void updateCategory_Success() {

        // given
        Long categoryId = 1L;
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        TransactionCategory category = TransactionCategory.builder().user(user).name("기존").type(TransactionType.EXPENSE).build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByUserIsNullAndNameAndType("변경", TransactionType.INCOME)).willReturn(false);
        given(categoryRepository.existsByUserIdAndNameAndTypeAndIdNot(userId, "변경", TransactionType.INCOME, categoryId))
                .willReturn(false);

        CategoryRequest request = new CategoryRequest("변경", TransactionType.INCOME, null, null);

        // when
        categoryService.updateCategory(categoryId, userId, request);

        // then
        assertThat(category.getName()).isEqualTo("변경");
        assertThat(category.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    @DisplayName("카테고리 수정 - 기본 카테고리 수정 시 예외 발생")
    void updateCategory_Fail_Immutable() {

        // given
        Long categoryId = 1L;
        TransactionCategory systemCategory = TransactionCategory.builder().user(null).build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(systemCategory));

        // when & then
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, 1L, null))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.CATEGORY_IMMUTABLE);
                });
    }

    @Test
    @DisplayName("카테고리 삭제 - 고정내역 삭제 및 스냅샷 보존")
    void deleteCategory_Success() {

        // given
        Long categoryId = 1L;
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        TransactionCategory category = TransactionCategory.builder().user(user).name("식비").type(TransactionType.EXPENSE).build();

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(fixedTransactionRepository.softDeleteByTransactionCategoryId(categoryId)).willReturn(1);

        // when
        categoryService.deleteCategory(categoryId, userId);

        // then
        verify(fixedTransactionRepository, times(1)).softDeleteByTransactionCategoryId(categoryId);
        verify(transactionRepository, times(1)).backfillCategorySnapshot(categoryId, "식비");
        verify(transactionRepository, times(1)).markCategoryArchived(categoryId);
        verify(budgetRepository, times(1)).backfillCategorySnapshot(categoryId, "식비");
        verify(budgetRepository, times(1)).markCategoryArchived(categoryId);
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("카테고리 삭제 - 거래·예산이 있어도 삭제 가능")
    void deleteCategory_Success_WithExistingHistory() {

        // given
        Long categoryId = 1L;
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        TransactionCategory category = TransactionCategory.builder().user(user).name("식비").type(TransactionType.EXPENSE).build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(fixedTransactionRepository.softDeleteByTransactionCategoryId(categoryId)).willReturn(0);

        // when
        categoryService.deleteCategory(categoryId, userId);

        // then
        verify(categoryRepository, times(1)).delete(category);
        verify(transactionRepository, never()).existsByTransactionCategoryId(any());
    }

    @Test
    @DisplayName("카테고리 삭제 - 타인 카테고리 삭제 시 예외 발생")
    void deleteCategory_Fail_Forbidden() {

        // given
        Long categoryId = 1L;
        User owner = mock(User.class);
        given(owner.getId()).willReturn(2L);

        TransactionCategory category = TransactionCategory.builder().user(owner).build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // when & then
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.CATEGORY_FORBIDDEN);
                });
    }

    @Test
    @DisplayName("카테고리 생성 - 중복 이름 시 예외 발생")
    void createCustomCategory_Fail_DuplicateName() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        CategoryRequest request = new CategoryRequest("쇼핑", TransactionType.EXPENSE, null, null);
        given(categoryRepository.findByUserIdAndNameAndTypeIncludingDeleted(1L, "쇼핑", "EXPENSE"))
                .willReturn(Optional.empty());
        given(categoryRepository.existsByUserIsNullAndNameAndType("쇼핑", TransactionType.EXPENSE)).willReturn(false);
        given(categoryRepository.existsByUserIdAndNameAndType(1L, "쇼핑", TransactionType.EXPENSE)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryService.createCustomCategory(user, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.DUPLICATE_CATEGORY_NAME);
                });
    }
}

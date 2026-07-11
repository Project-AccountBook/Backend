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
    @DisplayName("카테고리 삭제 - 성공")
    void deleteCategory_Success() {

        // given
        Long categoryId = 1L;
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        TransactionCategory category = TransactionCategory.builder().user(user).build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(transactionRepository.existsByTransactionCategoryId(categoryId)).willReturn(false);
        given(fixedTransactionRepository.existsByTransactionCategoryId(categoryId)).willReturn(false);
        given(budgetRepository.existsByTransactionCategoryId(categoryId)).willReturn(false);

        // when
        categoryService.deleteCategory(categoryId, userId);

        // then
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("카테고리 삭제 - 사용 중인 카테고리 삭제 시 예외 발생")
    void deleteCategory_Fail_InUse() {

        // given
        Long categoryId = 1L;
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        TransactionCategory category = TransactionCategory.builder().user(user).build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(transactionRepository.existsByTransactionCategoryId(categoryId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.CATEGORY_IN_USE);
                });
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

    @Test
    @DisplayName("카테고리 생성 - 기본 카테고리와 이름 중복 시 예외 발생")
    void createCustomCategory_Fail_DuplicateSystemCategory() {

        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        CategoryRequest request = new CategoryRequest("식비", TransactionType.EXPENSE, null, null);
        given(categoryRepository.findByUserIdAndNameAndTypeIncludingDeleted(1L, "식비", "EXPENSE"))
                .willReturn(Optional.empty());
        given(categoryRepository.existsByUserIsNullAndNameAndType("식비", TransactionType.EXPENSE)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryService.createCustomCategory(user, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.DUPLICATE_CATEGORY_NAME);
                });
    }

    @Test
    @DisplayName("이체 카테고리 생성 - 저축률/투자율 플래그 저장")
    void createCustomCategory_TransferWithAllocationFlags() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        CategoryRequest request = new CategoryRequest("증권이체", TransactionType.TRANSFER, false, true);
        TransactionCategory savedCategory = TransactionCategory.builder()
                .user(user)
                .name("증권이체")
                .type(TransactionType.TRANSFER)
                .includeInSavingsRate(false)
                .includeInInvestmentRate(true)
                .build();
        given(categoryRepository.findByUserIdAndNameAndTypeIncludingDeleted(1L, "증권이체", "TRANSFER"))
                .willReturn(Optional.empty());
        given(categoryRepository.existsByUserIsNullAndNameAndType("증권이체", TransactionType.TRANSFER)).willReturn(false);
        given(categoryRepository.existsByUserIdAndNameAndType(1L, "증권이체", TransactionType.TRANSFER)).willReturn(false);
        given(categoryRepository.save(any())).willReturn(savedCategory);

        CategoryResponse response = categoryService.createCustomCategory(user, request);

        assertThat(response.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.includeInSavingsRate()).isFalse();
        assertThat(response.includeInInvestmentRate()).isTrue();
    }

    @Test
    @DisplayName("이체 카테고리 생성 - 플래그 미입력 시 기본값 적용")
    void createCustomCategory_TransferDefaultAllocationFlags() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        CategoryRequest request = new CategoryRequest("자동이체", TransactionType.TRANSFER, null, null);
        given(categoryRepository.findByUserIdAndNameAndTypeIncludingDeleted(1L, "자동이체", "TRANSFER"))
                .willReturn(Optional.empty());
        given(categoryRepository.existsByUserIsNullAndNameAndType("자동이체", TransactionType.TRANSFER)).willReturn(false);
        given(categoryRepository.existsByUserIdAndNameAndType(1L, "자동이체", TransactionType.TRANSFER)).willReturn(false);
        given(categoryRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.createCustomCategory(user, request);

        assertThat(response.includeInSavingsRate()).isTrue();
        assertThat(response.includeInInvestmentRate()).isFalse();
    }

    @Test
    @DisplayName("카테고리 수정 - 이체에서 지출로 변경 시 플래그 초기화")
    void updateCategory_ClearAllocationFlagsWhenNotTransfer() {
        Long categoryId = 1L;
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        TransactionCategory category = TransactionCategory.builder()
                .user(user)
                .name("적금이체")
                .type(TransactionType.TRANSFER)
                .includeInSavingsRate(true)
                .includeInInvestmentRate(false)
                .build();
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByUserIsNullAndNameAndType("적금이체", TransactionType.EXPENSE)).willReturn(false);
        given(categoryRepository.existsByUserIdAndNameAndTypeAndIdNot(userId, "적금이체", TransactionType.EXPENSE, categoryId))
                .willReturn(false);

        categoryService.updateCategory(
                categoryId,
                userId,
                new CategoryRequest("적금이체", TransactionType.EXPENSE, true, true)
        );

        assertThat(category.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(category.isIncludeInSavingsRate()).isFalse();
        assertThat(category.isIncludeInInvestmentRate()).isFalse();
    }

    @Test
    @DisplayName("기본 이체 카테고리 - 저축률·투자율 플래그만 수정 가능")
    void updateCategoryAllocation_SystemTransferCategory() {
        Long categoryId = 10L;
        Long userId = 1L;
        TransactionCategory category = TransactionCategory.builder()
                .user(null)
                .name("적금")
                .type(TransactionType.TRANSFER)
                .includeInSavingsRate(true)
                .includeInInvestmentRate(false)
                .build();

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        categoryService.updateCategoryAllocation(
                categoryId,
                userId,
                new com.chaewookim.accountbookformoms.domain.asset.dto.request.CategoryAllocationRequest(false, true)
        );

        assertThat(category.isIncludeInSavingsRate()).isFalse();
        assertThat(category.isIncludeInInvestmentRate()).isTrue();
    }
}
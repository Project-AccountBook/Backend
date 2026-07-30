package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetCopyResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    @DisplayName("예산 생성 - 소프트 삭제된 예산 복구")
    void createBudget_restoreDeleted() {

        // given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest(1L, "2026-07", new BigDecimal("1000"), new BigDecimal("500"));
        Budget deletedBudget = Budget.builder()
                .yearMonth("2026-07")
                .totalBudget(new BigDecimal("100"))
                .expectedExpense(BigDecimal.ZERO)
                .build();
        deletedBudget.delete();

        given(budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(userId, request.yearMonth(), request.categoryId()))
                .willReturn(Optional.empty());
        given(budgetRepository.findByUserIdAndYearMonthAndCategoryIdIncludingDeleted(userId, request.yearMonth(), request.categoryId()))
                .willReturn(Optional.of(deletedBudget));
        given(budgetRepository.save(deletedBudget)).willReturn(deletedBudget);

        // when
        budgetService.createBudget(userId, request);

        // then
        assertThat(deletedBudget.getDeletedAt()).isNull();
        verify(budgetRepository, times(1)).save(deletedBudget);
        verify(userRepository, never()).getReferenceById(any());
        verify(categoryRepository, never()).getReferenceById(any());
    }

    @Test
    @DisplayName("예산 생성 - 성공")
    void createBudget_success() {

        // given
        Long userId = 1L;
        Long categoryId = 1L;
        BudgetRequest request = new BudgetRequest(1L, "2026-06", new BigDecimal("1000"), new BigDecimal("500"));

        given(userRepository.getReferenceById(userId)).willReturn(mock(com.chaewookim.accountbookformoms.domain.user.entity.User.class));
        given(categoryRepository.getReferenceById(categoryId)).willReturn(mock(com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory.class));
        given(budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(any(), any(), any())).willReturn(Optional.empty());
        given(budgetRepository.findByUserIdAndYearMonthAndCategoryIdIncludingDeleted(any(), any(), any())).willReturn(Optional.empty());
        given(budgetRepository.save(any())).willReturn(Budget.builder().build());

        // when
        budgetService.createBudget(userId, request);

        // then
        verify(budgetRepository, times(1)).save(any(Budget.class));
        verify(userRepository, times(1)).getReferenceById(userId);
        verify(categoryRepository, times(1)).getReferenceById(categoryId);
    }

    @Test
    @DisplayName("월간 예산 현황 조회 - 성공")
    void getMonthlyBudgetStatus_success() {

        // given
        Long userId = 1L;
        String yearMonth = "2026-06";

        // when
        List<BudgetResponse> responses = budgetService.getMonthlyBudgetStatus(userId, yearMonth);

        // then
        assertThat(responses).isNotNull();
    }

    @Test
    @DisplayName("월간 예산 요약 조회 - 성공")
    void getMonthlyBudgetSummary_success() {

        // given
        Long userId = 1L;
        String yearMonth = "2026-06";

        given(budgetRepository.findByUserIdAndYearMonth(userId, yearMonth)).willReturn(List.of());
        given(transactionRepository.sumByUserAndType(any(), eq(TransactionType.EXPENSE), any(), any())).willReturn(BigDecimal.ZERO);

        // when
        BudgetSummaryResponse summary = budgetService.getMonthlyBudgetSummary(userId, yearMonth);

        // then
        assertThat(summary.totalPlannedBudgetSum()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("예산 수정 - 성공")
    void updateBudget_success() {

        // given
        Long userId = 1L;
        Long budgetId = 1L;
        Budget budget = mock(Budget.class);
        User user = mock(User.class);

        given(budgetRepository.findById(budgetId)).willReturn(Optional.of(budget));
        given(budget.getUser()).willReturn(user);
        given(user.getId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        budgetService.updateBudget(userId, budgetId, new BudgetRequest(1L, "2026-06", new BigDecimal("2000"), new BigDecimal("0")));

        // then
        verify(budget, times(1)).update(any(), any());
        verify(user, times(1)).updateLastBudgetAlertMonth(null);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("예산 삭제 - 성공")
    void deleteBudget_success() {

        // given
        Long userId = 1L;
        Long budgetId = 1L;
        Budget budget = mock(Budget.class);
        User user = mock(User.class);

        given(budgetRepository.findById(budgetId)).willReturn(Optional.of(budget));
        given(budget.getUser()).willReturn(user);
        given(user.getId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        budgetService.deleteBudget(userId, budgetId);

        // then
        verify(budgetRepository, times(1)).delete(budget);
        verify(user, times(1)).updateLastBudgetAlertMonth(null);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("최근 예산 불러오기 미리보기 - 삭제된 카테고리 제외")
    void previewCopyFromLatest_skipDeletedCategory() {
        Long userId = 1L;
        String targetYearMonth = "2026-08";
        String sourceYearMonth = "2026-06";

        TransactionCategory activeCategory = mock(TransactionCategory.class);
        TransactionCategory deletedCategory = mock(TransactionCategory.class);
        given(activeCategory.getId()).willReturn(1L);
        given(activeCategory.getType()).willReturn(TransactionType.EXPENSE);
        given(deletedCategory.getId()).willReturn(2L);
        given(deletedCategory.getName()).willReturn("삭제된 카테고리");

        Budget activeBudget = Budget.builder()
                .yearMonth(sourceYearMonth)
                .totalBudget(new BigDecimal("1000"))
                .expectedExpense(new BigDecimal("100"))
                .build();
        given(activeBudget.getTransactionCategory()).willReturn(activeCategory);

        Budget deletedCategoryBudget = Budget.builder()
                .yearMonth(sourceYearMonth)
                .totalBudget(new BigDecimal("500"))
                .expectedExpense(BigDecimal.ZERO)
                .build();
        given(deletedCategoryBudget.getTransactionCategory()).willReturn(deletedCategory);

        given(budgetRepository.findByUserIdAndYearMonth(userId, targetYearMonth)).willReturn(List.of());
        given(budgetRepository.findLatestBudgetYearMonthBefore(userId, targetYearMonth))
                .willReturn(Optional.of(sourceYearMonth));
        given(budgetRepository.findByUserIdAndYearMonth(userId, sourceYearMonth))
                .willReturn(List.of(activeBudget, deletedCategoryBudget));
        given(categoryRepository.findAllByUserOrSystem(userId)).willReturn(List.of(activeCategory));

        BudgetCopyResponse preview = budgetService.previewCopyFromLatest(userId, targetYearMonth);

        assertThat(preview.copyCount()).isEqualTo(1);
        assertThat(preview.items()).hasSize(2);
        assertThat(preview.items().stream().filter(BudgetCopyResponse.Item::selected).count()).isEqualTo(1);
        assertThat(preview.items().stream().filter(item -> !item.selected()).findFirst().orElseThrow().skipReason())
                .isEqualTo("DELETED_CATEGORY");
    }

    @Test
    @DisplayName("최근 예산 불러오기 - 대상 월에 예산이 있으면 실패")
    void copyFromLatest_targetMonthNotEmpty() {
        Long userId = 1L;
        String targetYearMonth = "2026-07";

        given(budgetRepository.findByUserIdAndYearMonth(userId, targetYearMonth))
                .willReturn(List.of(mock(Budget.class)));

        assertThatThrownBy(() -> budgetService.copyFromLatest(userId, targetYearMonth))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("최근 예산 불러오기 - 성공")
    void copyFromLatest_success() {
        Long userId = 1L;
        String targetYearMonth = "2026-08";
        String sourceYearMonth = "2026-06";

        TransactionCategory category = mock(TransactionCategory.class);
        given(category.getId()).willReturn(1L);
        given(category.getType()).willReturn(TransactionType.EXPENSE);
        given(category.getName()).willReturn("식비");

        Budget sourceBudget = Budget.builder()
                .yearMonth(sourceYearMonth)
                .totalBudget(new BigDecimal("1000"))
                .expectedExpense(new BigDecimal("200"))
                .build();
        given(sourceBudget.getTransactionCategory()).willReturn(category);

        User user = mock(User.class);

        given(budgetRepository.findByUserIdAndYearMonth(userId, targetYearMonth)).willReturn(List.of());
        given(budgetRepository.findLatestBudgetYearMonthBefore(userId, targetYearMonth))
                .willReturn(Optional.of(sourceYearMonth));
        given(budgetRepository.findByUserIdAndYearMonth(userId, sourceYearMonth)).willReturn(List.of(sourceBudget));
        given(categoryRepository.findAllByUserOrSystem(userId)).willReturn(List.of(category));
        given(budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(userId, targetYearMonth, 1L))
                .willReturn(Optional.empty());
        given(budgetRepository.findByUserIdAndYearMonthAndCategoryIdIncludingDeleted(userId, targetYearMonth, 1L))
                .willReturn(Optional.empty());
        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(categoryRepository.getReferenceById(1L)).willReturn(category);
        given(budgetRepository.save(any())).willReturn(Budget.builder().build());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        BudgetCopyResponse response = budgetService.copyFromLatest(userId, targetYearMonth);

        assertThat(response.copyCount()).isEqualTo(1);
        assertThat(response.items().stream().filter(BudgetCopyResponse.Item::selected).count()).isEqualTo(1);
        verify(budgetRepository, times(1)).save(any(Budget.class));
        verify(user, times(1)).updateLastBudgetAlertMonth(null);
    }
}
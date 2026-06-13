package com.chaewookim.accountbookformoms.domain.budget.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.budget.dto.request.BudgetRequest;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetResponse;
import com.chaewookim.accountbookformoms.domain.budget.dto.response.BudgetSummaryResponse;
import com.chaewookim.accountbookformoms.domain.budget.entity.Budget;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
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
    @DisplayName("예산 생성 - 성공")
    void createBudget_success() {

        // given
        Long userId = 1L;
        Long categoryId = 1L;
        BudgetRequest request = new BudgetRequest(1L, "2026-06", new BigDecimal("1000"), new BigDecimal("500"));

        given(userRepository.getReferenceById(userId)).willReturn(mock(com.chaewookim.accountbookformoms.domain.user.entity.User.class));
        given(categoryRepository.getReferenceById(categoryId)).willReturn(mock(com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory.class));
        given(budgetRepository.findByUserIdAndYearMonthAndTransactionCategoryId(any(), any(), any())).willReturn(Optional.empty());
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
}
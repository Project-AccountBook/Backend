package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.portfolio.application.PortfolioService;
import com.chaewookim.accountbookformoms.domain.portfolio.dto.response.MyPortfolioResponse;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MonthlyAllocationServiceTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransactionCategoryRepository categoryRepository;

    @Mock
    PortfolioService portfolioService;

    @InjectMocks
    MonthlyAllocationService monthlyAllocationService;

    @Test
    @DisplayName("월별 저축·투자 집계 - 역할 계좌와 카테고리 플래그 반영")
    void computeMonthlyAllocation_success() {
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Account checking = account(user, 1L, "월급통장", AccountRole.CHECKING, "1000000");
        Account savings = account(user, 2L, "비상금", AccountRole.SAVINGS, "500000");

        TransactionCategory savingsCategory = category(10L, "비상금", true, false);
        Transaction transfer = transfer(
                user,
                checking,
                savings,
                savingsCategory,
                new BigDecimal("200000"),
                LocalDate.of(2026, 6, 15)
        );

        given(accountRepository.findByUserId(userId)).willReturn(List.of(checking, savings));
        given(categoryRepository.findAllByUserOrSystem(userId)).willReturn(List.of(savingsCategory));
        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of(transfer));

        MonthlyAllocationResponse response = monthlyAllocationService.computeMonthlyAllocation(
                userId,
                "2026-06",
                new BigDecimal("1000000")
        );

        assertThat(response.savings().inflow()).isEqualByComparingTo("200000");
        assertThat(response.savings().net()).isEqualByComparingTo("200000");
        assertThat(response.savings().rate()).isEqualByComparingTo("20.0");
        assertThat(response.investment().net()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("월별 저축·투자 집계 API - 월 수입 포함 응답")
    void getMonthlyAllocationSummary_success() {
        Long userId = 1L;
        given(portfolioService.getMyPortfolio(userId, "2026-06")).willReturn(
                new MyPortfolioResponse(
                        "2026-06",
                        new BigDecimal("1000000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("1000000"),
                        null,
                        null,
                        null
                )
        );
        given(accountRepository.findByUserId(userId)).willReturn(List.of());
        given(categoryRepository.findAllByUserOrSystem(userId)).willReturn(List.of());
        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of());

        var response = monthlyAllocationService.getMonthlyAllocationSummary(userId, "2026-06");

        assertThat(response.yearMonth()).isEqualTo("2026-06");
        assertThat(response.totalIncome()).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("목표 달성률 목록 - 목표가 있는 계좌만 반환")
    void buildGoalProgress_success() {
        Long userId = 1L;
        User user = User.builder().build();

        Account withGoal = Account.builder()
                .user(user)
                .accountName("비상금")
                .initialBalance(new BigDecimal("3000000"))
                .role(AccountRole.SAVINGS)
                .build();
        ReflectionTestUtils.setField(withGoal, "id", 1L);
        ReflectionTestUtils.setField(withGoal, "currentBalance", new BigDecimal("3000000"));
        withGoal.updateGoal(new BigDecimal("5000000"), LocalDate.of(2026, 12, 31));

        Account withoutGoal = account(user, 2L, "월급통장", AccountRole.CHECKING, "1000000");

        given(accountRepository.findByUserId(userId)).willReturn(List.of(withGoal, withoutGoal));

        var result = monthlyAllocationService.buildGoalProgress(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).accountName()).isEqualTo("비상금");
        assertThat(result.get(0).progressPercent()).isEqualTo(60);
    }

    private Account account(User user, Long id, String name, AccountRole role, String balance) {
        Account account = Account.builder()
                .user(user)
                .accountName(name)
                .initialBalance(new BigDecimal(balance))
                .role(role)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        ReflectionTestUtils.setField(account, "currentBalance", new BigDecimal(balance));
        return account;
    }

    private TransactionCategory category(Long id, String name, boolean savings, boolean investment) {
        TransactionCategory category = TransactionCategory.builder()
                .user(null)
                .name(name)
                .type(TransactionType.TRANSFER)
                .includeInSavingsRate(savings)
                .includeInInvestmentRate(investment)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Transaction transfer(
            User user,
            Account source,
            Account target,
            TransactionCategory category,
            BigDecimal amount,
            LocalDate date
    ) {
        return Transaction.builder()
                .user(user)
                .account(source)
                .targetAccount(target)
                .transactionCategory(category)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .transactionDate(date)
                .build();
    }
}

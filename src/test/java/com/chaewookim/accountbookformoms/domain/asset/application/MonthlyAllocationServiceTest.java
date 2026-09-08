package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.MonthlyAllocationResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
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
    PortfolioService portfolioService;

    @InjectMocks
    MonthlyAllocationService monthlyAllocationService;

    @Test
    @DisplayName("월별 저축·투자 집계 - 역할 계좌의 이체 순유입 반영")
    void computeMonthlyAllocation_success() {
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Account checking = account(user, 1L, "월급통장", AccountRole.CHECKING, "1000000");
        Account savings = account(user, 2L, "비상금", AccountRole.SAVINGS, "500000");

        Transaction inflow = transfer(
                user,
                checking,
                savings,
                null,
                new BigDecimal("200000"),
                LocalDate.of(2026, 6, 15)
        );
        Transaction outflow = transfer(
                user,
                savings,
                checking,
                null,
                new BigDecimal("50000"),
                LocalDate.of(2026, 6, 20)
        );

        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of(inflow, outflow));

        MonthlyAllocationResponse response = monthlyAllocationService.computeMonthlyAllocation(
                userId,
                "2026-06",
                new BigDecimal("1000000")
        );

        assertThat(response.savings().inflow()).isEqualByComparingTo("200000");
        assertThat(response.savings().outflow()).isEqualByComparingTo("50000");
        assertThat(response.savings().net()).isEqualByComparingTo("150000");
        assertThat(response.savings().rate()).isEqualByComparingTo("15.0");
        assertThat(response.investment().net()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("월별 저축·투자 집계 - 일반 이체 카테고리에 의존하지 않음")
    void computeMonthlyAllocation_ignoresCategory() {
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Account source = account(user, 1L, "생활비", AccountRole.CHECKING, "1000000");
        Account target = account(user, 2L, "용돈", AccountRole.CHECKING, "0");
        Transaction transfer = transfer(
                user,
                source,
                target,
                null,
                new BigDecimal("200000"),
                LocalDate.of(2026, 6, 15)
        );

        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of(transfer));

        MonthlyAllocationResponse response = monthlyAllocationService.computeMonthlyAllocation(
                userId,
                "2026-06",
                new BigDecimal("1000000")
        );

        assertThat(response.savings().net()).isEqualByComparingTo("0");
        assertThat(response.investment().net()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("월별 저축·투자 집계 - 삭제된 계좌 이체는 snapshotAccountId로 처리")
    void computeMonthlyAllocation_archivedAccountTransfer() {
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Account savings = account(user, 2L, "비상금", AccountRole.SAVINGS, "500000");
        Transaction transfer = transfer(
                user,
                null,
                savings,
                null,
                new BigDecimal("100000"),
                LocalDate.of(2026, 7, 10)
        );
        ReflectionTestUtils.setField(transfer, "snapshotAccountId", 99L);
        ReflectionTestUtils.setField(transfer, "snapshotAccountName", "삭제된 통장");
        ReflectionTestUtils.setField(transfer, "accountArchived", true);

        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of(transfer));

        MonthlyAllocationResponse response = monthlyAllocationService.computeMonthlyAllocation(
                userId,
                "2026-07",
                new BigDecimal("1000000")
        );

        assertThat(response.savings().inflow()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("역할 변경 후에도 거래 시점 역할로 과거 통계를 계산")
    void computeMonthlyAllocation_preservesSnapshotAfterRoleChange() {
        Long userId = 1L;
        User user = User.builder().build();
        Account checking = account(user, 1L, "생활비", AccountRole.CHECKING, "1000000");
        Account savings = account(user, 2L, "저축", AccountRole.SAVINGS, "0");
        Transaction transfer = transfer(user, checking, savings, null,
                new BigDecimal("200000"), LocalDate.of(2026, 6, 15));

        savings.updateRole(AccountRole.CHECKING);
        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of(transfer));

        MonthlyAllocationResponse response = monthlyAllocationService.computeMonthlyAllocation(
                userId, "2026-06", new BigDecimal("1000000"));

        assertThat(response.savings().inflow()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("삭제된 저축·투자 계좌도 역할 스냅샷으로 과거 통계를 계산")
    void computeMonthlyAllocation_preservesDeletedSavingsAndInvestmentAccounts() {
        Long userId = 1L;
        User user = User.builder().build();
        Account savings = account(user, 1L, "저축", AccountRole.SAVINGS, "300000");
        Account investment = account(user, 2L, "투자", AccountRole.INVESTMENT, "0");
        Transaction transfer = transfer(user, savings, investment, null,
                new BigDecimal("100000"), LocalDate.of(2026, 6, 15));
        ReflectionTestUtils.setField(transfer, "account", null);
        ReflectionTestUtils.setField(transfer, "targetAccount", null);

        given(transactionRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .willReturn(List.of(transfer));

        MonthlyAllocationResponse response = monthlyAllocationService.computeMonthlyAllocation(
                userId, "2026-06", new BigDecimal("1000000"));

        assertThat(response.savings().outflow()).isEqualByComparingTo("100000");
        assertThat(response.investment().inflow()).isEqualByComparingTo("100000");
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
        Account creditCardGoal = Account.builder()
                .user(user)
                .accountName("카드")
                .initialBalance(new BigDecimal("-100000"))
                .kind(AccountKind.CREDIT_CARD)
                .creditLimit(new BigDecimal("1000000"))
                .build();

        Account loanGoal = Account.builder()
                .user(user)
                .accountName("주택담보대출")
                .initialBalance(new BigDecimal("-10000000"))
                .kind(AccountKind.LOAN)
                .build();
        ReflectionTestUtils.setField(loanGoal, "currentBalance", new BigDecimal("-7000000"));
        loanGoal.updateGoal(BigDecimal.ZERO, LocalDate.of(2030, 12, 31));

        given(accountRepository.findByUserId(userId))
                .willReturn(List.of(withGoal, withoutGoal, creditCardGoal, loanGoal));

        var result = monthlyAllocationService.buildGoalProgress(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).accountName()).isEqualTo("비상금");
        assertThat(result.get(0).progressPercent()).isEqualTo(60);
        assertThat(result.get(1).accountName()).isEqualTo("주택담보대출");
        assertThat(result.get(1).progressPercent()).isEqualTo(30);
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
                .description("test")
                .build();
    }
}

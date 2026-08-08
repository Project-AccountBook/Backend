package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionFrequency;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedTransactionExecutorTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private FixedTransactionExecutor fixedTransactionExecutor;

    private FixedTransaction fixedTransaction;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = Account.builder().build();
        ReflectionTestUtils.setField(account, "id", 10L);

        TransactionCategory category = TransactionCategory.builder().build();
        ReflectionTestUtils.setField(category, "id", 20L);

        fixedTransaction = FixedTransaction.builder()
                .user(user)
                .account(account)
                .transactionCategory(category)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10000"))
                .frequency(TransactionFrequency.MONTHLY)
                .repeatDay(today.getDayOfMonth())
                .startDate(today.minusDays(1))
                .description("월세")
                .build();
    }

    @Test
    @DisplayName("실행일이면 거래를 생성한다")
    void executeIfDue_OnExecutionDay() {
        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isTrue();
        verify(transactionService).createTransactionFromFixed(eq(1L), any(TransactionRequest.class));
        assertThat(fixedTransaction.getLastExecutedDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("실행일이 아니면 거래를 생성하지 않는다")
    void executeIfDue_NotExecutionDay() {
        int otherDay = today.getDayOfMonth() == 1 ? 2 : 1;
        ReflectionTestUtils.setField(fixedTransaction, "repeatDay", otherDay);

        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isFalse();
        verify(transactionService, never()).createTransactionFromFixed(any(), any());
    }

    @Test
    @DisplayName("오늘 이미 실행했으면 거래를 생성하지 않는다")
    void executeIfDue_AlreadyExecutedToday() {
        ReflectionTestUtils.setField(fixedTransaction, "lastExecutedDate", today);

        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isFalse();
        verify(transactionService, never()).createTransactionFromFixed(any(), any());
    }

    @Test
    @DisplayName("실행일이 지났으면 예정일 기준으로 보정 생성한다")
    void executeIfDue_CatchUpMissedExecutionDate() {
        LocalDate missedDate = today.minusDays(1);
        ReflectionTestUtils.setField(fixedTransaction, "repeatDay", missedDate.getDayOfMonth());
        ReflectionTestUtils.setField(fixedTransaction, "nextExecutionDate", missedDate);

        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isTrue();
        assertThat(fixedTransaction.getLastExecutedDate()).isEqualTo(missedDate);
        verify(transactionService).createTransactionFromFixed(eq(1L), argThat(request ->
                request.transactionDate().equals(missedDate)));
    }

    @Test
    @DisplayName("이체 고정 거래면 대상 계좌 ID를 넘겨 생성한다")
    void executeIfDue_TransferIncludesTargetAccount() {
        Account targetAccount = Account.builder().build();
        ReflectionTestUtils.setField(targetAccount, "id", 11L);
        ReflectionTestUtils.setField(fixedTransaction, "type", TransactionType.TRANSFER);
        ReflectionTestUtils.setField(fixedTransaction, "targetAccount", targetAccount);

        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isTrue();
        verify(transactionService).createTransactionFromFixed(eq(1L), argThat(request ->
                request.type() == TransactionType.TRANSFER
                        && request.accountId().equals(10L)
                        && request.targetAccountId().equals(11L)));
    }
}

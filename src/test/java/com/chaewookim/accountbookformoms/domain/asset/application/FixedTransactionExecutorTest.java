package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.FixedTransactionExecutionFailure;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionFrequency;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.asset.event.FixedTransactionExecutionFailedEvent;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedTransactionExecutorTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FixedTransactionExecutor fixedTransactionExecutor;

    private FixedTransaction fixedTransaction;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Account account = Account.builder().accountName("생활비").build();
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
        assertThat(fixedTransaction.hasExecutionFailure()).isFalse();
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

    @Test
    @DisplayName("잔액 부족이면 실패 상태로 남기고 알림 이벤트를 발행한다")
    void executeIfDue_InsufficientBalanceMarksFailure() {
        doThrow(new CustomException(AssetErrorCode.INSUFFICIENT_BALANCE))
                .when(transactionService).createTransactionFromFixed(any(), any());

        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isFalse();
        assertThat(fixedTransaction.hasExecutionFailure()).isTrue();
        assertThat(fixedTransaction.getFailureReason()).isEqualTo(FixedTransactionExecutionFailure.INSUFFICIENT_BALANCE);
        assertThat(fixedTransaction.getFailedExecutionDate()).isEqualTo(today);
        assertThat(fixedTransaction.getLastExecutedDate()).isNull();
        verify(eventPublisher).publishEvent(any(FixedTransactionExecutionFailedEvent.class));
    }

    @Test
    @DisplayName("이미 실패 상태면 자동 재시도하지 않는다")
    void executeIfDue_SkipsWhenAlreadyFailed() {
        fixedTransaction.markExecutionFailed(today, FixedTransactionExecutionFailure.INSUFFICIENT_BALANCE);

        boolean executed = fixedTransactionExecutor.executeIfDue(fixedTransaction, today);

        assertThat(executed).isFalse();
        verify(transactionService, never()).createTransactionFromFixed(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("실패 회차 재실행에 성공하면 실패 상태를 해제한다")
    void retryFailedOccurrence_SuccessClearsFailure() {
        fixedTransaction.markExecutionFailed(today, FixedTransactionExecutionFailure.INSUFFICIENT_BALANCE);

        fixedTransactionExecutor.retryFailedOccurrence(fixedTransaction);

        assertThat(fixedTransaction.hasExecutionFailure()).isFalse();
        assertThat(fixedTransaction.getLastExecutedDate()).isEqualTo(today);
        verify(transactionService).createTransactionFromFixed(eq(1L), argThat(request ->
                request.transactionDate().equals(today)));
    }

    @Test
    @DisplayName("실패하지 않은 고정 거래를 재실행하면 예외가 발생한다")
    void retryFailedOccurrence_NotFailed() {
        assertThatThrownBy(() -> fixedTransactionExecutor.retryFailedOccurrence(fixedTransaction))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(AssetErrorCode.FIXED_TRANSACTION_NOT_FAILED);
    }
}

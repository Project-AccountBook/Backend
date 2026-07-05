package com.chaewookim.accountbookformoms.domain.asset.scheduler;

import com.chaewookim.accountbookformoms.domain.asset.application.TransactionService;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FixedTransactionSchedulerTest {

    @Mock
    private FixedTransactionRepository fixedTransactionRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private FixedTransactionScheduler scheduler;

    private FixedTransaction fixedTransaction;

    @BeforeEach
    void setUp() {

        User user = mock(User.class);
        Account account = mock(Account.class);
        TransactionCategory category = mock(TransactionCategory.class);

        when(user.getId()).thenReturn(1L);
        when(account.getId()).thenReturn(1L);
        when(category.getId()).thenReturn(1L);

        fixedTransaction = FixedTransaction.builder()
                .user(user)
                .account(account)
                .transactionCategory(category)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10000"))
                .frequency(TransactionFrequency.MONTHLY)
                .repeatDay(LocalDate.now().getDayOfMonth())
                .startDate(LocalDate.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("고정 거래 스케줄러 - 성공")
    void processFixedTransactions_Success() {

        // given
        when(fixedTransactionRepository.findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class))).thenReturn(List.of(fixedTransaction));

        // when
        scheduler.processFixedTransactions();

        // then
        verify(transactionService, times(1)).createTransaction(any(), any());
    }

    @Test
    @DisplayName("고정 거래 스케줄러 - 오늘 이미 실행한 거래는 다시 생성하지 않음")
    void processFixedTransactions_AlreadyExecuted() {

        // given
        ReflectionTestUtils.setField(fixedTransaction, "lastExecutedDate", LocalDate.now());
        when(fixedTransactionRepository.findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class))).thenReturn(List.of(fixedTransaction));

        // when
        scheduler.processFixedTransactions();

        // then
        verify(transactionService, never()).createTransaction(any(), any());
    }

    @Test
    @DisplayName("고정 거래 스케줄러 - 실행일이 아니면 생성하지 않음")
    void processFixedTransactions_NotTargetDay() {

        // given
        ReflectionTestUtils.setField(fixedTransaction, "repeatDay", LocalDate.now().getDayOfMonth() + 1);
        when(fixedTransactionRepository.findAllByIsActiveTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class))).thenReturn(List.of(fixedTransaction));

        // when
        scheduler.processFixedTransactions();

        // then
        verify(transactionService, never()).createTransaction(any(), any());
    }
}

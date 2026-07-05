package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.FixedTransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.FixedTransactionResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionFrequency;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixedTransactionServiceTest {

    @Mock
    private FixedTransactionRepository fixedTransactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @InjectMocks
    private FixedTransactionService fixedTransactionService;

    private final LocalDate now = LocalDate.now();

    @Test
    @DisplayName("고정 내역 생성 - 성공")
    void createFixedTransaction_Success() {

        // given
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        FixedTransactionRequest request = new FixedTransactionRequest(1L, 1L, TransactionType.EXPENSE, new BigDecimal("10000"), TransactionFrequency.MONTHLY, 1, null, now, null, "desc");

        given(accountRepository.findByIdAndUserId(any(), any())).willReturn(Optional.of(Account.builder().build()));
        given(categoryRepository.findById(any())).willReturn(Optional.of(TransactionCategory.builder().build()));

        FixedTransaction savedFt = FixedTransaction.builder()
                .type(TransactionType.EXPENSE)
                .frequency(TransactionFrequency.MONTHLY)
                .startDate(now)
                .repeatDay(1)
                .build();
        ReflectionTestUtils.setField(savedFt, "id", 1L);
        given(fixedTransactionRepository.save(any())).willReturn(savedFt);

        // when
        Long id = fixedTransactionService.createFixedTransaction(userId, user, request);

        // then
        assertThat(id).isEqualTo(1L);
    }

    @Test
    @DisplayName("고정 내역 목록 조회 - 성공")
    void getFixedTransactions_Success() {

        // given
        Long userId = 1L;
        FixedTransaction ft = FixedTransaction.builder()
                .type(TransactionType.EXPENSE)
                .frequency(TransactionFrequency.MONTHLY)
                .startDate(now)
                .repeatDay(1)
                .account(Account.builder().accountName("Bank").build())
                .transactionCategory(TransactionCategory.builder().name("Food").build())
                .build();
        given(fixedTransactionRepository.findAllByUserId(userId)).willReturn(List.of(ft));

        // when
        List<FixedTransactionResponse> results = fixedTransactionService.getFixedTransactions(userId);

        // then
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("고정 내역 수정 - 성공")
    void updateFixedTransaction_Success() {

        // given
        Long userId = 1L;
        Long id = 1L;
        FixedTransactionRequest request = new FixedTransactionRequest(1L, 1L, TransactionType.EXPENSE, new BigDecimal("20000"), TransactionFrequency.MONTHLY, 5, null, now, null, "updated");
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        FixedTransaction ft = FixedTransaction.builder()
                .user(user)
                .type(TransactionType.EXPENSE)
                .frequency(TransactionFrequency.MONTHLY)
                .startDate(now)
                .repeatDay(1)
                .build();

        given(fixedTransactionRepository.findById(id)).willReturn(Optional.of(ft));
        given(accountRepository.findByIdAndUserId(any(), any())).willReturn(Optional.of(Account.builder().build()));
        given(categoryRepository.findById(any())).willReturn(Optional.of(TransactionCategory.builder().build()));

        // when
        fixedTransactionService.updateFixedTransaction(userId, id, request);

        // then
        assertThat(ft.getAmount()).isEqualTo(new BigDecimal("20000"));
    }

    @Test
    @DisplayName("고정 내역 활성/비활성 토글 - 성공")
    void toggleActiveStatus_Success() {

        // given
        Long userId = 1L;
        Long id = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        FixedTransaction ft = FixedTransaction.builder()
                .user(user)
                .type(TransactionType.EXPENSE)
                .frequency(TransactionFrequency.MONTHLY)
                .startDate(now)
                .repeatDay(1)
                .build();
        ReflectionTestUtils.setField(ft, "isActive", true);

        given(fixedTransactionRepository.findById(id)).willReturn(Optional.of(ft));

        // when
        fixedTransactionService.toggleActiveStatus(userId, id);

        // then
        assertThat(ft.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("고정 내역 삭제 - 성공")
    void deleteFixedTransaction_Success() {

        // given
        Long userId = 1L;
        Long id = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        FixedTransaction ft = FixedTransaction.builder()
                .user(user)
                .type(TransactionType.EXPENSE)
                .frequency(TransactionFrequency.MONTHLY)
                .startDate(now)
                .repeatDay(1)
                .build();

        given(fixedTransactionRepository.findById(id)).willReturn(Optional.of(ft));

        // when
        fixedTransactionService.deleteFixedTransaction(userId, id);

        // then
        verify(fixedTransactionRepository).delete(ft);
    }
}
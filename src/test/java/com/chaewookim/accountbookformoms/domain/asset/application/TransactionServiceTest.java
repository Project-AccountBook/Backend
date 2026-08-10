package com.chaewookim.accountbookformoms.domain.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.chaewookim.accountbookformoms.domain.asset.dao.*;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.TransactionResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.*;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private Cache dashboardCache;

    @InjectMocks
    private TransactionService transactionService;

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    @Test
    @DisplayName("일반 거래 등록 -  성공")
    void createNormalTransaction_Success() {

        // given
        User user = User.builder().build(); setId(user, 1L);
        Account account = Account.builder().user(user).initialBalance(new BigDecimal("1000")).build(); setId(account, 1L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);

        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(account));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(transactionRepository.save(any())).willAnswer(i -> {
            Transaction t = i.getArgument(0);
            setId(t, 1L);
            return t;
        });

        // when
        TransactionRequest req = new TransactionRequest(1L, null, 1L, TransactionType.INCOME, new BigDecimal("500"), LocalDate.now(), "월급");
        transactionService.createTransaction(1L, req);

        // then
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("이체 거래 등록 - 성공")
    void createTransferTransaction_Success() {

        // given
        User user = User.builder().build(); setId(user, 1L);
        Account src = Account.builder().user(user).initialBalance(new BigDecimal("1000")).build(); setId(src, 1L);
        Account target = Account.builder().user(user).initialBalance(new BigDecimal("1000")).build(); setId(target, 2L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);

        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(src));
        given(accountRepository.findByIdWithLock(2L)).willReturn(Optional.of(target));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(transactionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // when
        TransactionRequest req = new TransactionRequest(1L, 2L, 1L, TransactionType.TRANSFER, new BigDecimal("300"), LocalDate.now(), "이체");
        transactionService.createTransaction(1L, req);

        // then
        assertThat(src.getCurrentBalance()).isEqualByComparingTo("700");
        assertThat(target.getCurrentBalance()).isEqualByComparingTo("1300");
    }

    @Test
    @DisplayName("계좌 거래 내역 목록 조회 - 성공")
    void getTransactions_Success() {

        // given
        User user = User.builder().build(); setId(user, 1L);
        Account account = Account.builder().user(user).initialBalance(new BigDecimal("1000")).build(); setId(account, 1L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);
        Transaction t = Transaction.builder().user(user).account(account).transactionCategory(category).build(); setId(t, 1L);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(List.of(t), pageable, 1);

        given(transactionRepository.findAllByUserIdAndAccountIdAndTransactionDateBetween(any(), any(), any(), any(), any()))
                .willReturn(page);

        // when
        Page<TransactionResponse> result = transactionService.getTransactions(1L, 1L, LocalDate.now(), LocalDate.now(), pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("거래 내역 상세 조회- 성공")
    void getTransaction_Success() {

        // given
        User user = User.builder().build(); setId(user, 1L);
        Account account = Account.builder().user(user).build(); setId(account, 1L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);
        Transaction t = Transaction.builder().user(user).account(account).transactionCategory(category).type(TransactionType.EXPENSE).amount(BigDecimal.TEN).build();

        given(transactionRepository.findById(1L)).willReturn(Optional.of(t));

        // when
        TransactionResponse result = transactionService.getTransaction(1L);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("거래 내역 수정 - 성공")
    void updateTransaction_Success() {

        // given
        given(cacheManager.getCache("dashboard")).willReturn(dashboardCache);
        User user = User.builder().build(); setId(user, 1L);
        Account account = Account.builder().user(user).initialBalance(new BigDecimal("1000")).build(); setId(account, 1L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);
        Transaction t = Transaction.builder().user(user).account(account).transactionCategory(category).type(TransactionType.EXPENSE).amount(new BigDecimal("100")).transactionDate(LocalDate.now()).build();
        setId(t, 1L);

        given(transactionRepository.findById(1L)).willReturn(Optional.of(t));
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(account));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        TransactionRequest req = new TransactionRequest(1L, null, 1L, TransactionType.EXPENSE, new BigDecimal("200"), LocalDate.now(), "수정");
        transactionService.updateTransaction(1L, 1L, req);

        // then
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("900"); // 1000 - 200 + 100(기존값복구)
    }

    @Test
    @DisplayName("거래 내역 삭제 - 성공")
    void deleteTransaction_Success() {

        // given
        given(cacheManager.getCache("dashboard")).willReturn(dashboardCache);
        User user = User.builder().build(); setId(user, 1L);
        Account account = Account.builder().user(user).initialBalance(new BigDecimal("1000")).build(); setId(account, 1L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);
        Transaction t = Transaction.builder().user(user).account(account).transactionCategory(category).type(TransactionType.EXPENSE).amount(new BigDecimal("100")).transactionDate(LocalDate.now()).build();
        setId(t, 1L);

        given(transactionRepository.findById(1L)).willReturn(Optional.of(t));
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(account));

        // when
        transactionService.deleteTransaction(1L, 1L);

        // then
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1100"); // 1000 + 100
    }
}
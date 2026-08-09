package com.chaewookim.accountbookformoms.domain.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.chaewookim.accountbookformoms.domain.asset.dao.*;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.TransactionResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.*;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountKind;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.verify;

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
    private Cache dashboardCache;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getSnapshotAccountRole()).isEqualTo(AccountRole.CHECKING);
        assertThat(captor.getValue().getSnapshotTargetAccountRole()).isNull();
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
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getSnapshotAccountRole()).isEqualTo(AccountRole.CHECKING);
        assertThat(captor.getValue().getSnapshotTargetAccountRole()).isEqualTo(AccountRole.CHECKING);
    }

    @Test
    @DisplayName("신용카드 지출은 잔액을 음수로 기록")
    void createCreditCardExpense_success() {
        User user = User.builder().build(); setId(user, 1L);
        Account card = Account.builder()
                .user(user).initialBalance(BigDecimal.ZERO)
                .kind(AccountKind.CREDIT_CARD).creditLimit(new BigDecimal("1000")).build();
        setId(card, 1L);
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(card));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(transactionRepository.save(any())).willAnswer(i -> i.getArgument(0));

        transactionService.createTransaction(1L, new TransactionRequest(
                1L, null, 1L, TransactionType.EXPENSE, new BigDecimal("300"), LocalDate.now(), "카드 결제"));

        assertThat(card.getCurrentBalance()).isEqualByComparingTo("-300");
    }

    @Test
    @DisplayName("신용카드에는 수입을 직접 등록할 수 없음")
    void createCreditCardIncome_rejected() {
        User user = User.builder().build(); setId(user, 1L);
        Account card = Account.builder()
                .user(user).initialBalance(new BigDecimal("-300"))
                .kind(AccountKind.CREDIT_CARD).creditLimit(new BigDecimal("1000")).build();
        setId(card, 1L);
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(card));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transactionService.createTransaction(1L, new TransactionRequest(
                                1L, null, 1L, TransactionType.INCOME,
                                new BigDecimal("100"), LocalDate.now(), "잘못된 수입")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(AssetErrorCode.INCOME_NOT_ALLOWED_FOR_LIABILITY));
        assertThat(card.getCurrentBalance()).isEqualByComparingTo("-300");
    }

    @Test
    @DisplayName("자산 계좌에서 신용카드로 이체하면 카드 부채를 상환")
    void transferAssetToCreditCard_success() {
        User user = User.builder().build(); setId(user, 1L);
        Account asset = account(user, 1L, AccountKind.ASSET, "1000", null);
        Account card = account(user, 2L, AccountKind.CREDIT_CARD, "-500", "1000");
        prepareTransfer(asset, card);

        transactionService.createTransaction(1L, transferRequest("200"));

        assertThat(asset.getCurrentBalance()).isEqualByComparingTo("800");
        assertThat(card.getCurrentBalance()).isEqualByComparingTo("-300");
    }

    @Test
    @DisplayName("대출 계좌에서 자산 계좌로 이체하면 대출과 자산이 함께 증가")
    void transferLoanToAsset_success() {
        User user = User.builder().build(); setId(user, 1L);
        Account loan = account(user, 1L, AccountKind.LOAN, "-1000", null);
        Account asset = account(user, 2L, AccountKind.ASSET, "0", null);
        prepareTransfer(loan, asset);

        transactionService.createTransaction(1L, transferRequest("200"));

        assertThat(loan.getCurrentBalance()).isEqualByComparingTo("-1200");
        assertThat(loan.getDisbursedAmount()).isEqualByComparingTo("1200");
        assertThat(asset.getCurrentBalance()).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("자산 계좌에서 대출 계좌로 이체하면 대출을 상환")
    void transferAssetToLoan_success() {
        User user = User.builder().build(); setId(user, 1L);
        Account asset = account(user, 1L, AccountKind.ASSET, "1000", null);
        Account loan = account(user, 2L, AccountKind.LOAN, "-1000", null);
        prepareTransfer(asset, loan);

        transactionService.createTransaction(1L, transferRequest("300"));

        assertThat(asset.getCurrentBalance()).isEqualByComparingTo("700");
        assertThat(loan.getCurrentBalance()).isEqualByComparingTo("-700");
        assertThat(loan.getDisbursedAmount()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("대출을 부분 실행하고 누적 실행액이 약정액을 넘으면 거부")
    void disburseLoan_rejectsAmountBeyondLimit() {
        User user = User.builder().build(); setId(user, 1L);
        Account loan = loanAccount(user, 1L, "0", "1000", "0");
        Account asset = account(user, 2L, AccountKind.ASSET, "0", null);
        prepareTransfer(loan, asset);

        transactionService.createTransaction(1L, transferRequest("600"));

        assertThat(loan.getCurrentBalance()).isEqualByComparingTo("-600");
        assertThat(loan.getDisbursedAmount()).isEqualByComparingTo("600");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transactionService.createTransaction(1L, transferRequest("401")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(AssetErrorCode.LOAN_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("대출을 전액 상환해도 누적 실행액이 줄지 않아 재인출 불가")
    void repaidLoan_cannotBeRedrawn() {
        User user = User.builder().build(); setId(user, 1L);
        Account loan = loanAccount(user, 1L, "0", "500", "0");
        Account asset = account(user, 2L, AccountKind.ASSET, "0", null);
        prepareTransfer(loan, asset);

        transactionService.createTransaction(1L, transferRequest("500"));
        transactionService.createTransaction(1L, new TransactionRequest(
                2L, 1L, 1L, TransactionType.TRANSFER, new BigDecimal("500"),
                LocalDate.now(), "원금 상환"));

        assertThat(loan.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(loan.getDisbursedAmount()).isEqualByComparingTo("500");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transactionService.createTransaction(1L, transferRequest("1")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(AssetErrorCode.LOAN_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("대출 실행 거래 삭제 시 잔액과 누적 실행액을 함께 역산")
    void deleteLoanDisbursement_reversesBalanceAndDisbursedAmount() {
        given(cacheManager.getCache("dashboard")).willReturn(dashboardCache);
        User user = User.builder().build(); setId(user, 1L);
        Account loan = loanAccount(user, 1L, "0", "1000", "0");
        Account asset = account(user, 2L, AccountKind.ASSET, "0", null);
        loan.disburse(new BigDecimal("300"));
        asset.changeBalance(new BigDecimal("300"));
        Transaction transaction = Transaction.builder()
                .user(user).account(loan).targetAccount(asset)
                .type(TransactionType.TRANSFER).amount(new BigDecimal("300"))
                .transactionDate(LocalDate.now()).build();
        setId(transaction, 10L);
        given(transactionRepository.findById(10L)).willReturn(Optional.of(transaction));
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(loan));
        given(accountRepository.findByIdWithLock(2L)).willReturn(Optional.of(asset));

        transactionService.deleteTransaction(1L, 10L);

        assertThat(loan.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(loan.getDisbursedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("대출이 포함된 잘못된 방향의 이체를 거부")
    void transferLoanToCreditCard_rejected() {
        User user = User.builder().build(); setId(user, 1L);
        Account loan = loanAccount(user, 1L, "0", "1000", "0");
        Account card = account(user, 2L, AccountKind.CREDIT_CARD, "0", "1000");
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(loan));
        given(accountRepository.findByIdWithLock(2L)).willReturn(Optional.of(card));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transactionService.createTransaction(1L, transferRequest("100")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(AssetErrorCode.INVALID_LOAN_TRANSFER));
    }

    @Test
    @DisplayName("대출 계좌의 일반 지출을 거부")
    void createLoanExpense_rejected() {
        User user = User.builder().build(); setId(user, 1L);
        Account loan = loanAccount(user, 1L, "-500", "1000", "500");
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(loan));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transactionService.createTransaction(1L, new TransactionRequest(
                                1L, null, 1L, TransactionType.EXPENSE,
                                new BigDecimal("100"), LocalDate.now(), "잘못된 이자")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(AssetErrorCode.NORMAL_TRANSACTION_NOT_ALLOWED_FOR_LOAN));
    }

    @Test
    @DisplayName("이체 대상 계좌가 타인 소유면 거부")
    void createTransferTransaction_rejectsForeignTarget() {
        User owner = User.builder().build(); setId(owner, 1L);
        User other = User.builder().build(); setId(other, 2L);
        Account source = account(owner, 1L, AccountKind.ASSET, "1000", null);
        Account target = account(other, 2L, AccountKind.ASSET, "0", null);
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(source));
        given(accountRepository.findByIdWithLock(2L)).willReturn(Optional.of(target));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> transactionService.createTransaction(1L, transferRequest("100")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(AssetErrorCode.TRANSACTION_FORBIDDEN));
        assertThat(source.getCurrentBalance()).isEqualByComparingTo("1000");
        assertThat(target.getCurrentBalance()).isEqualByComparingTo("0");
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

    private Account account(User user, Long id, AccountKind kind, String balance, String creditLimit) {
        Account account = Account.builder()
                .user(user)
                .initialBalance(new BigDecimal(balance))
                .kind(kind)
                .creditLimit(creditLimit != null ? new BigDecimal(creditLimit) : null)
                .loanLimit(kind == AccountKind.LOAN ? new BigDecimal(balance).abs().add(new BigDecimal("1000")) : null)
                .disbursedAmount(kind == AccountKind.LOAN ? new BigDecimal(balance).abs() : null)
                .build();
        setId(account, id);
        return account;
    }

    private Account loanAccount(User user, Long id, String balance, String loanLimit, String disbursedAmount) {
        Account account = Account.builder()
                .user(user)
                .initialBalance(new BigDecimal(balance))
                .kind(AccountKind.LOAN)
                .loanLimit(new BigDecimal(loanLimit))
                .disbursedAmount(new BigDecimal(disbursedAmount))
                .build();
        setId(account, id);
        return account;
    }

    private void prepareTransfer(Account source, Account target) {
        TransactionCategory category = TransactionCategory.builder().build(); setId(category, 1L);
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(source));
        given(accountRepository.findByIdWithLock(2L)).willReturn(Optional.of(target));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(transactionRepository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    private TransactionRequest transferRequest(String amount) {
        return new TransactionRequest(
                1L, 2L, 1L, TransactionType.TRANSFER, new BigDecimal(amount), LocalDate.now(), "이체");
    }
}
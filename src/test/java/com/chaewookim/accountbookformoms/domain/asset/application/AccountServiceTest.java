package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountGoalRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountUpdateRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.Transaction;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    FixedTransactionRepository fixedTransactionRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    TransactionCategoryRepository categoryRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    CacheManager cacheManager;

    @InjectMocks
    AccountService accountService;

    @Test
    @DisplayName("계좌 생성 - 성공")
    void createAccount_success() {

        // given
        User user = User.builder().build();
        AccountRequest request = new AccountRequest("내 통장", BigDecimal.valueOf(10000), null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByUserIdAndAccountNameIncludingDeleted(1L, "내 통장")).willReturn(Optional.empty());
        given(accountRepository.existsByUserIdAndAccountName(1L, "내 통장")).willReturn(false);
        given(accountRepository.save(any(Account.class))).willReturn(Account.builder().build());

        // when
        accountService.createAccount(1L, request);

        // then
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("계좌 목록 조회 - 성공")
    void getAccounts_success() {

        // given
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Account account1 = Account.builder().user(user).accountName("계좌1").build();
        Account account2 = Account.builder().user(user).accountName("계좌2").build();

        given(accountRepository.findByUserId(userId)).willReturn(List.of(account1, account2));

        // when
        List<AccountResponse> accounts = accountService.getAccounts(userId);

        // then
        assertThat(accounts).hasSize(2);
        assertThat(accounts.get(0).accountName()).isEqualTo("계좌1");
        assertThat(accounts.get(1).accountName()).isEqualTo("계좌2");
    }

    @Test
    @DisplayName("계좌 단건 조회 - 성공")
    void getAccount_success() {

        // given
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder().user(user).accountName("통장").build();

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        // when
        AccountResponse response = accountService.getAccount(userId, accountId);

        // then
        assertThat(response.accountName()).isEqualTo("통장");
    }

    @Test
    @DisplayName("계좌 수정 - 성공")
    void updateAccount_success() {

        // given
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("기존 이름")
                .initialBalance(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(account, "currentBalance", BigDecimal.valueOf(15000));

        TransactionCategory category = TransactionCategory.builder()
                .name("잔고 조정")
                .type(TransactionType.INCOME)
                .build();
        given(accountRepository.findByIdWithLock(accountId)).willReturn(Optional.of(account));
        given(accountRepository.existsByUserIdAndAccountNameAndIdNot(userId, "새 이름", accountId)).willReturn(false);
        given(categoryRepository.findByUserIsNullAndNameAndType("잔고 조정", TransactionType.INCOME))
                .willReturn(Optional.of(category));
        AccountUpdateRequest request = new AccountUpdateRequest(
                "새 이름",
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(20000),
                AccountRole.SAVINGS
        );

        // when
        accountService.updateAccount(userId, accountId, request);

        // then
        assertThat(account.getAccountName()).isEqualTo("새 이름");
        assertThat(account.getInitialBalance()).isEqualByComparingTo(BigDecimal.valueOf(12000));
        assertThat(account.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(account.getRole()).isEqualTo(AccountRole.SAVINGS);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction adjustment = transactionCaptor.getValue();
        assertThat(adjustment.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(adjustment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(adjustment.getDescription()).isEqualTo("잔고 조정");
    }

    @Test
    @DisplayName("계좌 잔고 감소 수정 - 지출 조정 내역 생성")
    void updateAccount_decreaseBalance_createsExpenseAdjustment() {
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("생활비")
                .initialBalance(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(account, "currentBalance", BigDecimal.valueOf(15000));
        TransactionCategory category = TransactionCategory.builder()
                .name("잔고 조정")
                .type(TransactionType.EXPENSE)
                .build();

        given(accountRepository.findByIdWithLock(accountId)).willReturn(Optional.of(account));
        given(categoryRepository.findByUserIsNullAndNameAndType("잔고 조정", TransactionType.EXPENSE))
                .willReturn(Optional.of(category));

        accountService.updateAccount(
                userId,
                accountId,
                new AccountUpdateRequest(
                        "생활비",
                        BigDecimal.valueOf(10000),
                        BigDecimal.valueOf(7000),
                        AccountRole.CHECKING
                )
        );

        assertThat(account.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(8000));
    }

    @Test
    @DisplayName("계좌 잔고가 같으면 조정 내역을 생성하지 않음")
    void updateAccount_sameBalance_doesNotCreateAdjustment() {
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("생활비")
                .initialBalance(BigDecimal.valueOf(10000))
                .build();

        given(accountRepository.findByIdWithLock(accountId)).willReturn(Optional.of(account));

        accountService.updateAccount(
                userId,
                accountId,
                new AccountUpdateRequest(
                        "이름 변경",
                        BigDecimal.valueOf(10000),
                        BigDecimal.valueOf(10000),
                        AccountRole.SAVINGS
                )
        );

        assertThat(account.getAccountName()).isEqualTo("이름 변경");
        assertThat(account.getRole()).isEqualTo(AccountRole.SAVINGS);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("계좌 수정 - 타인 계좌 수정 시도 시 예외 발생")
    void updateAccount_fail_accessDenied() {

        // given
        Long ownerId = 1L;
        Long hackerId = 2L;
        User owner = User.builder().build();
        ReflectionTestUtils.setField(owner, "id", ownerId);
        Account account = Account.builder().user(owner).build();

        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(account));

        // when & then
        assertThrows(CustomException.class, () ->
                accountService.updateAccount(
                        hackerId,
                        1L,
                        new AccountUpdateRequest("이름", BigDecimal.ZERO, BigDecimal.ZERO, null)
                )
        );
    }

    @Test
    @DisplayName("계좌 삭제 - 성공")
    void deleteAccount_success() {

        // given
        Long userId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder().user(user).build();

        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(fixedTransactionRepository.softDeleteByAccountId(1L)).willReturn(0);

        // when
        accountService.deleteAccount(userId, 1L);

        // then
        verify(fixedTransactionRepository).softDeleteByAccountId(1L);
        verify(transactionRepository).backfillSourceAccountSnapshot(1L, account.getAccountName());
        verify(transactionRepository).backfillTargetAccountSnapshot(1L, account.getAccountName());
        verify(transactionRepository).markSourceAccountArchived(1L);
        verify(transactionRepository).markTargetAccountArchived(1L);
        verify(accountRepository).delete(account);
    }

    @Test
    @DisplayName("계좌 생성 - 중복 이름 시 예외 발생")
    void createAccount_fail_duplicateName() {

        // given
        User user = User.builder().build();
        AccountRequest request = new AccountRequest("내 통장", BigDecimal.valueOf(10000), null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByUserIdAndAccountNameIncludingDeleted(1L, "내 통장")).willReturn(Optional.empty());
        given(accountRepository.existsByUserIdAndAccountName(1L, "내 통장")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> accountService.createAccount(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.DUPLICATE_ACCOUNT_NAME);
                });
    }

    @Test
    @DisplayName("계좌 생성 - 삭제된 계좌와 동일 이름이면 복원")
    void createAccount_success_restoreDeleted() {

        // given
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Account deletedAccount = Account.builder()
                .user(user)
                .accountName("내 통장")
                .initialBalance(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(deletedAccount, "id", 10L);
        deletedAccount.delete();

        AccountRequest request = new AccountRequest("내 통장", BigDecimal.valueOf(5000), AccountRole.INVESTMENT);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByUserIdAndAccountNameIncludingDeleted(1L, "내 통장"))
                .willReturn(Optional.of(deletedAccount));
        given(accountRepository.save(deletedAccount)).willReturn(deletedAccount);

        // when
        Long accountId = accountService.createAccount(1L, request);

        // then
        assertThat(accountId).isEqualTo(10L);
        assertThat(deletedAccount.getDeletedAt()).isNull();
        assertThat(deletedAccount.getInitialBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(deletedAccount.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(deletedAccount.getRole()).isEqualTo(AccountRole.INVESTMENT);
    }

    @Test
    @DisplayName("계좌 목표 수정 - 성공")
    void updateAccountGoal_success() {
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("비상금")
                .initialBalance(BigDecimal.valueOf(1000000))
                .role(AccountRole.SAVINGS)
                .build();

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        accountService.updateAccountGoal(
                userId,
                accountId,
                new AccountGoalRequest(BigDecimal.valueOf(5000000), LocalDate.of(2026, 12, 31))
        );

        assertThat(account.getGoalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000000));
        assertThat(account.getGoalDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("계좌 목표 삭제 - 성공")
    void clearAccountGoal_success() {
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("비상금")
                .initialBalance(BigDecimal.valueOf(1000000))
                .role(AccountRole.SAVINGS)
                .build();
        account.updateGoal(BigDecimal.valueOf(5000000), LocalDate.of(2026, 12, 31));

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        accountService.clearAccountGoal(userId, accountId);

        assertThat(account.getGoalAmount()).isNull();
        assertThat(account.getGoalDate()).isNull();
        assertThat(account.getRole()).isEqualTo(AccountRole.SAVINGS);
    }

    @Test
    @DisplayName("계좌 단건 조회 - 목표 달성률 포함")
    void getAccount_success_withProgressPercent() {
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("비상금")
                .initialBalance(BigDecimal.valueOf(3000000))
                .role(AccountRole.SAVINGS)
                .build();
        ReflectionTestUtils.setField(account, "currentBalance", BigDecimal.valueOf(3000000));
        account.updateGoal(BigDecimal.valueOf(5000000), LocalDate.of(2026, 12, 31));

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount(userId, accountId);

        assertThat(response.role()).isEqualTo(AccountRole.SAVINGS);
        assertThat(response.goalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000000));
        assertThat(response.progressPercent()).isEqualTo(60);
    }

    @Test
    @DisplayName("계좌 수정 - 중복 이름 시 예외 발생")
    void updateAccount_fail_duplicateName() {

        // given
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder()
                .user(user)
                .accountName("기존 이름")
                .initialBalance(BigDecimal.valueOf(10000))
                .build();

        given(accountRepository.findByIdWithLock(accountId)).willReturn(Optional.of(account));
        given(accountRepository.existsByUserIdAndAccountNameAndIdNot(userId, "중복 이름", accountId)).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                accountService.updateAccount(
                        userId,
                        accountId,
                        new AccountUpdateRequest("중복 이름", BigDecimal.ZERO, BigDecimal.ZERO, null)
                )
        ).isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.DUPLICATE_ACCOUNT_NAME);
                });
    }
}
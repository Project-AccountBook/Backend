package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountGoalRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    ApplicationEventPublisher eventPublisher;

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

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(accountRepository.existsByUserIdAndAccountNameAndIdNot(userId, "새 이름", accountId)).willReturn(false);
        AccountRequest request = new AccountRequest("새 이름", BigDecimal.valueOf(20000), AccountRole.SAVINGS);

        // when
        accountService.updateAccount(userId, accountId, request);

        // then
        assertThat(account.getAccountName()).isEqualTo("새 이름");
        assertThat(account.getInitialBalance()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(account.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(account.getRole()).isEqualTo(AccountRole.SAVINGS);
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

        given(accountRepository.findById(1L)).willReturn(Optional.of(account));

        // when & then
        assertThrows(CustomException.class, () ->
                accountService.updateAccount(hackerId, 1L, new AccountRequest("이름", BigDecimal.ZERO, null))
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
        given(fixedTransactionRepository.findAllByAccountId(1L)).willReturn(List.of());

        // when
        accountService.deleteAccount(userId, 1L);

        // then
        verify(fixedTransactionRepository).findAllByAccountId(1L);
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

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(accountRepository.existsByUserIdAndAccountNameAndIdNot(userId, "중복 이름", accountId)).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                accountService.updateAccount(userId, accountId, new AccountRequest("중복 이름", BigDecimal.ZERO, null))
        ).isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getErrorCode()).isEqualTo(AssetErrorCode.DUPLICATE_ACCOUNT_NAME);
                });
    }
}
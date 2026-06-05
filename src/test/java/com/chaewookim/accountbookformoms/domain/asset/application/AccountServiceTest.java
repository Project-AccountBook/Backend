package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    AccountService accountService;

    @Test
    @DisplayName("계좌 생성 - 성공")
    void createAccount_success() {

        // given
        User user = User.builder().build();
        AccountRequest request = new AccountRequest("내 통장", BigDecimal.valueOf(10000));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.save(any(Account.class))).willReturn(Account.builder().build());

        // when
        accountService.createAccount(1L, request);

        // then
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("계좌 수정 - 성공")
    void updateAccount_success() {

        // given
        Long userId = 1L;
        Long accountId = 1L;
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        Account account = Account.builder().user(user).accountName("기존 이름").initialBalance(BigDecimal.ZERO).build();

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        AccountRequest request = new AccountRequest("새 이름", BigDecimal.ZERO);

        // when
        accountService.updateAccount(userId, accountId, request);

        // then
        assertThat(account.getAccountName()).isEqualTo("새 이름");
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
                accountService.updateAccount(hackerId, 1L, new AccountRequest("이름", BigDecimal.ZERO))
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

        // when
        accountService.deleteAccount(userId, 1L);

        // then
        verify(accountRepository).delete(account);
    }
}
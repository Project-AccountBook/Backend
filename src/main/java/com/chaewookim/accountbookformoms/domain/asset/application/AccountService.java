package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createAccount(Long userId, AccountRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Account account = Account.builder()
                .user(user)
                .accountName(request.accountName())
                .initialBalance(request.initialBalance())
                .build();

        return accountRepository.save(account).getId();
    }

    public List<AccountResponse> getAccounts(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(Long userId, Long accountId) {
        return new AccountResponse(validateAndGet(userId, accountId));
    }

    @Transactional
    public void updateAccount(Long userId, Long accountId, AccountRequest request) {
        Account account = validateAndGet(userId, accountId);
        account.updateAccountName(request.accountName());
        account.updateInitialBalance(request.initialBalance());
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        accountRepository.delete(validateAndGet(userId, accountId));
    }

    // 공통 검증 로직
    private Account validateAndGet(Long userId, Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUser().getId().equals(userId)) {
            throw new CustomException(UserErrorCode.ACCESS_DENIED);
        }

        return account;
    }
}

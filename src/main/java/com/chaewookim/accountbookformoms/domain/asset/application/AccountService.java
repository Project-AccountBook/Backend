package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountGoalRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.AccountRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.AccountResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.enums.AccountRole;
import com.chaewookim.accountbookformoms.domain.asset.entity.FixedTransaction;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.domain.asset.event.GoalAchievedCheckEvent;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final FixedTransactionRepository fixedTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createAccount(Long userId, AccountRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String accountName = request.accountName().trim();

        Optional<Account> deletedAccount = accountRepository.findByUserIdAndAccountNameIncludingDeleted(userId, accountName);
        if (deletedAccount.isPresent() && deletedAccount.get().getDeletedAt() != null) {
            Account account = deletedAccount.get();
            account.restore();
            account.resetBalance(request.initialBalance());
            account.updateRole(resolveRole(request));
            return accountRepository.save(account).getId();
        }

        if (accountRepository.existsByUserIdAndAccountName(userId, accountName)) {
            throw new CustomException(AssetErrorCode.DUPLICATE_ACCOUNT_NAME);
        }

        Account account = Account.builder()
                .user(user)
                .accountName(accountName)
                .initialBalance(request.initialBalance())
                .role(resolveRole(request))
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
        String accountName = request.accountName().trim();

        if (!accountName.equals(account.getAccountName())
                && accountRepository.existsByUserIdAndAccountNameAndIdNot(userId, accountName, accountId)) {
            throw new CustomException(AssetErrorCode.DUPLICATE_ACCOUNT_NAME);
        }

        account.updateAccountName(accountName);
        account.updateInitialBalance(request.initialBalance());
        if (request.role() != null) {
            account.updateRole(request.role());
        }
        eventPublisher.publishEvent(new GoalAchievedCheckEvent(userId, accountId));
    }

    @Transactional
    public void updateAccountGoal(Long userId, Long accountId, AccountGoalRequest request) {
        Account account = validateAndGet(userId, accountId);
        account.updateGoal(request.goalAmount(), request.goalDate());
        eventPublisher.publishEvent(new GoalAchievedCheckEvent(userId, accountId));
    }

    @Transactional
    public void clearAccountGoal(Long userId, Long accountId) {
        Account account = validateAndGet(userId, accountId);
        account.clearGoal();
    }

    private AccountRole resolveRole(AccountRequest request) {
        return request.role() != null ? request.role() : AccountRole.CHECKING;
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {

        Account account = validateAndGet(userId, accountId);

        List<FixedTransaction> fixedTransactions = fixedTransactionRepository.findAllByAccountId(accountId);
        fixedTransactions.forEach(fixedTransactionRepository::delete);

        transactionRepository.backfillSourceAccountSnapshot(accountId, account.getAccountName());
        transactionRepository.backfillTargetAccountSnapshot(accountId, account.getAccountName());
        transactionRepository.markSourceAccountArchived(accountId);
        transactionRepository.markTargetAccountArchived(accountId);

        accountRepository.delete(account);
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

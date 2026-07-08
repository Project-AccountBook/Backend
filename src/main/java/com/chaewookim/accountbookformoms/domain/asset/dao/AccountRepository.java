package com.chaewookim.accountbookformoms.domain.asset.dao;

import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    boolean existsByUserIdAndAccountName(Long userId, String accountName);

    boolean existsByUserIdAndAccountNameAndIdNot(Long userId, String accountName, Long id);

    @Query(value = """
            SELECT *
              FROM account
             WHERE user_id = :userId
               AND account_name = :accountName
             LIMIT 1
            """, nativeQuery = true)
    Optional<Account> findByUserIdAndAccountNameIncludingDeleted(@Param("userId") Long userId,
                                                                 @Param("accountName") String accountName);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}

package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseJoinResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPurchaseLockFacade {

    private final RedissonClient redissonClient;
    private final GroupPurchaseService groupPurchaseService;

    public GroupPurchaseJoinResponse joinGroupPurchase(Long userId, Long groupPurchaseId, com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseJoinRequest request) {
        String lockKey = "group_purchase_lock:" + groupPurchaseId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!available) {
                log.warn("공동구매 참여 락 획득 실패 (lockKey={})", lockKey);
                throw new CustomException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
            return groupPurchaseService.joinGroupPurchase(userId, groupPurchaseId, request);
        } catch (InterruptedException e) {
            log.error("공동구매 참여 락 대기 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public GroupPurchaseResponse leaveGroupPurchase(Long userId, Long groupPurchaseId) {
        String lockKey = "group_purchase_lock:" + groupPurchaseId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!available) {
                log.warn("공동구매 취소 락 획득 실패 (lockKey={})", lockKey);
                throw new CustomException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
            return groupPurchaseService.leaveGroupPurchase(userId, groupPurchaseId);
        } catch (InterruptedException e) {
            log.error("공동구매 취소 락 대기 중 인터럽트 발생", e);
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

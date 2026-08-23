package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPurchaseScheduler {

    private final GroupPurchaseRepository groupPurchaseRepository;
    private final GroupPurchaseService groupPurchaseService;

    /**
     * 매 분 0초마다 실행되어, 마감 기한이 지난 모집 중(RECRUITING) 상태의 공동구매를 실패(FAILED)로 변경합니다.
     * 이미 예약 결제 방식이므로 추가적인 환불(deduct취소) 로직은 필요 없습니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void closeExpiredGroupPurchases() {
        LocalDateTime now = LocalDateTime.now();
        List<GroupPurchase> expiredPurchases = groupPurchaseRepository.findExpiredGroupPurchases(PurchaseStatus.RECRUITING, now);

        if (!expiredPurchases.isEmpty()) {
            log.info("마감 기한이 경과된 공동구매 {}건을 처리합니다.", expiredPurchases.size());
            groupPurchaseService.processExpiredGroupPurchases(expiredPurchases);
        }
    }
}

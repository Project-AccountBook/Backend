package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.asset.application.TransactionService;
import com.chaewookim.accountbookformoms.domain.asset.dao.AccountRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.TransactionRequest;
import com.chaewookim.accountbookformoms.domain.asset.entity.Account;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseParticipantRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.ReportRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.WishlistRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchaseParticipant;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Wishlist;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseJoinResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseAdminResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.event.GroupPurchaseCreatedEvent;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPurchaseService {

    private final GroupPurchaseRepository groupPurchaseRepository;
    private final UserRepository userRepository;
    private final GroupPurchaseCategoryRepository groupPurchaseCategoryRepository;
    private final ReportRepository reportRepository;
    private final WishlistRepository wishlistRepository;
    private final GroupPurchaseParticipantRepository groupPurchaseParticipantRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    @Transactional
    public GroupPurchaseResponse createGroupPurchase(Long creatorId, GroupPurchaseCreateRequest request) {
        GroupPurchase groupPurchase = GroupPurchase.builder()
                .creatorId(creatorId)
                .categoryId(request.categoryId())
                .title(request.title())
                .content(request.content())
                .price(request.price())
                .minParticipants(request.minParticipants())
                .maxParticipants(request.maxParticipants())
                .deadline(request.deadline())
                .pickupLocation(request.pickupLocation())
                .imageUrl(request.imageUrl())
                .build();

        GroupPurchase saved = groupPurchaseRepository.save(groupPurchase);

        String categoryName = groupPurchaseCategoryRepository.findById(request.categoryId())
                .map(Category::getName)
                .orElse("기타");

        eventPublisher.publishEvent(new GroupPurchaseCreatedEvent(
                request.categoryId(),
                categoryName,
                "[" + categoryName + "] 새로운 공동구매가 시작되었습니다!",
                saved.getId()
        ));

        String creatorNickname = userRepository.findById(creatorId)
                .map(User::getUsername)
                .orElse("탈퇴한 사용자");

        return GroupPurchaseResponse.of(saved, creatorNickname);
    }

    @Transactional
    public GroupPurchaseResponse getGroupPurchase(Long id) {
        GroupPurchase groupPurchase = groupPurchaseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));
        groupPurchase.increaseViewCount();

        String creatorNickname = userRepository.findById(groupPurchase.getCreatorId())
                .map(User::getUsername)
                .orElse("탈퇴한 사용자");

        return GroupPurchaseResponse.of(groupPurchase, creatorNickname);
    }

    public List<GroupPurchaseResponse> getAllGroupPurchases(String region, Long categoryId, Boolean nearMe, Long currentUserId, String sortBy) {
        String filterRegion = (region != null && !region.trim().isEmpty()) ? region.trim() : null;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("deadline".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "deadline");
        }

        List<GroupPurchase> list = groupPurchaseRepository.findActiveGroupPurchases(PurchaseStatus.RECRUITING, filterRegion, categoryId, sort);

        if (Boolean.TRUE.equals(nearMe) && currentUserId != null) {
            User user = userRepository.findById(currentUserId).orElse(null);
            if (user != null) {
                if (user.getLatitude() != null && user.getLongitude() != null) {
                    list = list.stream()
                            .filter(gp -> gp.getLatitude() != null && gp.getLongitude() != null)
                            .filter(gp -> calculateDistance(user.getLatitude(), user.getLongitude(), gp.getLatitude(), gp.getLongitude()) <= 3.0)
                            .collect(Collectors.toList());
                } else if (user.getAddress() != null) {
                    String neighborhood = extractNeighborhood(user.getAddress());
                    if (neighborhood != null) {
                        list = list.stream()
                                .filter(gp -> gp.getPickupLocation() != null && gp.getPickupLocation().contains(neighborhood))
                                .collect(Collectors.toList());
                    }
                }
            }
        }

        List<Long> creatorIds = list.stream().map(GroupPurchase::getCreatorId).distinct().toList();
        Map<Long, String> nicknameMap = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return list.stream()
                .map(gp -> {
                    String nickname = nicknameMap.getOrDefault(gp.getCreatorId(), "탈퇴한 사용자");
                    return GroupPurchaseResponse.of(gp, nickname);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupPurchaseResponse updateGroupPurchase(Long id, Long currentUserId, GroupPurchaseUpdateRequest request) {
        GroupPurchase groupPurchase = groupPurchaseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));

        if (!groupPurchase.getCreatorId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_GROUP_PURCHASE);
        }

        groupPurchase.update(
                request.categoryId(),
                request.title(),
                request.content(),
                request.price(),
                request.minParticipants(),
                request.maxParticipants(),
                request.deadline(),
                request.pickupLocation(),
                request.imageUrl()
        );

        String creatorNickname = userRepository.findById(groupPurchase.getCreatorId())
                .map(User::getUsername)
                .orElse("탈퇴한 사용자");

        return GroupPurchaseResponse.of(groupPurchase, creatorNickname);
    }

    @Transactional
    public void deleteGroupPurchase(Long id, Long currentUserId) {
        GroupPurchase groupPurchase = groupPurchaseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));

        if (!groupPurchase.getCreatorId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_GROUP_PURCHASE);
        }

        groupPurchaseRepository.delete(groupPurchase);
    }

    private static final double EARTH_RADIUS_KM = 6371.0;

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String extractNeighborhood(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        String[] parts = address.split("\\s+");
        for (String part : parts) {
            if (part.endsWith("동") || part.endsWith("읍") || part.endsWith("면")) {
                return part;
            }
        }
        for (String part : parts) {
            if (part.endsWith("구")) {
                return part;
            }
        }
        return null;
    }

    public GroupPurchaseDashboardResponse getDashboardSummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long todayCreatedCount = groupPurchaseRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        long activeParticipantsCount = groupPurchaseRepository.sumCurrentParticipantsByStatus(PurchaseStatus.RECRUITING);

        long recruitingCount = groupPurchaseRepository.countByStatus(PurchaseStatus.RECRUITING);
        long successCount = groupPurchaseRepository.countByStatus(PurchaseStatus.SUCCESS)
                + groupPurchaseRepository.countByStatus(PurchaseStatus.CLOSED);
        long failedCount = groupPurchaseRepository.countByStatus(PurchaseStatus.FAILED);

        long total = recruitingCount + successCount + failedCount;
        double recruitingRatio = total == 0 ? 0.0 : Math.round(((double) recruitingCount / total * 100) * 100) / 100.0;
        double successRatio = total == 0 ? 0.0 : Math.round(((double) successCount / total * 100) * 100) / 100.0;
        double failedRatio = total == 0 ? 0.0 : Math.round(((double) failedCount / total * 100) * 100) / 100.0;

        return new GroupPurchaseDashboardResponse(
                todayCreatedCount,
                activeParticipantsCount,
                recruitingCount,
                successCount,
                failedCount,
                recruitingRatio,
                successRatio,
                failedRatio
        );
    }

    public Page<GroupPurchaseAdminResponse> getGroupPurchasesForAdmin(String status, Pageable pageable) {
        return groupPurchaseRepository.findAllForAdmin(status, pageable)
                .map(gp -> {
                    String creatorUsername = userRepository.findById(gp.getCreatorId())
                            .map(User::getUsername)
                            .orElse("탈퇴한 사용자");
                    String categoryName = groupPurchaseCategoryRepository.findById(gp.getCategoryId())
                            .map(Category::getName)
                            .orElse("미지정");
                    long reportCount = reportRepository.countByTargetTypeAndTargetId(
                            ReportTargetType.GROUP_PURCHASE,
                            gp.getId()
                        );
                    return GroupPurchaseAdminResponse.of(gp, creatorUsername, categoryName, reportCount);
                });
    }

    @Transactional
    public boolean toggleWish(Long userId, Long groupPurchaseId) {
        if (!groupPurchaseRepository.existsById(groupPurchaseId)) {
            throw new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND);
        }

        return wishlistRepository.findByUserIdAndGroupPurchaseId(userId, groupPurchaseId)
                .map(wish -> {
                    wishlistRepository.delete(wish);
                    return false;
                })
                .orElseGet(() -> {
                    Wishlist wish = Wishlist.builder()
                            .userId(userId)
                            .groupPurchaseId(groupPurchaseId)
                            .build();
                    wishlistRepository.save(wish);
                    return true;
                });
    }

    public Page<GroupPurchaseResponse> getWishedGroupPurchases(Long userId, Pageable pageable) {
        Page<GroupPurchase> page = groupPurchaseRepository.findWishedGroupPurchases(userId, pageable);

        List<Long> creatorIds = page.getContent().stream().map(GroupPurchase::getCreatorId).distinct().toList();
        Map<Long, String> nicknameMap = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return page.map(gp -> {
            String nickname = nicknameMap.getOrDefault(gp.getCreatorId(), "탈퇴한 사용자");
            return GroupPurchaseResponse.of(gp, nickname);
        });
    }

    @Transactional
    public GroupPurchaseJoinResponse joinGroupPurchase(Long userId, Long groupPurchaseId) {
        GroupPurchase groupPurchase = groupPurchaseRepository.findById(groupPurchaseId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));

        if (groupPurchase.getStatus() != PurchaseStatus.RECRUITING) {
            throw new CustomException(ErrorCode.GROUP_PURCHASE_NOT_RECRUITING);
        }

        if (groupPurchaseParticipantRepository.existsByGroupPurchaseIdAndUserId(groupPurchaseId, userId)) {
            throw new CustomException(ErrorCode.GROUP_PURCHASE_ALREADY_JOINED);
        }

        if (groupPurchase.getCurrentParticipants() >= groupPurchase.getMaxParticipants()) {
            throw new CustomException(ErrorCode.GROUP_PURCHASE_FULL);
        }

        GroupPurchaseParticipant participant = GroupPurchaseParticipant.builder()
                .groupPurchaseId(groupPurchaseId)
                .userId(userId)
                .build();
        groupPurchaseParticipantRepository.save(participant);

        groupPurchase.join();

        if (groupPurchase.getStatus() == PurchaseStatus.SUCCESS) {
            createAutoTransactionsForGroupPurchase(groupPurchase);
        }

        String creatorNickname = userRepository.findById(groupPurchase.getCreatorId())
                .map(User::getUsername)
                .orElse("탈퇴한 사용자");

        GroupPurchaseResponse groupPurchaseResponse = GroupPurchaseResponse.of(groupPurchase, creatorNickname);

        boolean budgetWarning = false;
        BigDecimal remainingBudget = BigDecimal.ZERO;

        String currentYearMonth = YearMonth.now().toString();
        List<com.chaewookim.accountbookformoms.domain.budget.entity.Budget> budgets =
                budgetRepository.findByUserIdAndYearMonth(userId, currentYearMonth);

        if (!budgets.isEmpty()) {
            BigDecimal totalPlannedBudgetSum = budgets.stream()
                    .map(budget -> budget.getTotalBudget().add(budget.getExpectedExpense()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LocalDate startDate = LocalDate.parse(currentYearMonth + "-01");
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            BigDecimal totalActualExpenseSum = transactionRepository.sumByUserAndType(userId, TransactionType.EXPENSE, startDate, endDate);
            if (totalActualExpenseSum == null) {
                totalActualExpenseSum = BigDecimal.ZERO;
            }

            remainingBudget = totalPlannedBudgetSum.subtract(totalActualExpenseSum);

            BigDecimal purchasePrice = BigDecimal.valueOf(groupPurchase.getPrice());
            if (remainingBudget.compareTo(purchasePrice) < 0) {
                budgetWarning = true;
            }
        }

        return new GroupPurchaseJoinResponse(groupPurchaseResponse, budgetWarning, remainingBudget);
    }

    @Transactional
    public GroupPurchaseResponse leaveGroupPurchase(Long userId, Long groupPurchaseId) {
        GroupPurchase groupPurchase = groupPurchaseRepository.findById(groupPurchaseId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));

        if (groupPurchase.getStatus() != PurchaseStatus.RECRUITING && groupPurchase.getStatus() != PurchaseStatus.SUCCESS) {
            throw new CustomException(ErrorCode.GROUP_PURCHASE_NOT_RECRUITING);
        }

        if (groupPurchase.getDeadline().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.GROUP_PURCHASE_NOT_RECRUITING);
        }

        GroupPurchaseParticipant participant = groupPurchaseParticipantRepository.findByGroupPurchaseIdAndUserId(groupPurchaseId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_JOINED));

        groupPurchaseParticipantRepository.delete(participant);

        groupPurchase.leave();

        String creatorNickname = userRepository.findById(groupPurchase.getCreatorId())
                .map(User::getUsername)
                .orElse("탈퇴한 사용자");

        return GroupPurchaseResponse.of(groupPurchase, creatorNickname);
    }

    private void createAutoTransactionsForGroupPurchase(GroupPurchase groupPurchase) {
        Set<Long> memberIds = new HashSet<>();
        memberIds.add(groupPurchase.getCreatorId());

        List<GroupPurchaseParticipant> participants =
                groupPurchaseParticipantRepository.findByGroupPurchaseId(groupPurchase.getId());
        for (GroupPurchaseParticipant participant : participants) {
            memberIds.add(participant.getUserId());
        }

        String categoryName = groupPurchaseCategoryRepository.findById(groupPurchase.getCategoryId())
                .map(Category::getName)
                .orElse("기타");

        BigDecimal amount = BigDecimal.valueOf(groupPurchase.getPrice());
        String description = "공동구매 지출: " + groupPurchase.getTitle();

        for (Long memberId : memberIds) {
            try {
                List<Account> accounts = accountRepository.findByUserId(memberId);
                if (accounts.isEmpty()) {
                    log.warn("가계부 자동 기입 실패: 사용자(ID={})의 자산 계좌가 존재하지 않습니다.", memberId);
                    continue;
                }
                Account account = accounts.get(0);

                List<TransactionCategory> userCategories = transactionCategoryRepository.findAllByUserOrSystem(memberId);

                TransactionCategory targetCategory = userCategories.stream()
                        .filter(c -> c.getType() == TransactionType.EXPENSE && c.getName().equals(categoryName))
                        .findFirst()
                        .orElseGet(() -> userCategories.stream()
                                .filter(c -> c.getType() == TransactionType.EXPENSE &&
                                        (c.getName().contains("기타") || c.getName().contains("공동구매")))
                                .findFirst()
                                .orElseGet(() -> userCategories.stream()
                                        .filter(c -> c.getType() == TransactionType.EXPENSE)
                                        .findFirst()
                                        .orElse(null)));

                if (targetCategory == null) {
                    log.warn("가계부 자동 기입 실패: 사용자(ID={})의 지출 카테고리가 존재하지 않습니다.", memberId);
                    continue;
                }

                TransactionRequest req = new TransactionRequest(
                        account.getId(),
                        null,
                        targetCategory.getId(),
                        TransactionType.EXPENSE,
                        amount,
                        LocalDate.now(),
                        description
                );

                transactionService.createTransaction(memberId, req);
            } catch (Exception e) {
                log.warn("가계부 자동 기입 실패: 사용자(ID={})의 지출 생성 중 예외가 발생했습니다. 메시지: {}", memberId, e.getMessage());
            }
        }
    }
}

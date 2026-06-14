package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
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
import java.math.BigDecimal;
import java.time.YearMonth;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                    // 1. 위경도 기반 거리 필터링 (3km 이내)
                    list = list.stream()
                            .filter(gp -> gp.getLatitude() != null && gp.getLongitude() != null)
                            .filter(gp -> calculateDistance(user.getLatitude(), user.getLongitude(), gp.getLatitude(), gp.getLongitude()) <= 3.0)
                            .collect(Collectors.toList());
                } else if (user.getAddress() != null) {
                    // 2. 동네/구 텍스트 매칭 필터링
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
}

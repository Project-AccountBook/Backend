package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.ReportRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.WishlistRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Wishlist;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.ReportTargetType;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseDashboardResponse;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseAdminResponse;
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
                .build();

        GroupPurchase saved = groupPurchaseRepository.save(groupPurchase);
        return GroupPurchaseResponse.from(saved);
    }

    public GroupPurchaseResponse getGroupPurchase(Long id) {
        GroupPurchase groupPurchase = groupPurchaseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));
        return GroupPurchaseResponse.from(groupPurchase);
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

        return list.stream()
                .map(GroupPurchaseResponse::from)
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
                request.pickupLocation()
        );

        return GroupPurchaseResponse.from(groupPurchase);
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
        return groupPurchaseRepository.findWishedGroupPurchases(userId, pageable)
                .map(GroupPurchaseResponse::from);
    }
}

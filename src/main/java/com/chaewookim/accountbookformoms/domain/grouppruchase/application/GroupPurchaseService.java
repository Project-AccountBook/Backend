package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.enums.PurchaseStatus;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.domain.user.dao.UserRepository;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
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
}

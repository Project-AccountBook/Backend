package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.GroupPurchase;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseResponse;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupPurchaseService {

    private final GroupPurchaseRepository groupPurchaseRepository;

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

    public List<GroupPurchaseResponse> getAllGroupPurchases() {
        return groupPurchaseRepository.findAll().stream()
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
}

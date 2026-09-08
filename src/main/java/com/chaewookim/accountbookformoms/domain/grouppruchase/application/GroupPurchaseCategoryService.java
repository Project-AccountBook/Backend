package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCategoryCreateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.request.GroupPurchaseCategoryUpdateRequest;
import com.chaewookim.accountbookformoms.domain.grouppruchase.dto.response.GroupPurchaseCategoryResponse;
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
public class GroupPurchaseCategoryService {

    private final GroupPurchaseCategoryRepository categoryRepository;

    @Transactional
    public GroupPurchaseCategoryResponse createCategory(GroupPurchaseCategoryCreateRequest request) {
        Category category = Category.builder()
                .name(request.name())
                .sortOrder(request.sortOrder())
                .description(request.description())
                .build();

        Category saved = categoryRepository.save(category);
        return GroupPurchaseCategoryResponse.from(saved);
    }

    public GroupPurchaseCategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        return GroupPurchaseCategoryResponse.from(category);
    }

    public List<GroupPurchaseCategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(GroupPurchaseCategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupPurchaseCategoryResponse updateCategory(Long id, GroupPurchaseCategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        category.update(request.name(), request.sortOrder(), request.description());
        return GroupPurchaseCategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(category);
    }
}

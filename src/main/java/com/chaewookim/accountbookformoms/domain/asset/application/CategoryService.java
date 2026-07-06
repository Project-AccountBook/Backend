package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.CategoryRequest;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.CategoryResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final TransactionCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final FixedTransactionRepository fixedTransactionRepository;
    private final BudgetRepository budgetRepository;

    public List<CategoryResponse> getCategories(Long userId) {
        return categoryRepository.findAllByUserOrSystem(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCustomCategory(User user, CategoryRequest request) {
        TransactionCategory savedCategory = categoryRepository.save(request.toEntity(user));
        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public void updateCategory(Long categoryId, Long userId, CategoryRequest request) {
        TransactionCategory category = validateAndGetCategory(categoryId, userId);
        category.update(request.name(), request.type());
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        TransactionCategory category = validateAndGetCategory(categoryId, userId);
        validateCategoryNotInUse(categoryId);
        categoryRepository.delete(category);
    }

    private void validateCategoryNotInUse(Long categoryId) {
        if (transactionRepository.existsByTransactionCategoryId(categoryId)
                || fixedTransactionRepository.existsByTransactionCategoryId(categoryId)
                || budgetRepository.existsByTransactionCategoryId(categoryId)) {
            throw new CustomException(AssetErrorCode.CATEGORY_IN_USE);
        }
    }

    // 공통 검증 로직
    private TransactionCategory validateAndGetCategory(Long categoryId, Long userId) {

        TransactionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        if (category.getUser() == null) {
            throw new CustomException(AssetErrorCode.CATEGORY_IMMUTABLE);
        }

        if (!category.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.CATEGORY_FORBIDDEN);
        }

        return category;
    }
}

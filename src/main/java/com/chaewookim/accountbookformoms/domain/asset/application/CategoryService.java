package com.chaewookim.accountbookformoms.domain.asset.application;

import com.chaewookim.accountbookformoms.domain.asset.dao.FixedTransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.CategoryAllocationRequest;
import com.chaewookim.accountbookformoms.domain.asset.dto.request.CategoryRequest;
import com.chaewookim.accountbookformoms.domain.budget.dao.BudgetRepository;
import com.chaewookim.accountbookformoms.domain.asset.dto.response.CategoryResponse;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import com.chaewookim.accountbookformoms.domain.asset.error.AssetErrorCode;
import com.chaewookim.accountbookformoms.domain.user.entity.User;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
        String name = request.name().trim();
        Long userId = user.getId();

        Optional<TransactionCategory> deletedCategory = categoryRepository
                .findByUserIdAndNameAndTypeIncludingDeleted(userId, name, request.type().name());
        if (deletedCategory.isPresent() && deletedCategory.get().getDeletedAt() != null) {
            TransactionCategory category = deletedCategory.get();
            category.restore();
            category.update(name, request.type(), request.includeInSavingsRate(), request.includeInInvestmentRate());
            return CategoryResponse.from(categoryRepository.save(category));
        }

        validateCategoryNameUnique(userId, name, request.type(), null);

        TransactionCategory savedCategory = categoryRepository.save(
                TransactionCategory.builder()
                        .user(user)
                        .name(name)
                        .type(request.type())
                        .includeInSavingsRate(request.includeInSavingsRate())
                        .includeInInvestmentRate(request.includeInInvestmentRate())
                        .build()
        );
        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public void updateCategory(Long categoryId, Long userId, CategoryRequest request) {
        TransactionCategory category = validateAndGetCategory(categoryId, userId);
        String name = request.name().trim();
        validateCategoryNameUnique(userId, name, request.type(), categoryId);
        category.update(name, request.type(), request.includeInSavingsRate(), request.includeInInvestmentRate());
    }

    @Transactional
    public void updateCategoryAllocation(Long categoryId, Long userId, CategoryAllocationRequest request) {
        TransactionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(AssetErrorCode.CATEGORY_NOT_FOUND));

        if (category.getType() != TransactionType.TRANSFER) {
            throw new CustomException(AssetErrorCode.CATEGORY_ALLOCATION_ONLY_TRANSFER);
        }

        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new CustomException(AssetErrorCode.CATEGORY_FORBIDDEN);
        }

        category.updateAllocationFlags(request.includeInSavingsRate(), request.includeInInvestmentRate());
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

    private void validateCategoryNameUnique(Long userId, String name, TransactionType type, Long excludeId) {
        if (categoryRepository.existsByUserIsNullAndNameAndType(name, type)) {
            throw new CustomException(AssetErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        boolean duplicate = excludeId == null
                ? categoryRepository.existsByUserIdAndNameAndType(userId, name, type)
                : categoryRepository.existsByUserIdAndNameAndTypeAndIdNot(userId, name, type, excludeId);

        if (duplicate) {
            throw new CustomException(AssetErrorCode.DUPLICATE_CATEGORY_NAME);
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

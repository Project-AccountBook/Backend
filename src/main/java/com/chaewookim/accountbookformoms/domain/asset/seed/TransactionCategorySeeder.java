package com.chaewookim.accountbookformoms.domain.asset.seed;

import com.chaewookim.accountbookformoms.domain.asset.dao.TransactionCategoryRepository;
import com.chaewookim.accountbookformoms.domain.asset.entity.TransactionCategory;
import com.chaewookim.accountbookformoms.domain.asset.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionCategorySeeder {

    private static final List<String> EXPENSE_CATEGORIES = List.of(
            "음식", "의류", "주거", "생활", "건강", "교통", "통신", "교육"
    );

    private static final List<String> INCOME_CATEGORIES = List.of(
            "급여", "사업", "투자", "용돈"
    );

    private static final List<String> TRANSFER_CATEGORIES = List.of(
            "적금", "비상금"
    );

    @Bean
    public ApplicationRunner seedTransactionCategories(TransactionCategoryRepository repository) {
        return args -> {
            seed(repository, TransactionType.EXPENSE, EXPENSE_CATEGORIES);
            seed(repository, TransactionType.INCOME, INCOME_CATEGORIES);
            seed(repository, TransactionType.TRANSFER, TRANSFER_CATEGORIES);
        };
    }

    private void seed(TransactionCategoryRepository repository, TransactionType type, List<String> names) {
        for (String name : names) {
            if (repository.existsByUserIsNullAndNameAndType(name, type)) {
                continue;
            }
            repository.save(TransactionCategory.builder()
                    .user(null)
                    .name(name)
                    .type(type)
                    .build());
            log.info("Seeded TransactionCategory type={}, name={}", type, name);
        }
    }
}

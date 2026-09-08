package com.chaewookim.accountbookformoms.domain.grouppruchase.application;

import com.chaewookim.accountbookformoms.domain.grouppruchase.dao.GroupPurchaseCategoryRepository;
import com.chaewookim.accountbookformoms.domain.grouppruchase.domain.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPurchaseDataInitializer implements CommandLineRunner {

    private final GroupPurchaseCategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            log.info("공동구매 카테고리가 비어 있어 기본 카테고리를 등록합니다.");
            categoryRepository.saveAll(List.of(
                    Category.builder().name("식품").sortOrder(1).build(),
                    Category.builder().name("생활용품").sortOrder(2).build(),
                    Category.builder().name("육아용품").sortOrder(3).build(),
                    Category.builder().name("가전").sortOrder(4).build()
            ));
            log.info("공동구매 기본 카테고리 등록 완료");
        }
    }

}

package com.chaewookim.accountbookformoms.domain.boardcategory.seed;

import com.chaewookim.accountbookformoms.domain.board.enums.BOARD_TYPE;
import com.chaewookim.accountbookformoms.domain.boardcategory.dao.BoardCategoryRepository;
import com.chaewookim.accountbookformoms.domain.boardcategory.entity.BoardCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BoardCategorySeeder {

    private static final List<String> QNA_CATEGORIES = List.of(
            "재테크", "가계부", "육아 비용", "절약", "공동구매", "살림"
    );

    private static final List<String> KNOWHOW_CATEGORIES = List.of(
            "절약 노하우", "재테크", "가계부", "육아 꿀팁", "공동구매", "살림 팁"
    );

    @Bean
    public ApplicationRunner seedBoardCategories(BoardCategoryRepository repository) {
        return args -> {
            seed(repository, BOARD_TYPE.QNA, QNA_CATEGORIES);
            seed(repository, BOARD_TYPE.KNOWHOW, KNOWHOW_CATEGORIES);
        };
    }

    private void seed(BoardCategoryRepository repo, BOARD_TYPE type, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (repo.existsByBoardTypeAndName(type, name)) continue;
            repo.save(BoardCategory.builder()
                    .name(name)
                    .boardType(type)
                    .displayOrder(i)
                    .build());
            log.info("Seeded BoardCategory type={}, name={}", type, name);
        }
    }
}

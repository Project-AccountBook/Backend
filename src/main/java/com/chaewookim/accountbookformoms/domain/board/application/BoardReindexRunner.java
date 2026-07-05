package com.chaewookim.accountbookformoms.domain.board.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 앱 부팅 시 자동 재색인.
 * <p>{@code app.board.reindex-on-startup=true} 일 때만 실행. 로컬/배포 직후 스키마 변경 대응용.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.board.reindex-on-startup", havingValue = "true")
public class BoardReindexRunner {

    @Bean
    public ApplicationRunner boardReindexOnStartup(BoardReindexService reindexService) {
        return args -> {
            log.info("app.board.reindex-on-startup=true → Board ES 전체 재색인 시작");
            long count = reindexService.reindexAll();
            log.info("Board ES 부팅 시 재색인 완료: {} 문서", count);
        };
    }
}

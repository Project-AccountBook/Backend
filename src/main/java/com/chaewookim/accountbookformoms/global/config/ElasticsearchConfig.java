package com.chaewookim.accountbookformoms.global.config;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackageClasses = BoardSearchRepository.class)
public class ElasticsearchConfig {
}

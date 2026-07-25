package com.chaewookim.accountbookformoms;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class AccountBookForMomsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountBookForMomsApplication.class, args);
    }

}

package com.chaewookim.accountbookformoms;

import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchQueryRepository;
import com.chaewookim.accountbookformoms.domain.board.dao.BoardSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AccountBookForMomsApplicationTests {

    @MockBean
    private BoardSearchRepository boardSearchRepository;

    @MockBean
    private BoardSearchQueryRepository boardSearchQueryRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
    }

}

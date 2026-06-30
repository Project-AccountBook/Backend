package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailVerificationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(emailVerificationService, "mailFrom", "test@example.com");
    }

    @Test
    @DisplayName("인증번호 발송 - 성공")
    void sendVerificationCode_success() {

        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.hasKey(anyString())).willReturn(false);

        // when
        emailVerificationService.sendVerificationCode("test@email.com", VerificationType.SIGNUP);

        // then
        verify(valueOperations, times(2)).set(anyString(), anyString(), any());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("인증번호 검증 - 성공")
    void verifyCode_success() {

        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("123456");

        // when
        boolean result = emailVerificationService.verifyCode("test@email.com", "123456", VerificationType.SIGNUP);

        // then
        assertThat(result).isTrue();
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("인증번호 검증 - 실패(코드 불일치)")
    void verifyCode_fail() {

        // given
        given(valueOperations.get(anyString())).willReturn("654321");

        // when
        boolean result = emailVerificationService.verifyCode("test@email.com", "123456", VerificationType.SIGNUP);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 확인 - 성공")
    void isVerified_success() {

        // given
        given(redisTemplate.hasKey(anyString())).willReturn(true);

        // when
        boolean result = emailVerificationService.isVerified("test@email.com", VerificationType.SIGNUP);

        // then
        assertThat(result).isTrue();
    }
}
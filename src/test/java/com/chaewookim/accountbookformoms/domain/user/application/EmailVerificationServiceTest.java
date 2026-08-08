package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.mail.AsyncVerificationEmailSender;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailVerificationServiceTest {

    private static final String CLIENT_IP = "203.0.113.10";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AsyncVerificationEmailSender asyncVerificationEmailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("인증번호 발송 - 성공")
    void sendVerificationCode_success() {
        given(redisTemplate.hasKey(startsWith("LOCK:"))).willReturn(false);
        given(valueOperations.get(startsWith("RL:EMAIL:"))).willReturn(null);
        given(valueOperations.get(startsWith("RL:IP:"))).willReturn(null);
        given(valueOperations.increment(startsWith("RL:"))).willReturn(1L);

        emailVerificationService.sendVerificationCode("test@email.com", VerificationType.SIGNUP, CLIENT_IP);

        verify(valueOperations).set(startsWith("VERIFY:"), anyString(), eq(Duration.ofMinutes(3)));
        verify(valueOperations).set(startsWith("LOCK:"), eq("locked"), eq(Duration.ofMinutes(1)));
        verify(valueOperations, times(2)).increment(startsWith("RL:"));
        verify(redisTemplate, times(2)).expire(startsWith("RL:"), eq(Duration.ofDays(2)));
        verify(asyncVerificationEmailSender).send(eq("test@email.com"), anyString(), eq(VerificationType.SIGNUP));
    }

    @Test
    @DisplayName("인증번호 발송 - 쿨다운 중이면 거부")
    void sendVerificationCode_rejected_when_cooldown() {
        given(redisTemplate.hasKey(startsWith("LOCK:"))).willReturn(true);

        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationCode("test@email.com", VerificationType.SIGNUP, CLIENT_IP)
        ).isInstanceOf(CustomException.class);

        verify(asyncVerificationEmailSender, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("인증번호 발송 - 이메일 일일 한도 초과 시 거부")
    void sendVerificationCode_rejected_when_email_daily_limit() {
        given(redisTemplate.hasKey(startsWith("LOCK:"))).willReturn(false);
        given(valueOperations.get(startsWith("RL:EMAIL:"))).willReturn("5");

        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationCode("test@email.com", VerificationType.SIGNUP, CLIENT_IP)
        ).isInstanceOf(CustomException.class);

        verify(asyncVerificationEmailSender, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("인증번호 발송 - IP 일일 한도 초과 시 거부")
    void sendVerificationCode_rejected_when_ip_daily_limit() {
        given(redisTemplate.hasKey(startsWith("LOCK:"))).willReturn(false);
        given(valueOperations.get(startsWith("RL:EMAIL:"))).willReturn("1");
        given(valueOperations.get(startsWith("RL:IP:"))).willReturn("20");

        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationCode("test@email.com", VerificationType.SIGNUP, CLIENT_IP)
        ).isInstanceOf(CustomException.class);

        verify(asyncVerificationEmailSender, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("인증번호 검증 - 성공")
    void verifyCode_success() {
        given(valueOperations.get(anyString())).willReturn("123456");

        boolean result = emailVerificationService.verifyCode("test@email.com", "123456", VerificationType.SIGNUP);

        assertThat(result).isTrue();
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("인증번호 검증 - 실패(코드 불일치)")
    void verifyCode_fail() {
        given(valueOperations.get(anyString())).willReturn("654321");

        boolean result = emailVerificationService.verifyCode("test@email.com", "123456", VerificationType.SIGNUP);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 확인 - 성공")
    void isVerified_success() {
        given(redisTemplate.hasKey(anyString())).willReturn(true);

        boolean result = emailVerificationService.isVerified("test@email.com", VerificationType.SIGNUP);

        assertThat(result).isTrue();
    }
}

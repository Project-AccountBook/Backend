package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.mail.AsyncVerificationEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AsyncVerificationEmailSender asyncVerificationEmailSender;

    // 인증 번호 발송
    public void sendVerificationCode(String email, VerificationType type) {

        String lockKey = "LOCK:" + type + ":" + email;
        if (redisTemplate.hasKey(lockKey)) {
            throw new CustomException(UserErrorCode.TOO_MANY_REQUESTS);
        }

        String code = generateRandomCode();
        redisTemplate.opsForValue().set("VERIFY:" + type + ":" + email, code, Duration.ofMinutes(3));
        redisTemplate.opsForValue().set(lockKey, "locked", Duration.ofMinutes(1));

        asyncVerificationEmailSender.send(email, code, type);
        log.info("인증 메일 발송 요청 - email: {}, type: {}", email, type);
    }

    // 인증 번호 검증
    public boolean verifyCode(String email, String code, VerificationType type) {

        String verifyKey = "VERIFY:" + type + ":" + email;
        String savedCode = redisTemplate.opsForValue().get(verifyKey);

        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.opsForValue().set("VERIFIED:" + type + ":" + email, "true", Duration.ofMinutes(10));
            redisTemplate.delete(verifyKey);
            return true;
        }
        return false;
    }

    // 인증 확인
    public boolean isVerified(String email, VerificationType type) {
        return redisTemplate.hasKey("VERIFIED:" + type + ":" + email);
    }

    // 데이터 삭제
    public void deleteVerification(String email, VerificationType type) {
        redisTemplate.delete("VERIFIED:" + type + ":" + email);
    }

    private String generateRandomCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}

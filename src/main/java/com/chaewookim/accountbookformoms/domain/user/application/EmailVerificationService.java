package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.mail.AsyncVerificationEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final int MAX_PER_EMAIL_PER_DAY = 5;
    private static final int MAX_PER_IP_PER_DAY = 20;

    private final RedisTemplate<String, String> redisTemplate;
    private final AsyncVerificationEmailSender asyncVerificationEmailSender;

    @Value("${app.mail.dev-log-code:false}")
    private boolean devLogCode;

    public void sendVerificationCode(String email, VerificationType type, String clientIp) {
        String day = LocalDate.now(KST).format(DAY_FORMAT);
        String lockKey = lockKey(type, email);
        String emailCountKey = emailCountKey(type, email, day);
        String ipCountKey = ipCountKey(type, clientIp, day);

        if (redisTemplate.hasKey(lockKey)) {
            throw new CustomException(UserErrorCode.TOO_MANY_REQUESTS);
        }
        if (currentCount(emailCountKey) >= MAX_PER_EMAIL_PER_DAY) {
            throw new CustomException(UserErrorCode.TOO_MANY_REQUESTS);
        }
        if (currentCount(ipCountKey) >= MAX_PER_IP_PER_DAY) {
            throw new CustomException(UserErrorCode.TOO_MANY_REQUESTS);
        }

        String code = generateRandomCode();
        redisTemplate.opsForValue().set(verifyKey(type, email), code, CODE_TTL);
        redisTemplate.opsForValue().set(lockKey, "locked", COOLDOWN);
        incrementDailyCount(emailCountKey);
        incrementDailyCount(ipCountKey);

        if (devLogCode) {
            log.warn("[DEV] 인증번호 - email: {}, type: {}, code: {}", email, type, code);
        }

        asyncVerificationEmailSender.send(email, code, type);
        log.info("인증 메일 발송 요청 - email: {}, type: {}, ip: {}", email, type, clientIp);
    }

    public boolean verifyCode(String email, String code, VerificationType type) {
        String key = verifyKey(type, email);
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.opsForValue().set(verifiedKey(type, email), "true", VERIFIED_TTL);
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public boolean isVerified(String email, VerificationType type) {
        return redisTemplate.hasKey(verifiedKey(type, email));
    }

    public void deleteVerification(String email, VerificationType type) {
        redisTemplate.delete(verifiedKey(type, email));
    }

    private long currentCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void incrementDailyCount(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2));
        }
    }

    private static String lockKey(VerificationType type, String email) {
        return "LOCK:" + type + ":" + email;
    }

    private static String verifyKey(VerificationType type, String email) {
        return "VERIFY:" + type + ":" + email;
    }

    private static String verifiedKey(VerificationType type, String email) {
        return "VERIFIED:" + type + ":" + email;
    }

    private static String emailCountKey(VerificationType type, String email, String day) {
        return "RL:EMAIL:" + type + ":" + email + ":" + day;
    }

    private static String ipCountKey(VerificationType type, String ip, String day) {
        return "RL:IP:" + type + ":" + ip + ":" + day;
    }

    private String generateRandomCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}

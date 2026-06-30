package com.chaewookim.accountbookformoms.domain.user.application;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import com.chaewookim.accountbookformoms.domain.user.error.UserErrorCode;
import com.chaewookim.accountbookformoms.global.error.CustomException;
import com.chaewookim.accountbookformoms.global.mail.VerificationEmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    // 인증 번호 발송
    public void sendVerificationCode(String email, VerificationType type) {

        String lockKey = "LOCK:" + type + ":" + email;
        if (redisTemplate.hasKey(lockKey)) {
            throw new CustomException(UserErrorCode.TOO_MANY_REQUESTS);
        }

        String code = generateRandomCode();
        redisTemplate.opsForValue().set("VERIFY:" + type + ":" + email, code, Duration.ofMinutes(3));
        redisTemplate.opsForValue().set(lockKey, "locked", Duration.ofMinutes(1));

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email);
            helper.setFrom(mailFrom, VerificationEmailTemplate.SENDER_NAME);
            helper.setSubject(VerificationEmailTemplate.getSubject(type));
            helper.setText(
                    VerificationEmailTemplate.buildPlainText(code, type),
                    VerificationEmailTemplate.buildHtml(code, type)
            );
            mailSender.send(mimeMessage);
            log.info("인증 메일 발송 완료 - email: {}, type: {}", email, type);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            redisTemplate.delete("VERIFY:" + type + ":" + email);
            redisTemplate.delete(lockKey);
            log.error("인증 메일 발송 실패 - email: {}, type: {}", email, type, e);
            throw new CustomException(UserErrorCode.EMAIL_SEND_FAILED);
        }
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

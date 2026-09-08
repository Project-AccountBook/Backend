package com.chaewookim.accountbookformoms.global.mail;

import com.chaewookim.accountbookformoms.domain.user.enums.VerificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncVerificationEmailSender {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Async
    public void send(String email, String code, VerificationType type) {
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
            redisTemplate.delete("LOCK:" + type + ":" + email);
            log.error("인증 메일 발송 실패 - email: {}, type: {}, from: {}, reason: {} (Redis 롤백 완료)", email, type, mailFrom, e.getMessage(), e);
        }
    }
}

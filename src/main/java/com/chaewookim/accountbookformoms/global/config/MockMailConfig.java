//package com.chaewookim.accountbookformoms.global.config;
//
//import jakarta.mail.internet.MimeMessage;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessagePreparator;
//
//import java.io.InputStream;
//import java.util.Properties;
//
//@Configuration
//public class MockMailConfig {
//
//    @Bean
//    @ConditionalOnMissingBean(JavaMailSender.class)
//    public JavaMailSender javaMailSender() {
//        return new JavaMailSender() {
//            @Override
//            public MimeMessage createMimeMessage() {
//                return new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
//            }
//
//            @Override
//            public MimeMessage createMimeMessage(InputStream contentStream) {
//                try {
//                    return new MimeMessage(jakarta.mail.Session.getInstance(new Properties()), contentStream);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            @Override
//            public void send(MimeMessage mimeMessage) {
//                // Mock send
//            }
//
//            @Override
//            public void send(MimeMessage... mimeMessages) {
//                // Mock send
//            }
//
//            @Override
//            public void send(MimeMessagePreparator mimeMessagePreparator) {
//                // Mock send
//            }
//
//            @Override
//            public void send(SimpleMailMessage simpleMessage) {
//                // Mock send
//            }
//
//            @Override
//            public void send(SimpleMailMessage... simpleMessages) {
//                // Mock send
//            }
//
//            @Override
//            public void send(MimeMessagePreparator... mimeMessagePreparators) {
//                // Mock send
//            }
//        };
//    }
//}

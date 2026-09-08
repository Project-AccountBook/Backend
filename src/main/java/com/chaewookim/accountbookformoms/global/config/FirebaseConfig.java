package com.chaewookim.accountbookformoms.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-path:classpath:jointliving-notification.json}")
    private Resource serviceAccount;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            if (!serviceAccount.exists()) {
                log.warn("Firebase credentials not found at [{}]. Push notifications will be disabled.", serviceAccount);
                return;
            }

            try (InputStream inputStream = serviceAccount.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized from [{}]", serviceAccount);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase App from [{}]: {}", serviceAccount, e.getMessage());
        }
    }
}

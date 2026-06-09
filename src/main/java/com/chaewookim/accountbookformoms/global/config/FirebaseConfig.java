package com.chaewookim.accountbookformoms.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("classpath:jointliving-notification.json")
    private Resource serviceAccount;

    @PostConstruct
    public void init() {
        try {
            if (serviceAccount.exists()) {
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                            .build();

                    FirebaseApp.initializeApp(options);
                }
            } else {
                System.out.println("WARN: Firebase service account file [jointliving-notification.json] not found. Push notifications will be disabled.");
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to initialize Firebase App: " + e.getMessage());
        }
    }
}

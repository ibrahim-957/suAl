package com.delivery.SuAl.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_CREDENTIALS_JSON:#{null}}")
    private String credentialsJson;

    @PostConstruct
    public void initialize() throws IOException {
        GoogleCredentials credentials;

        if (credentialsJson != null && !credentialsJson.isEmpty()) {
            System.out.println("Loading Firebase credentials from environment variable");
            InputStream stream = new ByteArrayInputStream(
                    credentialsJson.getBytes(StandardCharsets.UTF_8)
            );
            credentials = GoogleCredentials.fromStream(stream);
        } else {
            System.out.println("Loading Firebase credentials from local file");
            String localPath = "src/main/resources/firebase-service-account.json";
            File credentialsFile = new File(localPath);

            if (!credentialsFile.exists()) {
                throw new IllegalStateException(
                        "Firebase credentials not found. " +
                                "For local development, place firebase-service-account.json in src/main/resources/. " +
                                "For Render, set FIREBASE_CREDENTIALS_JSON environment variable."
                );
            }

            FileInputStream serviceAccount = new FileInputStream(localPath);
            credentials = GoogleCredentials.fromStream(serviceAccount);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        System.out.println("Firebase initialized successfully");
    }
}

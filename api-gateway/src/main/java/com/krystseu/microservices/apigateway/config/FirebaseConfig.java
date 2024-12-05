package com.krystseu.microservices.apigateway.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service.account.path}")
    private String serviceAccountPath;

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        // Resolve the path to the service account file
        FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        // Initialize FirebaseApp if it hasn't been initialized
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirebaseAuth.getInstance();
    }
}

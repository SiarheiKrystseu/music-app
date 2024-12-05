package com.krystseu.microservices.resourceprocessor.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FirebaseAuthUtils {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthUtils(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebaseToken verifyToken(String token) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(token);
    }

    public void setAuthentication(FirebaseToken firebaseToken, String rawToken) {
        Authentication authentication = new FirebaseAuthentication(firebaseToken, rawToken);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        log.info("Authentication set for user: {} with roles: {}",
                firebaseToken.getUid(),
                authentication.getAuthorities());
    }

    public String getAuthTokenFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof FirebaseAuthentication firebaseAuth) {
            return firebaseAuth.getRawToken();
        }
        log.warn("Authorization token not found in the security context.");
        return "";
    }
}


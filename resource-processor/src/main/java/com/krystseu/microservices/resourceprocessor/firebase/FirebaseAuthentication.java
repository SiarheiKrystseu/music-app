package com.krystseu.microservices.resourceprocessor.firebase;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class FirebaseAuthentication implements Authentication {

    private final FirebaseToken firebaseToken;
    private boolean authenticated = true;

    public FirebaseAuthentication(FirebaseToken firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Example role assignment. Adjust according to your needs.
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Object getCredentials() {
        // FirebaseToken doesn't have a getToken() method. Use uid or email as credentials.
        return firebaseToken.getUid(); // or use firebaseToken.getEmail()
    }

    @Override
    public Object getDetails() {
        return firebaseToken;
    }

    @Override
    public Object getPrincipal() {
        return firebaseToken.getUid();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        // FirebaseToken has no direct method for name; you may return email or UID
        return firebaseToken.getEmail(); // or firebaseToken.getUid()
    }
}





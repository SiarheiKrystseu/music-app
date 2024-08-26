package com.krystseu.microservices.resourceprocessor.firebase;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FirebaseAuthentication implements Authentication {

    private final FirebaseToken firebaseToken;
    private boolean authenticated = true;
    private final String rawToken;

    public FirebaseAuthentication(FirebaseToken firebaseToken, String rawToken) {
        this.firebaseToken = firebaseToken;
        this.rawToken = rawToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert Firebase roles to Spring Security roles
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = (List<String>) firebaseToken.getClaims().get("roles");

        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
        }

        return authorities;
    }

    @Override
    public Object getCredentials() {
        return firebaseToken;
    }

    @Override
    public Object getDetails() {
        return null;
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
        return firebaseToken.getUid();
    }

    public String getRawToken() {
        return rawToken;
    }
}





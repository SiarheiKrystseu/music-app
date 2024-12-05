package com.krystseu.microservices.resourceprocessor.firebase;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class HeaderAuthorizationFilter extends UsernamePasswordAuthenticationFilter {

    private final FirebaseAuthUtils firebaseAuthUtils;

    public HeaderAuthorizationFilter(FirebaseAuthUtils firebaseAuthUtils) {
        this.firebaseAuthUtils = firebaseAuthUtils;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                FirebaseToken decodedToken = firebaseAuthUtils.verifyToken(token);
                firebaseAuthUtils.setAuthentication(decodedToken, token);
            } catch (FirebaseAuthException e) {
                log.error("FirebaseAuthException: {}", e.getMessage(), e);
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (Exception e) {
                log.error("Unexpected error: {}", e.getMessage(), e);
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
package com.krystseu.microservices.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.krystseu.microservices.apigateway.authentication.FirebaseAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import java.util.List;
import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class FirebaseAuthenticationFilter implements WebFilter {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No Authorization header found or invalid header format");
            return handleUnauthorized(exchange);
        }

        String idToken = authHeader.substring(7);
        log.debug("Extracted ID Token: {}", idToken);

        return Mono.fromCallable(() -> verifyToken(idToken))
                .flatMap(decodedToken -> processValidToken(exchange, chain, decodedToken))
                .onErrorResume(FirebaseAuthException.class, e -> handleFirebaseAuthException(exchange, e))
                .onErrorResume(Exception.class, e -> handleUnexpectedException(exchange, e));
    }

    private FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(idToken);
    }

    private Mono<Void> processValidToken(ServerWebExchange exchange, WebFilterChain chain, FirebaseToken decodedToken) {
        log.debug("Successfully verified ID Token for UID: {}", decodedToken.getUid());

        // Extract custom claims (e.g., roles)
        List<String> roles = extractRolesFromClaims(decodedToken.getClaims());
        log.debug("Extracted roles from token: {}", roles);

        // Convert roles list to a comma-separated string
        String rolesHeader = roles != null ? String.join(",", roles) : "";

        // Add roles and user ID to request headers
        ServerHttpRequest requestWithRoles = exchange.getRequest().mutate()
                .header("X-User-Roles", rolesHeader)
                .header("X-User-ID", decodedToken.getUid())
                .build();

        // Create a security context with the decoded token
        Authentication authentication = new FirebaseAuthentication(decodedToken);
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        log.debug("Authentication created for UID: {}", decodedToken.getUid());

        return chain.filter(exchange.mutate().request(requestWithRoles).build())
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }

    private List<String> extractRolesFromClaims(Map<String, Object> claims) {
        if (claims == null) {
            return Collections.emptyList();
        }

        // Safely extract roles from claims
        Object rolesObject = claims.get("roles");
        if (rolesObject instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) rolesObject;
            return roles;
        }
        return Collections.emptyList();
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> handleFirebaseAuthException(ServerWebExchange exchange, FirebaseAuthException e) {
        log.error("FirebaseAuthException occurred", e);
        return handleUnauthorized(exchange);
    }

    private Mono<Void> handleUnexpectedException(ServerWebExchange exchange, Exception e) {
        log.error("Unexpected error occurred", e);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return exchange.getResponse().setComplete();
    }
}



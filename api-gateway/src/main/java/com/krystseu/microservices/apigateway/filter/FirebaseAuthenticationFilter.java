package com.krystseu.microservices.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.krystseu.microservices.apigateway.authentication.FirebaseAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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

import java.nio.charset.StandardCharsets;

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
            return handleUnauthorized(exchange, "No Authorization header found or invalid header format");
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

        // Create a security context with the decoded token
        Authentication authentication = new FirebaseAuthentication(decodedToken);
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        log.debug("Authentication created for UID: {}", decodedToken.getUid());

        // Forward the original Authorization header
        ServerHttpRequest requestWithAuthorization = exchange.getRequest().mutate()
                .header("Authorization", exchange.getRequest().getHeaders().getFirst("Authorization"))
                .build();

        return chain.filter(exchange.mutate().request(requestWithAuthorization).build())
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
        DataBuffer dataBuffer = dataBufferFactory.wrap(message.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private Mono<Void> handleFirebaseAuthException(ServerWebExchange exchange, FirebaseAuthException e) {
        log.error("FirebaseAuthException occurred", e);

        String errorMessage;

        // Check if the exception is due to token expiration
        if (e.getMessage() != null && e.getMessage().contains("Firebase ID token has expired")) {
            errorMessage = "Token has expired. Please get a new token.";
        } else {
            errorMessage = "Authentication failed: " + e.getMessage();
        }

        return handleUnauthorized(exchange, errorMessage);
    }

    private Mono<Void> handleUnexpectedException(ServerWebExchange exchange, Exception e) {
        log.error("Unexpected error occurred", e);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return exchange.getResponse().setComplete();
    }
}
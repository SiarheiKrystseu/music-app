package com.krystseu.microservices.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.krystseu.microservices.apigateway.security.FirebaseAuthentication;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;


@Component
public class FirebaseAuthenticationFilter implements WebFilter {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7);

            return Mono.fromCallable(() -> {
                        try {
                            return firebaseAuth.verifyIdToken(idToken);
                        } catch (FirebaseAuthException e) {
                            throw new RuntimeException(e); // Wrap the checked exception
                        }
                    })
                    .flatMap(decodedToken -> {
                        Authentication authentication = new FirebaseAuthentication(decodedToken);
                        SecurityContext securityContext = new SecurityContextImpl(authentication);
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                    })
                    .onErrorResume(RuntimeException.class, e -> {
                        // Handle FirebaseAuthException and other exceptions wrapped in RuntimeException
                        if (e.getCause() instanceof FirebaseAuthException) {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        } else {
                            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        return exchange.getResponse().setComplete();
                    });
        } else {
            // No token provided, unauthorized
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}








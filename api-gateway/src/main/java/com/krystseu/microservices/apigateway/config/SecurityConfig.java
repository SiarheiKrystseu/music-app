package com.krystseu.microservices.apigateway.config;

import com.google.firebase.auth.FirebaseAuth;
import com.krystseu.microservices.apigateway.filter.FirebaseAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    // Define roles as static final strings
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private final FirebaseAuth firebaseAuth;

    @Autowired
    public SecurityConfig(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring security settings");
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/prometheus").permitAll()
                        .pathMatchers("/api/storages/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .pathMatchers("/api/songs/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .pathMatchers("/api/resources/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .anyExchange().authenticated()
                )
                .addFilterAt(new FirebaseAuthenticationFilter(firebaseAuth), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}




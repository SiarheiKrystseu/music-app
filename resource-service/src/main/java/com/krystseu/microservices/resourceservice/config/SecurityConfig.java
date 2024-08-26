package com.krystseu.microservices.resourceservice.config;

import com.google.firebase.auth.FirebaseAuth;
import com.krystseu.microservices.songservice.firebase.HeaderAuthorizationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private final FirebaseAuth firebaseAuth;

    @Autowired
    public SecurityConfig(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/actuator/prometheus").permitAll()
                                .requestMatchers("/api/songs/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                                .requestMatchers("/api/storages/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                                .requestMatchers("/api/resources/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                                .anyRequest().authenticated()
                )
                .addFilterBefore(new HeaderAuthorizationFilter(firebaseAuth), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}




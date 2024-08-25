package com.krystseu.microservices.songservice.config;

import com.krystseu.microservices.songservice.filter.HeaderAuthorizationFilter;
import lombok.extern.slf4j.Slf4j;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HeaderAuthorizationFilter customHeaderAuthorizationFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/actuator/prometheus").permitAll()
                                .requestMatchers("/api/songs/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                                .anyRequest().authenticated()
                )
                .addFilterBefore(customHeaderAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}








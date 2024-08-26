package com.krystseu.microservices.storageservice.config;

import com.google.firebase.auth.FirebaseAuth;
import com.krystseu.microservices.storageservice.firebase.HeaderAuthorizationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    @Autowired
    public SecurityConfig(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security settings");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/actuator/prometheus").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/storages").hasRole(ROLE_ADMIN) // POST /api/storages is restricted to ADMIN
                                .requestMatchers(HttpMethod.DELETE, "/api/storages").hasRole(ROLE_ADMIN) // DELETE /api/storages is restricted to ADMIN
                                .requestMatchers(HttpMethod.GET, "/api/storages").hasAnyRole(ROLE_USER, ROLE_ADMIN) // GET /api/storages is accessible by USER and ADMIN
                                .requestMatchers(HttpMethod.GET, "/api/storages/**").hasAnyRole(ROLE_USER, ROLE_ADMIN) // GET /api/storages/{id} is accessible by USER and ADMIN
                                .anyRequest().authenticated() // Ensure all other requests are authenticated
                )
                .addFilterBefore(new HeaderAuthorizationFilter(firebaseAuth), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}






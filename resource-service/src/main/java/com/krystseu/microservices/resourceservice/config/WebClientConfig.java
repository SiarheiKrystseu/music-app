package com.krystseu.microservices.resourceservice.config;

import org.slf4j.MDC;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter((request, next) -> next.exchange(ClientRequest.from(request)
                        .header("X-Trace-ID", MDC.get("traceId"))
                        .build()));
    }
}

package com.krystseu.microservices.resourceprocessor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.*;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@Configuration
@Slf4j
public class WebClientConfig {
    @Value("${spring.codec.max-in-memory-size}")
    private int bufferSize;

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                        .build())
                .filter(addTraceIdHeaderFilter());
    }

    private ExchangeFilterFunction addTraceIdHeaderFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                log.debug("Adding trace ID to outgoing request: {}", traceId);
                return Mono.just(ClientRequest.from(clientRequest)
                        .header("X-Trace-Id", traceId)
                        .build());
            }
            log.debug("No trace ID found in MDC for outgoing request");
            return Mono.just(clientRequest);
        });
    }
}
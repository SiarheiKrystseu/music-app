package com.krystseu.microservices.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ErrorHandlerFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(e -> handleError(exchange, e));
    }

    private Mono<Void> handleError(ServerWebExchange exchange, Throwable throwable) {
        log.error("Routing failure", throwable);

        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory().wrap("The server encountered an error".getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
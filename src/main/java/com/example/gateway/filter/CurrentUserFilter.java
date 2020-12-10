package com.example.gateway.filter;

import net.minidev.json.JSONArray;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CurrentUserFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext().map(securityContext -> {
            Authentication authentication = securityContext.getAuthentication();
            return authentication;
        }).doOnNext(authentication -> {
            DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) authentication.getPrincipal();
            String userName = principal.getAttribute("user_name");
            exchange.getRequest().mutate().header("user_name", userName);
            Object authoritiesJson = principal.getAttribute("authorities");
            if (authoritiesJson instanceof JSONArray) {
                List<String> array = ((JSONArray) authoritiesJson)
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                exchange.getRequest().mutate().header("authorities", (String.join(" ", array)));
            }
        }).and(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

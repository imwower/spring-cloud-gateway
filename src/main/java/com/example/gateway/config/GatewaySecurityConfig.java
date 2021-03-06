package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.annotation.Resource;

@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    @Resource
    private TokenServerAuthenticationEntryPoint tokenServerAuthenticationEntryPoint;
    @Resource
    private AuthorizationManager authorizationManager;

    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
        http.oauth2ResourceServer()
                .authenticationEntryPoint(tokenServerAuthenticationEntryPoint)
                .opaqueToken();

        http.authorizeExchange()
                .anyExchange()
                .access(authorizationManager)
                .and()
                .csrf().disable();
        return http.build();
    }
}
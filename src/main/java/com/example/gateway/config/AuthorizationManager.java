package com.example.gateway.config;

import net.minidev.json.JSONArray;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class AuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {
    private static Map<String, List<String>> paths = new HashMap<String, List<String>>() {
        {
            put("/demo/demo/**", Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
            put("/demo/admin/**", Arrays.asList("ROLE_ADMIN"));
        }
    };

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {
        return authentication.map(
                auth -> new AuthorizationDecision(checkAuthorities(object.getExchange(), auth)));
    }

    private boolean checkAuthorities(ServerWebExchange exchange, Authentication authentication) {
        List<String> auths = new ArrayList<>();
        AntPathMatcher pathMatcher = new AntPathMatcher();
        String path = exchange.getRequest().getURI().getPath();
        paths.forEach((k, v) -> {
            String pattern = k;
            if (pathMatcher.match(pattern, path)) {
                auths.addAll(v);
            }
        });

        Object principal = authentication.getPrincipal();
        boolean granted = false;
        Object authoritiesJson = ((BearerTokenAuthentication) authentication).getTokenAttributes().get("authorities");
        if (authoritiesJson != null && authoritiesJson instanceof JSONArray) {
            granted = ((JSONArray) authoritiesJson).stream().anyMatch(a -> auths.contains(a));
        }
        return granted;
    }
}

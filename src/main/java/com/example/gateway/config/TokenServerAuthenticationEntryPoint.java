package com.example.gateway.config;

import com.example.gateway.util.JSONUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TokenServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
        Map<String, Object> parameters = createParameters(authException);
        String wwwAuthenticate = computeWWWAuthenticateHeaderValue(parameters);
        String body = "{}";
        try {
            body = JSONUtils.toJson(parameters);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String finalBody = body;
        Mono<Void> defer = Mono.defer(() -> {
            HttpStatus status = getStatus(authException);
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
            response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            DataBuffer wrap = response.bufferFactory().wrap(finalBody.getBytes(Charset.forName("UTF-8")));
            response.setStatusCode(status);
            return response.writeWith(Mono.just(wrap));
        });

        return defer;
    }


    private Map<String, Object> createParameters(AuthenticationException authException) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("code", -1);
        parameters.put("error_description", authException.getCause().toString());

        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) authException).getError();

            parameters.put("error", error.getErrorCode());

            if (StringUtils.hasText(error.getDescription())) {
                parameters.put("error_description", error.getDescription());
            }

            if (StringUtils.hasText(error.getUri())) {
                parameters.put("error_uri", error.getUri());
            }

            if (error instanceof BearerTokenError) {
                BearerTokenError bearerTokenError = (BearerTokenError) error;

                if (StringUtils.hasText(bearerTokenError.getScope())) {
                    parameters.put("scope", bearerTokenError.getScope());
                }
            }
        }
        return parameters;
    }

    private HttpStatus getStatus(AuthenticationException authException) {
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) authException).getError();
            if (error instanceof BearerTokenError) {
                return ((BearerTokenError) error).getHttpStatus();
            }
        }
        return HttpStatus.UNAUTHORIZED;
    }

    private static String computeWWWAuthenticateHeaderValue(Map<String, Object> parameters) {
        StringBuilder wwwAuthenticate = new StringBuilder();
        wwwAuthenticate.append("Bearer");

        if (!parameters.isEmpty()) {
            wwwAuthenticate.append(" ");
            int i = 0;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                wwwAuthenticate.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
                if (i != parameters.size() - 1) {
                    wwwAuthenticate.append(", ");
                }
                i++;
            }
        }

        return wwwAuthenticate.toString();
    }
}


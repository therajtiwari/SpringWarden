package com.springwarden.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class RoleBasedAccessFilter extends AbstractGatewayFilterFactory<RoleBasedAccessFilter.Config> {

    public RoleBasedAccessFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String rolesHeader = request.getHeaders().getFirst("X-User-Roles");

            // If the JWT filter didn't add the roles header, something is wrong. Forbid access.
            if (rolesHeader == null || rolesHeader.isEmpty()) {
                return handleForbidden(exchange, "User roles not found in request context.");
            }

            List<String> userRoles = Arrays.asList(rolesHeader.split(","));
            List<String> requiredRoles = config.getRoles();

            // Check if the user has at least one of the required roles
            boolean hasRequiredRole = !Collections.disjoint(userRoles, requiredRoles);

            if (!hasRequiredRole) {
                return handleForbidden(exchange, "User does not have the required role(s).");
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        // You can add a custom body here if needed, but for an API gateway, the status code is often enough.
        return response.setComplete();
    }

    @Getter
    @Setter
    public static class Config {
        private List<String> roles;
    }
}
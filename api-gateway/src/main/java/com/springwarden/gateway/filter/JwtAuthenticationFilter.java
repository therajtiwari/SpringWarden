package com.springwarden.gateway.filter;

import com.springwarden.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // These paths will be bypassed by the filter.
    private final List<String> publicPaths = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh"
    );

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // This check is a safeguard, but routing should handle public paths separately.
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return handleUnauthorized(exchange, "Invalid or expired JWT token");
            }

            try {
                String username = jwtUtil.extractUsername(token);
                Set<String> roles = jwtUtil.extractRoles(token);

                // Add user information to headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Email", username)
                        .header("X-User-Roles", String.join(",", roles))
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                return handleUnauthorized(exchange, "Error processing JWT token");
            }
        };
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // Optionally add a WWW-Authenticate header
        response.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\", error_description=\"" + message + "\"");
        return response.setComplete();
    }

    // Empty config class as this filter doesn't need specific route configuration.
    public static class Config {}
}
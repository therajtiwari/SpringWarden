package com.springwarden.gateway.config;

import com.springwarden.gateway.filter.JwtAuthenticationFilter;
import com.springwarden.gateway.filter.RoleBasedAccessFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           JwtAuthenticationFilter jwtFilter,
                                           RoleBasedAccessFilter roleFilter) {
        return builder.routes()
                // --- AUTH SERVICE ROUTES ---

                // 1. Public endpoints for authentication (NO JWT filter)
                .route("auth-service-public", r -> r
                        .path("/auth/login", "/auth/register", "/auth/refresh")
                        .uri("lb://auth-service"))

                // 2. Protected endpoints for token validation and user info (Requires a valid JWT)
                .route("auth-service-protected", r -> r
                        .path("/auth/validate", "/auth/user")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://auth-service"))


                // --- USER SERVICE ROUTES ---
                // IMPORTANT: More specific paths must be defined BEFORE more general ones.
                // "/api/users/admin/**" must come before "/api/users/**".

                // 3. Admin-only endpoints for user management (Requires JWT + ADMIN role)
                .route("user-service-admin", r -> r
                        .path("/api/users/admin/**")
                        .filters(f -> f
                                .filter(jwtFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(roleFilter.apply(createRoleConfig("ADMIN"))))
                        .uri("lb://user-service"))

                // 4. General user endpoints (Requires JWT + any valid role)
                .route("user-service-user", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(jwtFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(roleFilter.apply(createRoleConfig("USER", "MANAGER", "ADMIN"))))
                        .uri("lb://user-service"))

                .build();
    }

    /**
     * Helper method to create a RoleBasedAccessFilter configuration.
     * @param roles A list of roles that are permitted to access a route.
     * @return A configuration object for the RoleBasedAccessFilter.
     */
    private RoleBasedAccessFilter.Config createRoleConfig(String... roles) {
        RoleBasedAccessFilter.Config config = new RoleBasedAccessFilter.Config();
        config.setRoles(List.of(roles));
        return config;
    }
}
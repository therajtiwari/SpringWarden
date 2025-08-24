package com.springwarden.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${jwt.access-expiration}")
    private long accessExpirationValue;

    @Bean
    public long accessExpiration() {
        return accessExpirationValue;
    }
}
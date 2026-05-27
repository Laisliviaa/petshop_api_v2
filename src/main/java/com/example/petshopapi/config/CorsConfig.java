package com.example.petshopapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-API-Key");
        config.addAllowedHeader("X-Idempotency-Key");
        config.addAllowedHeader("X-API-Version");
        config.addAllowedHeader("Accept");
        config.addExposedHeader("X-RateLimit-Remaining");
        config.addExposedHeader("Retry-After");
        config.addExposedHeader("X-API-Key-Role");
        config.addExposedHeader("X-API-Version");
        config.addExposedHeader("Location");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

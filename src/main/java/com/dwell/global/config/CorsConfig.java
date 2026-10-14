package com.dwell.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Allow only origins defined in the environment variable (spaces after commas and a trailing slash are ignored)
        config.setAllowedOrigins(Arrays.stream(allowedOrigins)
                .map(String::strip)
                .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
                .filter(origin -> !origin.isEmpty())
                .toList());

        // Allowed HTTP methods
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // Allowed request headers
        config.setAllowedHeaders(List.of("*"));

        // Expose the Authorization header to the frontend
        config.setExposedHeaders(List.of("Authorization"));

        // Allow requests with cookies/credentials
        config.setAllowCredentials(true);

        // Apply the CORS config to all paths
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}

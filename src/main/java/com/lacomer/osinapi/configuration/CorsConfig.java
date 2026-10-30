package com.lacomer.osinapi.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Permite que el frontend Angular (otro puerto u otro dominio) consuma la API desde el navegador.
// Los origenes se configuran con CORS_ORIGENES
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.origenes:http://localhost:4200}")
    private String[] origenes;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenes)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "OPTIONS");
    }
}

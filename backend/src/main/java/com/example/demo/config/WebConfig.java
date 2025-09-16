package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String frontendUrl = System.getenv("FRONTEND_URL");
                if (frontendUrl != null) {
                    String[] originsOrPatterns = frontendUrl.split(",");
                    for (int i = 0; i < originsOrPatterns.length; i++) {
                        originsOrPatterns[i] = originsOrPatterns[i].trim();
                    }
                    registry
                        .addMapping("/**")
                        .allowedOriginPatterns(originsOrPatterns)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowCredentials(true);
                } else {
                    // Local development defaults
                    registry
                        .addMapping("/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowCredentials(true);
                }
            }
        };
    }
}
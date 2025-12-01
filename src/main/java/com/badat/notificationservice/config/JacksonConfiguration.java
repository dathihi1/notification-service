package com.badat.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Configuration for JSON serialization
 * Provides ObjectMapper bean for notification queue manager
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Create ObjectMapper bean with proper configuration
     * Register JavaTimeModule for JSR310 support
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }
}
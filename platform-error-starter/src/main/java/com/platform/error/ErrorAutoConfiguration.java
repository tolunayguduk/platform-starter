package com.platform.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Any module (or future microservice) that adds platform-error-spring-boot-starter as a
 * dependency gets the GlobalExceptionHandler wired automatically - no manual @Import needed.
 */
@AutoConfiguration
public class ErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}

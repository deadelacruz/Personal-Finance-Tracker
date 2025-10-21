package com.example.demo.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for JPA and transaction management.
 * Enables JPA auditing and repository scanning.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.domain.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // Configuration is handled by annotations
}

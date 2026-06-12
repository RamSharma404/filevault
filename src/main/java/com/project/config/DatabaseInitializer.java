package com.project.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            // Install pg_trgm extension for fast ILIKE searches
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            // Create GIN index on original_filename
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS file_name_trgm_idx ON files USING GIN (original_filename gin_trgm_ops)");
            logger.info("Successfully initialized pg_trgm extension and GIN index for search");
        } catch (Exception e) {
            logger.warn("Could not initialize pg_trgm (this is expected if using H2 database in tests): {}", e.getMessage());
        }
    }
}

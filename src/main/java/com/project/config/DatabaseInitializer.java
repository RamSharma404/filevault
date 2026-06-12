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
            // Phase 1: pg_trgm for fast ILIKE searches
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS file_name_trgm_idx ON files USING GIN (original_filename gin_trgm_ops)");

            // Phase 2A: Index on folder materialized path for subtree queries
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS folder_path_idx ON folders USING btree (path varchar_pattern_ops)");

            // Phase 2B: Indexes on deleted_at for trash queries and cleanup job
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS files_deleted_at_idx ON files (deleted_at) WHERE deleted_at IS NOT NULL");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS folders_deleted_at_idx ON folders (deleted_at) WHERE deleted_at IS NOT NULL");

            logger.info("Database indexes initialized successfully");
        } catch (Exception e) {
            logger.warn("Could not initialize database indexes (expected with H2 in tests): {}", e.getMessage());
        }
    }
}

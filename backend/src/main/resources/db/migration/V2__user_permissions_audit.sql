ALTER TABLE app_user
    ADD COLUMN permissions VARCHAR(512) DEFAULT NULL COMMENT 'Optional fine-grained permissions JSON or comma-separated',
    ADD COLUMN user_status VARCHAR(16) NOT NULL DEFAULT 'active',
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

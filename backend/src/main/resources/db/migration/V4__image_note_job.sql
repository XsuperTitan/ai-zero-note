CREATE TABLE image_note_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    error_message VARCHAR(2048),
    image_file_name VARCHAR(512),
    source_excerpt LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_image_note_job_job_id (job_id),
    KEY idx_image_note_job_user_created (user_id, created_at DESC),
    CONSTRAINT fk_image_note_job_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

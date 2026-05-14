CREATE TABLE note_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    note_id VARCHAR(36) NOT NULL,
    markdown_file_name VARCHAR(512) NOT NULL,
    source_label VARCHAR(512),
    title VARCHAR(512),
    abstract_excerpt VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_job_note_id (note_id),
    KEY idx_note_job_user_created (user_id, created_at DESC),
    CONSTRAINT fk_note_job_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE TABLE video_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id VARCHAR(8) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    title_snapshot VARCHAR(512),
    duration_seconds INT,
    frame_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_task_task_id (task_id),
    KEY idx_video_task_user_created (user_id, created_at DESC),
    CONSTRAINT fk_video_task_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

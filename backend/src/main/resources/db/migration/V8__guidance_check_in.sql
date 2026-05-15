CREATE TABLE guidance_check_in (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    remark VARCHAR(2000),
    video_url VARCHAR(2048),
    transcript_text MEDIUMTEXT,
    consumed_note_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_guidance_check_in_session_created (session_id, created_at DESC),
    KEY idx_guidance_check_in_user_created (user_id, created_at DESC),
    CONSTRAINT fk_guidance_check_in_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_guidance_check_in_session FOREIGN KEY (session_id) REFERENCES guidance_session (id) ON DELETE CASCADE
);

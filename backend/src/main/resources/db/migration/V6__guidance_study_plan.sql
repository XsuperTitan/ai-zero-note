CREATE TABLE guidance_study_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    plan_json MEDIUMTEXT NOT NULL,
    generation_source VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guidance_study_plan_session (session_id),
    CONSTRAINT fk_guidance_study_plan_session FOREIGN KEY (session_id) REFERENCES guidance_session (id) ON DELETE CASCADE
);

CREATE TABLE guidance_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tutor_persona VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    questionnaire_json MEDIUMTEXT NOT NULL,
    report_summary MEDIUMTEXT NOT NULL,
    llm_prompt_constraints MEDIUMTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_guidance_session_user_created (user_id, created_at DESC),
    CONSTRAINT fk_guidance_session_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

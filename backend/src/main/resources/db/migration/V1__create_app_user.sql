CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account VARCHAR(64) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_name VARCHAR(128),
    user_role VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_account (user_account)
);

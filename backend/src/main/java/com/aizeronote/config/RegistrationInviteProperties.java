package com.aizeronote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration")
public class RegistrationInviteProperties {

    /**
     * Required invite code for self-service registration (override via REGISTRATION_INVITE_CODE).
     */
    private String inviteCode = "ninifun";

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}

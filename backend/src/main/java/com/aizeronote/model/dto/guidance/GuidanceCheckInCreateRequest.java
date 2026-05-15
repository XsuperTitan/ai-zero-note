package com.aizeronote.model.dto.guidance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GuidanceCheckInCreateRequest {

    @NotNull
    private Long sessionId;

    @Size(max = 2000)
    private String remark;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

package com.aizeronote.model.enums;

public enum UserRoleEnum {

    USER("user"),
    ADMIN("admin");

    private final String value;

    UserRoleEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            if (roleEnum.value.equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }
}

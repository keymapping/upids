package com.upids.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum RoleEnum {

    ADMIN("admin", "管理员"),
    USER("user", "普通用户");

    @EnumValue
    private final String value;
    private final String description;

    RoleEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RoleEnum fromValue(String value) {
        for (RoleEnum role : values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
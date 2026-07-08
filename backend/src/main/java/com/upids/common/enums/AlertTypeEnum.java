package com.upids.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 预警类型枚举
 */
@Getter
public enum AlertTypeEnum {

    THRESHOLD("threshold", "阈值预警"),
    ANOMALY("anomaly", "异常预警"),
    SYSTEM("system", "系统预警");

    @EnumValue
    private final String value;
    private final String description;

    AlertTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static AlertTypeEnum fromValue(String value) {
        for (AlertTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown alert type: " + value);
    }
}
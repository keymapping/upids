package com.upids.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 缺陷类型枚举
 */
@Getter
public enum DefectTypeEnum {

    CRACK("crack", "裂缝"),
    CORROSION("corrosion", "腐蚀"),
    FRACTURE("fracture", "断裂");

    @EnumValue
    private final String value;
    private final String description;

    DefectTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static DefectTypeEnum fromValue(String value) {
        for (DefectTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown defect type: " + value);
    }
}
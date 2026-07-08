package com.upids.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatusEnum {

    PENDING("pending", "待处理"),
    RUNNING("running", "处理中"),
    DONE("done", "已完成"),
    FAILED("failed", "失败");

    @EnumValue
    private final String value;
    private final String description;

    TaskStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static TaskStatusEnum fromValue(String value) {
        for (TaskStatusEnum status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + value);
    }
}
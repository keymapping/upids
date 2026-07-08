package com.upids.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 风险报告实体
 */
@Data
@TableName(value = "risk_report", autoResultMap = true)
public class RiskReport {

    @TableId(type = IdType.AUTO)
    private Long reportId;

    private String reportTitle;

    private String regionCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer totalDefects;

    private Integer highRiskCount;

    /**
     * JSONB 类型，使用 JsonbTypeHandler 处理
     */
    @TableField(typeHandler = com.upids.common.handler.JsonbTypeHandler.class)
    private Map<String, Object> reportContent;

    private String filePath;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
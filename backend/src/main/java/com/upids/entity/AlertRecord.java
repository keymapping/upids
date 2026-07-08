package com.upids.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.upids.common.enums.AlertTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警记录实体
 */
@Data
@TableName("alert_record")
public class AlertRecord {

    @TableId(type = IdType.AUTO)
    private Long alertId;

    private Long defectId;

    private String pipelineId;

    private Integer alertLevel;

    private AlertTypeEnum alertType;

    private String alertMessage;

    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggeredAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
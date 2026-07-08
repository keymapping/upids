package com.upids.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.upids.common.enums.TaskStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 识别任务实体
 */
@Data
@TableName("detection_task")
public class DetectionTask {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private Long recordId;

    private TaskStatusEnum status;

    private Integer retryCount;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
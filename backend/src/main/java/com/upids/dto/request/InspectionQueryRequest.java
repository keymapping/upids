package com.upids.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 巡检查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "巡检查询请求")
public class InspectionQueryRequest extends PageRequest {

    @Schema(description = "检测结果")
    private String detectionResult;

    @Schema(description = "任务状态")
    private String taskStatus;
}
package com.upids.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页请求基类
 */
@Data
@Schema(description = "分页请求")
public class PageRequest {

    @Schema(description = "页码，从1开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer pageSize = 20;

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
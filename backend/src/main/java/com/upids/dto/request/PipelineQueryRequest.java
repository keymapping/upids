package com.upids.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管线查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管线查询请求")
public class PipelineQueryRequest extends PageRequest {

    @Schema(description = "材质类型")
    private String materialType;

    @Schema(description = "区域编码")
    private String regionCode;

    @Schema(description = "状态（数字类型）")
    private Integer status;
}
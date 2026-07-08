package com.upids.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.upids.common.enums.DefectTypeEnum;
import com.upids.common.handler.PostgisGeometryTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 缺陷实体
 */
@Data
@TableName(value = "defect", autoResultMap = true)
public class Defect {

    @TableId(type = IdType.AUTO)
    private Long defectId;

    private Long recordId;

    private String pipelineId;

    private DefectTypeEnum defectType;

    private Integer severityLevel;

    /**
     * PostGIS Point WKT格式
     */
    @TableField(typeHandler = PostgisGeometryTypeHandler.class)
    private String location;

    private String bbox;

    private BigDecimal confidenceScore;

    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
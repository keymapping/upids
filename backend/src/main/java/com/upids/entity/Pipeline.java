package com.upids.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.upids.common.handler.PostgisGeometryTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管线实体
 */
@Data
@TableName(value = "pipeline", autoResultMap = true)
public class Pipeline {

    @TableId(type = IdType.INPUT)
    private String pipelineId;

    private String pipelineName;

    /**
     * PostGIS LineString WKT格式
     */
    @TableField(typeHandler = PostgisGeometryTypeHandler.class)
    private String geoCoordinates;

    private String materialType;

    private BigDecimal diameter;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate installTime;

    private String regionCode;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
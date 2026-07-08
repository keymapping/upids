package com.upids.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upids.entity.Defect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 缺陷 Mapper
 */
@Mapper
public interface DefectMapper extends BaseMapper<Defect> {

    /**
     * 使用 PostGIS 函数直接获取缺陷坐标，绕过类型处理器
     */
    @Select("SELECT defect_id as \"defectId\", record_id as \"recordId\", " +
            "pipeline_id as \"pipelineId\", defect_type as \"defectType\", " +
            "severity_level as \"severityLevel\", confidence_score as \"confidenceScore\", " +
            "source, detected_at as \"detectedAt\", " +
            "ST_X(location) as lng, ST_Y(location) as lat " +
            "FROM defect WHERE location IS NOT NULL")
    List<Map<String, Object>> selectDefectsWithCoords();
}

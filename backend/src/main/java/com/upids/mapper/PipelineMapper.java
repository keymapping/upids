package com.upids.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upids.entity.Pipeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管线 Mapper
 */
@Mapper
public interface PipelineMapper extends BaseMapper<Pipeline> {

    /**
     * 使用 PostGIS 函数直接获取管线 GeoJSON，绕过类型处理器
     */
    @Select("SELECT p.pipeline_id as \"pipelineId\", p.pipeline_name as \"pipelineName\", " +
            "p.material_type as \"materialType\", p.diameter, " +
            "ST_AsGeoJSON(p.geo_coordinates)::json as geometry, " +
            "COALESCE(dc.defect_count, 0) as \"defectCount\" " +
            "FROM pipeline p LEFT JOIN " +
            "(SELECT pipeline_id, COUNT(*) as defect_count FROM defect GROUP BY pipeline_id) dc " +
            "ON p.pipeline_id = dc.pipeline_id " +
            "WHERE p.geo_coordinates IS NOT NULL")
    List<Map<String, Object>> selectPipelinesWithGeoJSON();
}

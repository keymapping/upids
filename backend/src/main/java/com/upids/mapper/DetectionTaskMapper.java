package com.upids.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upids.entity.DetectionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 识别任务 Mapper
 */
@Mapper
public interface DetectionTaskMapper extends BaseMapper<DetectionTask> {

    List<Map<String, Object>> selectTasksFiltered(@Param("status") String status,
                                                   @Param("pipelineName") String pipelineName,
                                                   @Param("detectionResult") String detectionResult,
                                                   @Param("startTime") String startTime,
                                                   @Param("endTime") String endTime,
                                                   @Param("limit") int limit,
                                                   @Param("offset") int offset);

    int countFiltered(@Param("status") String status, @Param("pipelineName") String pipelineName,
                      @Param("detectionResult") String detectionResult,
                      @Param("startTime") String startTime, @Param("endTime") String endTime);
}

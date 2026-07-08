package com.upids.service;

import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取概览统计数据（ECharts数据）
     * 包含：summary, defectByType, defectBySeverity, defectTrend, taskStatusDistribution
     */
    Map<String, Object> getOverview();

    /**
     * 获取概览统计数据（支持筛选）
     */
    Map<String, Object> getOverview(String startTime, String endTime, String regionCode);
}

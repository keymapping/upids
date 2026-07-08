package com.upids.service;

import java.util.Map;

/**
 * Mock数据服务接口
 */
public interface MockService {

    /**
     * 生成模拟数据
     * @param pipelineCount 管线数量
     * @param inspectionCount 检测记录数量
     * @param defectRatio 缺陷比例 (0-1)
     */
    Map<String, Object> generateMockData(int pipelineCount, int inspectionCount, double defectRatio);
}

package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.entity.DetectionTask;

/**
 * 识别任务服务接口
 */
public interface DetectionTaskService extends IService<DetectionTask> {

    /**
     * 创建识别任务
     */
    DetectionTask createTask(Long recordId);

    /**
     * 查询任务状态
     */
    DetectionTask getTaskStatus(Long taskId);

    /**
     * 重试失败任务
     */
    DetectionTask retryTask(Long taskId);

    /**
     * 处理任务（调用Python服务）
     */
    void processTask(Long taskId);
}

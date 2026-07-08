package com.upids.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.client.PythonDetectClient;
import com.upids.common.enums.TaskStatusEnum;
import com.upids.common.exception.BusinessException;
import com.upids.entity.DetectionTask;
import com.upids.entity.InspectionRecord;
import com.upids.mapper.DetectionTaskMapper;
import com.upids.mapper.InspectionRecordMapper;
import com.upids.queue.DetectionTaskConsumer;
import com.upids.service.DetectionTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 识别任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionTaskServiceImpl extends ServiceImpl<DetectionTaskMapper, DetectionTask>
        implements DetectionTaskService {

    private final InspectionRecordMapper inspectionRecordMapper;
    private final DetectionTaskConsumer detectionTaskConsumer;
    private final PythonDetectClient pythonDetectClient;

    @Override
    public DetectionTask createTask(Long recordId) {
        InspectionRecord record = inspectionRecordMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound("检测记录不存在: " + recordId);
        }

        DetectionTask task = new DetectionTask();
        task.setRecordId(recordId);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        save(task);

        detectionTaskConsumer.enqueue(task.getTaskId());
        log.info("Detection task created: taskId={}, recordId={}", task.getTaskId(), recordId);
        return task;
    }

    @Override
    public DetectionTask getTaskStatus(Long taskId) {
        DetectionTask task = getById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + taskId);
        }
        return task;
    }

    @Override
    public DetectionTask retryTask(Long taskId) {
        DetectionTask task = getById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + taskId);
        }
        if (task.getStatus() != TaskStatusEnum.FAILED) {
            throw BusinessException.badRequest("只能重试失败的任务");
        }

        task.setStatus(TaskStatusEnum.PENDING);
        task.setRetryCount(0);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        updateById(task);

        detectionTaskConsumer.processTaskAsync(task.getTaskId());
        log.info("Detection task retried: taskId={}", taskId);
        return task;
    }

    @Override
    public void processTask(Long taskId) {
        detectionTaskConsumer.processTaskAsync(taskId);
    }
}

package com.upids.queue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.upids.client.PythonDetectClient;
import com.upids.common.enums.TaskStatusEnum;
import com.upids.entity.Defect;
import com.upids.entity.DetectionTask;
import com.upids.entity.InspectionRecord;
import com.upids.mapper.DefectMapper;
import com.upids.mapper.DetectionTaskMapper;
import com.upids.mapper.InspectionRecordMapper;
import com.upids.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 识别任务队列消费者
 * 使用 BlockingQueue 实现
 * 消费 pending 任务 → 改为 running → 调用 Python → 改为 done/failed
 * 失败最多重试 3 次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionTaskConsumer {

    private final DetectionTaskMapper detectionTaskMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final DefectMapper defectMapper;
    private final PythonDetectClient pythonDetectClient;
    private final AlertService alertService;

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private DetectionTaskConsumer self;

    @Qualifier("taskExecutor")
    @org.springframework.beans.factory.annotation.Autowired
    private Executor taskExecutor;

    private final BlockingQueue<Long> taskQueue = new LinkedBlockingQueue<>(1000);

    private static final int MAX_RETRY_COUNT = 3;
    private static final long STUCK_TIMEOUT_MINUTES = 2;

    /**
     * 添加任务到队列
     */
    public void enqueue(Long taskId) {
        try {
            taskQueue.put(taskId);
            log.info("Task {} enqueued", taskId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to enqueue task {}", taskId, e);
        }
    }

    /**
     * 每 3 秒检查并消费待处理任务
     * 优先从内存队列取，同时从数据库兜底查询
     */
    @Scheduled(fixedDelay = 3000)
    public void consumeTasks() {
        // 先回收卡住的 running 任务
        recoverStuckTasks();

        // 从内存队列取任务
        int count = 0;
        while (!taskQueue.isEmpty() && count < 10) {
            Long taskId = taskQueue.poll();
            if (taskId != null) {
                final Long tid = taskId;
                taskExecutor.execute(() -> processTask(tid));
                count++;
            }
        }

        // 兜底：从数据库查询未入队的 pending 任务
        if (count == 0) {
            List<DetectionTask> pendingTasks = detectionTaskMapper.selectList(
                    new LambdaQueryWrapper<DetectionTask>()
                            .eq(DetectionTask::getStatus, TaskStatusEnum.PENDING)
                            .orderByAsc(DetectionTask::getCreatedAt)
                            .last("LIMIT 10")
            );
            for (DetectionTask t : pendingTasks) {
                if (!taskQueue.contains(t.getTaskId())) {
                    final Long tid = t.getTaskId();
                    taskExecutor.execute(() -> processTask(tid));
                }
            }
        }
    }

    /**
     * 回收卡住的 running 任务（超过2分钟未完成的）
     */
    private void recoverStuckTasks() {
        try {
            List<DetectionTask> stuckTasks = detectionTaskMapper.selectList(
                    new LambdaQueryWrapper<DetectionTask>()
                            .eq(DetectionTask::getStatus, TaskStatusEnum.RUNNING)
                            .lt(DetectionTask::getStartedAt, LocalDateTime.now().minusMinutes(STUCK_TIMEOUT_MINUTES))
            );
            for (DetectionTask stuck : stuckTasks) {
                log.warn("Recovering stuck task {}, started at {}", stuck.getTaskId(), stuck.getStartedAt());
                stuck.setStatus(TaskStatusEnum.PENDING);
                stuck.setErrorMessage(null);
                detectionTaskMapper.updateById(stuck);
                enqueue(stuck.getTaskId());
            }
        } catch (Exception e) {
            log.error("Failed to recover stuck tasks", e);
        }
    }

    /**
     * 处理单个任务
     * pending → running → 调用Python → done/failed
     * 失败最多重试3次
     */
    public void processTask(Long taskId) {
        DetectionTask task = detectionTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("Task {} not found", taskId);
            return;
        }

        // 只处理 pending 状态的任务
        if (task.getStatus() != TaskStatusEnum.PENDING) {
            return;
        }

        // 先检查 Python 服务是否可用，不可用直接标记失败，不浪费重试次数
        if (!pythonDetectClient.healthCheck()) {
            task.setStatus(TaskStatusEnum.FAILED);
            task.setErrorMessage("Python识别服务未启动，请先启动 python-service");
            task.setStartedAt(LocalDateTime.now());
            task.setFinishedAt(LocalDateTime.now());
            detectionTaskMapper.updateById(task);
            log.warn("Task {} failed: Python service unavailable", taskId);
            return;
        }

        // 更新状态为 running
        task.setStatus(TaskStatusEnum.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        detectionTaskMapper.updateById(task);

        try {
            // 获取关联的检测记录
            InspectionRecord record = inspectionRecordMapper.selectById(task.getRecordId());
            if (record == null) {
                throw new RuntimeException("检测记录不存在: " + task.getRecordId());
            }

            // 调用 Python 识别服务
            String imagePath = record.getImagePath();
            PythonDetectClient.DetectResponse response = pythonDetectClient.detect(
                    imagePath, record.getPipelineId(), task.getRecordId());

            // 解析结果，保存缺陷
            if (response != null && response.isSuccess()) {
                if (response.getDefectType() != null && !"none".equalsIgnoreCase(response.getDefectType())) {
                    saveDefect(record, response);
                }
            }

            // 标记任务完成
            task.setStatus(TaskStatusEnum.DONE);
            task.setErrorMessage(null);
            task.setFinishedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Task {} processing failed", taskId, e);

            int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            if (retryCount < MAX_RETRY_COUNT) {
                task.setStatus(TaskStatusEnum.PENDING);
                task.setRetryCount(retryCount + 1);
                task.setErrorMessage(e.getMessage());
                // 重新入队
                enqueue(taskId);
            } else {
                task.setStatus(TaskStatusEnum.FAILED);
                task.setErrorMessage(e.getMessage());
                task.setFinishedAt(LocalDateTime.now());
            }
        }

        detectionTaskMapper.updateById(task);
    }

    /**
     * 保存缺陷记录并触发预警
     */
    private void saveDefect(InspectionRecord record, PythonDetectClient.DetectResponse response) {
        Defect defect = new Defect();
        defect.setRecordId(record.getRecordId());
        defect.setPipelineId(record.getPipelineId());

        if (response.getDefectType() != null) {
            try {
                defect.setDefectType(com.upids.common.enums.DefectTypeEnum.fromValue(response.getDefectType()));
            } catch (IllegalArgumentException e) {
                defect.setDefectType(com.upids.common.enums.DefectTypeEnum.CRACK);
            }
        }

        defect.setSeverityLevel(response.getSeverityLevel() != null ? response.getSeverityLevel() : 1);
        defect.setConfidenceScore(response.getConfidenceScore() != null
                ? response.getConfidenceScore() : BigDecimal.valueOf(response.getConfidence() != null ? response.getConfidence() : 0));
        defect.setBbox(response.getBbox());
        defect.setSource(response.getSource() != null ? response.getSource() : "ai");
        defect.setDetectedAt(LocalDateTime.now());
        defect.setCreatedAt(LocalDateTime.now());
        defectMapper.insert(defect);

        // 触发预警判断
        alertService.createAlert(defect);

        log.info("Defect saved: defectId={}, pipelineId={}", defect.getDefectId(), defect.getPipelineId());
    }

    /**
     * 获取待处理任务数量
     */
    public int getPendingTaskCount() {
        return detectionTaskMapper.selectCount(
                new LambdaQueryWrapper<DetectionTask>()
                        .eq(DetectionTask::getStatus, TaskStatusEnum.PENDING)
        ).intValue();
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * 异步处理任务（用于重试或手动触发）
     */
    @Async("taskExecutor")
    public void processTaskAsync(Long taskId) {
        processTask(taskId);
    }
}

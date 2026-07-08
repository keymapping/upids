package com.upids.controller;

import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.entity.DetectionTask;
import com.upids.mapper.DetectionTaskMapper;
import com.upids.service.DetectionTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 识别任务控制器
 */
@Tag(name = "识别任务", description = "异步识别任务管理接口")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class DetectionTaskController {

    private final DetectionTaskService detectionTaskService;
    private final DetectionTaskMapper detectionTaskMapper;

    @Operation(summary = "查询任务列表")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pipelineName,
            @RequestParam(required = false) String detectionResult,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String nameFilter = (pipelineName != null && !pipelineName.isBlank()) ? pipelineName.trim() : null;
        String statusFilter = (status != null && !status.isBlank()) ? status.trim() : null;
        String detectionResultFilter = (detectionResult != null && !detectionResult.isBlank()) ? detectionResult.trim() : null;
        String startTimeFilter = (startTime != null && !startTime.isBlank()) ? startTime.trim() : null;
        String endTimeFilter = (endTime != null && !endTime.isBlank()) ? endTime.trim() + " 23:59:59" : null;
        int total = detectionTaskMapper.countFiltered(statusFilter, nameFilter, detectionResultFilter, startTimeFilter, endTimeFilter);
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> list = detectionTaskMapper.selectTasksFiltered(statusFilter, nameFilter, detectionResultFilter, startTimeFilter, endTimeFilter, pageSize, offset);
        PageResult<Map<String, Object>> result = new PageResult<>(list, (long) total, page, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "创建识别任务")
    @PostMapping
    public Result<DetectionTask> createTask(@RequestParam Long recordId) {
        return Result.success(detectionTaskService.createTask(recordId));
    }

    @Operation(summary = "查询任务状态")
    @GetMapping("/{taskId}")
    public Result<DetectionTask> getTaskStatus(@PathVariable Long taskId) {
        return Result.success(detectionTaskService.getTaskStatus(taskId));
    }

    @Operation(summary = "重试失败任务")
    @PostMapping("/{taskId}/retry")
    public Result<DetectionTask> retryTask(@PathVariable Long taskId) {
        return Result.success(detectionTaskService.retryTask(taskId));
    }
}

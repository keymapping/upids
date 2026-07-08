package com.upids.controller;

import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.entity.AlertRecord;
import com.upids.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 预警控制器
 */
@Tag(name = "预警管理", description = "预警记录管理接口")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "预警列表")
    @GetMapping
    public Result<PageResult<AlertRecord>> listAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String pipelineId,
            @RequestParam(required = false) Integer minLevel,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(alertService.listAlerts(page, pageSize, isRead, alertType, pipelineId, minLevel, startTime, endTime));
    }

    @Operation(summary = "未读预警数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        Map<String, Long> data = new HashMap<>();
        data.put("count", alertService.getUnreadCount());
        return Result.success(data);
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        alertService.markAllAsRead();
        return Result.success();
    }
}

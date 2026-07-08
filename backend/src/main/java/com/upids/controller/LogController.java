package com.upids.controller;

import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.entity.OperationLog;
import com.upids.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 日志控制器
 */
@Tag(name = "操作日志", description = "操作日志查询接口")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "日志列表（管理员）")
    @GetMapping
    public Result<PageResult<OperationLog>> listLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(operationLogService.listLogs(page, pageSize, operation, username, module, startTime, endTime));
    }

    @Operation(summary = "我的日志")
    @GetMapping("/mine")
    public Result<PageResult<OperationLog>> listMyLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Long userId = getCurrentUserId();
        return Result.success(operationLogService.listMyLogs(userId, page, pageSize, operation, module, startTime, endTime));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }
}

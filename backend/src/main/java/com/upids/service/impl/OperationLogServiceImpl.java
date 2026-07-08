package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.common.result.PageResult;
import com.upids.entity.OperationLog;
import com.upids.mapper.OperationLogMapper;
import com.upids.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public PageResult<OperationLog> listLogs(Integer page, Integer pageSize, String operation, String username, String module, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = buildFilterWrapper(operation, username, module, startTime, endTime);

        Page<OperationLog> result = page(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public PageResult<OperationLog> listMyLogs(Long userId, Integer page, Integer pageSize, String operation, String module, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = buildFilterWrapper(operation, null, module, startTime, endTime);
        wrapper.eq(OperationLog::getUserId, userId);

        Page<OperationLog> result = page(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    private LambdaQueryWrapper<OperationLog> buildFilterWrapper(String operation, String username, String module, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt);

        if (operation != null && !operation.isBlank()) {
            wrapper.like(OperationLog::getOperation, operation);
        }

        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLog::getUsername, username);
        }

        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLog::getModule, module);
        }

        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }

        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(OperationLog::getCreatedAt, endTime + " 23:59:59");
        }

        return wrapper;
    }
}

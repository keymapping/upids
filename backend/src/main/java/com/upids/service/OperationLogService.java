package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 日志列表（管理员可查看所有，支持筛选）
     */
    PageResult<OperationLog> listLogs(Integer page, Integer pageSize, String operation, String username, String module, String startTime, String endTime);

    /**
     * 当前用户的日志（支持筛选）
     */
    PageResult<OperationLog> listMyLogs(Long userId, Integer page, Integer pageSize, String operation, String module, String startTime, String endTime);
}

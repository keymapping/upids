package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.entity.AlertRecord;
import com.upids.entity.Defect;

/**
 * 预警服务接口
 */
public interface AlertService extends IService<AlertRecord> {

    /**
     * 创建预警（判断是否触发）
     */
    void createAlert(Defect defect);

    /**
     * 预警列表（分页，支持筛选）
     */
    PageResult<AlertRecord> listAlerts(Integer page, Integer pageSize, Boolean isRead,
                                        String alertType, String pipelineId, Integer minLevel,
                                        String startTime, String endTime);

    /**
     * 未读预警数量
     */
    long getUnreadCount();

    /**
     * 标记单条已读
     */
    void markAsRead(Long alertId);

    /**
     * 全部已读
     */
    void markAllAsRead();
}

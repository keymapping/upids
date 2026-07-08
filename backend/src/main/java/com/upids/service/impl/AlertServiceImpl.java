package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.common.enums.AlertTypeEnum;
import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.entity.AlertRecord;
import com.upids.entity.Defect;
import com.upids.mapper.AlertRecordMapper;
import com.upids.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 预警服务实现
 */
@Slf4j
@Service
public class AlertServiceImpl extends ServiceImpl<AlertRecordMapper, AlertRecord> implements AlertService {

    @Value("${upids.alert.severity-threshold:4}")
    private Integer severityThreshold;

    @Override
    public void createAlert(Defect defect) {
        if (defect.getSeverityLevel() == null || defect.getSeverityLevel() < severityThreshold) {
            return;
        }

        AlertRecord alert = new AlertRecord();
        alert.setDefectId(defect.getDefectId());
        alert.setPipelineId(defect.getPipelineId());
        alert.setAlertLevel(defect.getSeverityLevel());
        alert.setAlertType(AlertTypeEnum.THRESHOLD);
        alert.setAlertMessage(buildAlertMessage(defect));
        alert.setIsRead(false);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setCreatedAt(LocalDateTime.now());
        save(alert);

        log.info("Alert created for defect {}: severity={}", defect.getDefectId(), defect.getSeverityLevel());
    }

    @Override
    public PageResult<AlertRecord> listAlerts(Integer page, Integer pageSize, Boolean isRead,
                                               String alertType, String pipelineId, Integer minLevel,
                                               String startTime, String endTime) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AlertRecord::getTriggeredAt);

        if (isRead != null) {
            wrapper.eq(AlertRecord::getIsRead, isRead);
        }

        if (alertType != null && !alertType.isBlank()) {
            wrapper.eq(AlertRecord::getAlertType, AlertTypeEnum.valueOf(alertType.toUpperCase()));
        }

        if (pipelineId != null && !pipelineId.isBlank()) {
            wrapper.eq(AlertRecord::getPipelineId, pipelineId);
        }

        if (minLevel != null && minLevel > 0) {
            wrapper.ge(AlertRecord::getAlertLevel, minLevel);
        }

        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(AlertRecord::getTriggeredAt, startTime);
        }

        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(AlertRecord::getTriggeredAt, endTime + " 23:59:59");
        }

        Page<AlertRecord> pageResult = page(new Page<>(page, pageSize), wrapper);
        return PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, pageSize);
    }

    @Override
    public long getUnreadCount() {
        return count(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getIsRead, false));
    }

    @Override
    public void markAsRead(Long alertId) {
        AlertRecord alert = getById(alertId);
        if (alert == null) {
            throw BusinessException.notFound("预警记录不存在: " + alertId);
        }
        alert.setIsRead(true);
        updateById(alert);
    }

    @Override
    public void markAllAsRead() {
        update(new LambdaUpdateWrapper<AlertRecord>()
                .eq(AlertRecord::getIsRead, false)
                .set(AlertRecord::getIsRead, true));
    }

    private String buildAlertMessage(Defect defect) {
        String typeDesc = defect.getDefectType() != null ? defect.getDefectType().getDescription() : "未知";
        return String.format("管线 %s 检测到 %s 缺陷，严重等级: %d，请及时处理",
                defect.getPipelineId(), typeDesc, defect.getSeverityLevel());
    }
}

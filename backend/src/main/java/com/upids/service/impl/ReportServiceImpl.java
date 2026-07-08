package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.entity.Defect;
import com.upids.entity.DetectionTask;
import com.upids.entity.InspectionRecord;
import com.upids.entity.RiskReport;
import com.upids.mapper.DefectMapper;
import com.upids.mapper.DetectionTaskMapper;
import com.upids.mapper.InspectionRecordMapper;
import com.upids.mapper.RiskReportMapper;
import com.upids.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<RiskReportMapper, RiskReport> implements ReportService {

    private final DefectMapper defectMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final DetectionTaskMapper detectionTaskMapper;
    private final com.upids.mapper.PipelineMapper pipelineMapper;

    @Override
    public RiskReport generateReport(String regionCode, String reportTitle, String startTime, String endTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reportStartTime = StringUtils.hasText(startTime) ? parseDateTime(startTime) : now.minusDays(30);
        LocalDateTime reportEndTime = StringUtils.hasText(endTime) ? parseDateTime(endTime) : now;

        LambdaQueryWrapper<Defect> defectWrapper = new LambdaQueryWrapper<>();
        defectWrapper.ge(Defect::getDetectedAt, reportStartTime);
        defectWrapper.le(Defect::getDetectedAt, reportEndTime);
        if (StringUtils.hasText(regionCode)) {
            defectWrapper.inSql(Defect::getPipelineId,
                    "SELECT pipeline_id FROM pipeline WHERE region_code = '" + regionCode + "'");
        }
        List<Defect> defects = defectMapper.selectList(defectWrapper);
        int totalDefects = defects.size();
        long highRiskCount = defects.stream()
                .filter(d -> d.getSeverityLevel() != null && d.getSeverityLevel() >= 4)
                .count();
        long mediumRiskCount = defects.stream()
                .filter(d -> d.getSeverityLevel() != null && d.getSeverityLevel() >= 2 && d.getSeverityLevel() <= 3)
                .count();
        long lowRiskCount = defects.stream()
                .filter(d -> d.getSeverityLevel() != null && d.getSeverityLevel() == 1)
                .count();

        LambdaQueryWrapper<InspectionRecord> inspectionWrapper = new LambdaQueryWrapper<>();
        inspectionWrapper.ge(InspectionRecord::getCreatedAt, reportStartTime);
        inspectionWrapper.le(InspectionRecord::getCreatedAt, reportEndTime);
        long totalInspections = inspectionRecordMapper.selectCount(inspectionWrapper);

        LambdaQueryWrapper<DetectionTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.ge(DetectionTask::getCreatedAt, reportStartTime);
        taskWrapper.le(DetectionTask::getCreatedAt, reportEndTime);
        long completedTasks = detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, com.upids.common.enums.TaskStatusEnum.DONE));
        long failedTasks = detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, com.upids.common.enums.TaskStatusEnum.FAILED));

        Map<String, Object> content = new HashMap<>();
        content.put("totalDefects", totalDefects);
        content.put("highRiskCount", highRiskCount);
        content.put("mediumRiskCount", mediumRiskCount);
        content.put("lowRiskCount", lowRiskCount);
        content.put("totalInspections", totalInspections);
        content.put("completedTasks", completedTasks);
        content.put("failedTasks", failedTasks);

        double defectRate = totalInspections > 0 ? (double) totalDefects / totalInspections * 100 : 0;
        double successRate = totalInspections > 0 ? (double) completedTasks / totalInspections * 100 : 0;
        content.put("defectRate", Math.round(defectRate * 100) / 100.0);
        content.put("successRate", Math.round(successRate * 100) / 100.0);

        Map<String, Long> byType = defects.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDefectType() != null ? d.getDefectType().getValue() : "unknown",
                        Collectors.counting()));
        content.put("defectsByType", byType);

        Map<Integer, Long> bySeverity = defects.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getSeverityLevel() != null ? d.getSeverityLevel() : 0,
                        Collectors.counting()));
        content.put("defectsBySeverity", bySeverity);

        content.put("defectTrend", buildDefectTrendForReport(reportStartTime, reportEndTime, regionCode));

        Map<String, Long> byRegion = defects.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getPipelineId() != null ? getRegionCode(d.getPipelineId()) : "未知",
                        Collectors.counting()));
        content.put("defectsByRegion", byRegion);

        List<Map<String, Object>> topRiskPipelines = defects.stream()
                .filter(d -> d.getSeverityLevel() != null && d.getSeverityLevel() >= 3)
                .collect(Collectors.groupingBy(Defect::getPipelineId))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("pipelineId", entry.getKey());
                    item.put("defectCount", entry.getValue().size());
                    item.put("maxSeverity", entry.getValue().stream()
                            .mapToInt(d -> d.getSeverityLevel() != null ? d.getSeverityLevel() : 0)
                            .max().orElse(0));
                    return item;
                })
                .sorted((a, b) -> (int) b.get("maxSeverity") - (int) a.get("maxSeverity"))
                .limit(10)
                .collect(Collectors.toList());
        content.put("topRiskPipelines", topRiskPipelines);

        List<Map<String, Object>> defectDetails = defects.stream()
                .limit(20)
                .map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("defectId", d.getDefectId());
                    item.put("pipelineId", d.getPipelineId());
                    item.put("defectType", d.getDefectType() != null ? d.getDefectType().getValue() : "unknown");
                    item.put("severityLevel", d.getSeverityLevel());
                    item.put("confidenceScore", d.getConfidenceScore());
                    item.put("detectedAt", d.getDetectedAt());
                    return item;
                })
                .collect(Collectors.toList());
        content.put("defectDetails", defectDetails);

        List<String> recommendations = new ArrayList<>();
        if (highRiskCount > 0) {
            recommendations.add("存在 " + highRiskCount + " 个高风险缺陷，建议立即安排检修");
        }
        if (mediumRiskCount > 0) {
            recommendations.add("存在 " + mediumRiskCount + " 个中等风险缺陷，建议制定维修计划");
        }
        if (failedTasks > 0) {
            recommendations.add("有 " + failedTasks + " 个检测任务失败，建议检查检测服务状态");
        }
        if (totalDefects > 100) {
            recommendations.add("缺陷数量较多，建议加强日常巡检频率");
        }
        if (defectRate > 30) {
            recommendations.add("缺陷检出率较高(" + String.format("%.1f", defectRate) + "%)，建议增加检测频次");
        }
        recommendations.add("建议定期更新管线检测数据，确保风险评估准确性");
        content.put("recommendations", recommendations);

        RiskReport report = new RiskReport();
        report.setReportTitle(StringUtils.hasText(reportTitle) ? reportTitle :
                "风险评估报告 - " + (StringUtils.hasText(regionCode) ? regionCode : "全区域"));
        report.setRegionCode(regionCode);
        report.setStartTime(reportStartTime);
        report.setEndTime(reportEndTime);

        report.setTotalDefects(totalDefects);
        report.setHighRiskCount((int) highRiskCount);
        report.setReportContent(content);
        report.setCreatedBy(getCurrentUserId());
        report.setCreatedAt(now);
        save(report);

        log.info("Report generated: reportId={}, region={}, defects={}", report.getReportId(), regionCode, totalDefects);
        return report;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr.contains(" ")) {
                return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
            } else if (dateTimeStr.length() == 10) {
                return LocalDate.parse(dateTimeStr).atStartOfDay();
            }
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private List<Map<String, Object>> buildDefectTrendForReport(LocalDateTime startTime, LocalDateTime endTime, String regionCode) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Defect::getDetectedAt, dayStart);
            wrapper.lt(Defect::getDetectedAt, dayEnd);
            if (StringUtils.hasText(regionCode)) {
                wrapper.inSql(Defect::getPipelineId,
                        "SELECT pipeline_id FROM pipeline WHERE region_code = '" + regionCode + "'");
            }

            Long count = defectMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("date", currentDate.format(fmt));
            item.put("count", count);
            trend.add(item);

            currentDate = currentDate.plusDays(1);
        }
        return trend;
    }

    private String getRegionCode(String pipelineId) {
        try {
            com.upids.entity.Pipeline pipeline = pipelineMapper.selectOne(
                    new LambdaQueryWrapper<com.upids.entity.Pipeline>()
                            .select(com.upids.entity.Pipeline::getRegionCode)
                            .eq(com.upids.entity.Pipeline::getPipelineId, pipelineId)
            );
            return pipeline != null ? pipeline.getRegionCode() : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }

    @Override
    public PageResult<RiskReport> listReports(Integer page, Integer pageSize, String keyword, String regionCode, String startTime, String endTime) {
        LambdaQueryWrapper<RiskReport> wrapper = new LambdaQueryWrapper<RiskReport>()
                .orderByDesc(RiskReport::getCreatedAt);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(RiskReport::getReportTitle, keyword);
        }

        if (regionCode != null && !regionCode.isBlank()) {
            wrapper.eq(RiskReport::getRegionCode, regionCode);
        }

        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(RiskReport::getCreatedAt, startTime);
        }

        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(RiskReport::getCreatedAt, endTime + " 23:59:59");
        }

        Page<RiskReport> pageResult = page(new Page<>(page, pageSize), wrapper);
        return PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, pageSize);
    }

    @Override
    public RiskReport getReportDetail(Long reportId) {
        RiskReport report = getById(reportId);
        if (report == null) {
            throw BusinessException.notFound("报告不存在: " + reportId);
        }
        return report;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }
}

package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.upids.common.enums.DefectTypeEnum;
import com.upids.common.enums.TaskStatusEnum;
import com.upids.entity.*;
import com.upids.mapper.*;
import com.upids.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PipelineMapper pipelineMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final DefectMapper defectMapper;
    private final DetectionTaskMapper detectionTaskMapper;
    private final AlertRecordMapper alertRecordMapper;

    @Override
    public Map<String, Object> getOverview() {
        return getOverview(null, null, null);
    }

    @Override
    public Map<String, Object> getOverview(String startTime, String endTime, String regionCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", buildSummary(startTime, endTime, regionCode));
        result.put("defectByType", buildDefectByType(startTime, endTime, regionCode));
        result.put("defectBySeverity", buildDefectBySeverity(startTime, endTime, regionCode));
        result.put("defectTrend", buildDefectTrend(startTime, endTime));
        result.put("taskStatusDistribution", buildTaskStatusDistribution(startTime, endTime));
        result.put("pipelineByRegion", buildPipelineByRegion(regionCode));
        result.put("pipelineByMaterial", buildPipelineByMaterial(regionCode));
        result.put("detectionRate", buildDetectionRate(startTime, endTime));
        result.put("recentDefects", buildRecentDefects(startTime, endTime, regionCode));
        result.put("recentTasks", buildRecentTasks(startTime, endTime));
        return result;
    }

    private Map<String, Object> buildSummary(String startTime, String endTime, String regionCode) {
        Map<String, Object> summary = new HashMap<>();

        LambdaQueryWrapper<Pipeline> pipelineWrapper = new LambdaQueryWrapper<>();
        if (regionCode != null && !regionCode.isBlank()) {
            pipelineWrapper.eq(Pipeline::getRegionCode, regionCode);
        }
        summary.put("pipelineCount", pipelineMapper.selectCount(pipelineWrapper));

        LambdaQueryWrapper<InspectionRecord> inspectionWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            inspectionWrapper.ge(InspectionRecord::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            inspectionWrapper.le(InspectionRecord::getCreatedAt, endTime + " 23:59:59");
        }
        summary.put("inspectionCount", inspectionRecordMapper.selectCount(inspectionWrapper));

        LambdaQueryWrapper<Defect> defectWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            defectWrapper.ge(Defect::getDetectedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            defectWrapper.le(Defect::getDetectedAt, endTime + " 23:59:59");
        }
        if (regionCode != null && !regionCode.isBlank()) {
            defectWrapper.inSql(Defect::getPipelineId, 
                    "SELECT pipeline_id FROM pipeline WHERE region_code = '" + regionCode + "'");
        }
        summary.put("defectCount", defectMapper.selectCount(defectWrapper));
        summary.put("highRiskCount", defectMapper.selectCount(defectWrapper.clone().ge(Defect::getSeverityLevel, 4)));

        LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            alertWrapper.ge(AlertRecord::getTriggeredAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            alertWrapper.le(AlertRecord::getTriggeredAt, endTime + " 23:59:59");
        }
        if (regionCode != null && !regionCode.isBlank()) {
            alertWrapper.eq(AlertRecord::getPipelineId, regionCode);
        }
        summary.put("alertCount", alertRecordMapper.selectCount(alertWrapper));
        summary.put("unreadAlerts", alertRecordMapper.selectCount(alertWrapper.clone().eq(AlertRecord::getIsRead, false)));

        LambdaQueryWrapper<DetectionTask> taskWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            taskWrapper.ge(DetectionTask::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            taskWrapper.le(DetectionTask::getCreatedAt, endTime + " 23:59:59");
        }
        summary.put("pendingTasks", detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.PENDING)));
        summary.put("runningTasks", detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.RUNNING)));
        summary.put("completedTasks", detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.DONE)));
        summary.put("failedTasks", detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.FAILED)));

        return summary;
    }

    private List<Map<String, Object>> buildDefectByType(String startTime, String endTime, String regionCode) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        applyDefectFilters(wrapper, startTime, endTime, regionCode);
        List<Defect> defects = defectMapper.selectList(wrapper);

        Map<String, Long> grouped = defects.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDefectType() != null ? d.getDefectType().getValue() : "unknown",
                        Collectors.counting()));

        List<Map<String, Object>> list = new ArrayList<>();
        grouped.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", type);
            item.put("count", count);
            list.add(item);
        });
        return list;
    }

    private List<Map<String, Object>> buildDefectBySeverity(String startTime, String endTime, String regionCode) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        applyDefectFilters(wrapper, startTime, endTime, regionCode);
        List<Defect> defects = defectMapper.selectList(wrapper);

        Map<Integer, Long> grouped = defects.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getSeverityLevel() != null ? d.getSeverityLevel() : 0,
                        Collectors.counting()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("level", i);
            item.put("count", grouped.getOrDefault(i, 0L));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildDefectTrend(String startTime, String endTime) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();

        LocalDate startDate = startTime != null && !startTime.isBlank() 
                ? LocalDate.parse(startTime) 
                : now.toLocalDate().minusDays(29);
        LocalDate endDate = endTime != null && !endTime.isBlank() 
                ? LocalDate.parse(endTime) 
                : now.toLocalDate();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.plusDays(1).atStartOfDay();

            Long count = defectMapper.selectCount(
                    new LambdaQueryWrapper<Defect>()
                            .ge(Defect::getDetectedAt, dayStart)
                            .lt(Defect::getDetectedAt, dayEnd));

            Map<String, Object> item = new HashMap<>();
            item.put("date", currentDate.format(fmt));
            item.put("count", count);
            trend.add(item);

            currentDate = currentDate.plusDays(1);
        }
        return trend;
    }

    private List<Map<String, Object>> buildTaskStatusDistribution(String startTime, String endTime) {
        LambdaQueryWrapper<DetectionTask> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(DetectionTask::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(DetectionTask::getCreatedAt, endTime + " 23:59:59");
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (TaskStatusEnum status : TaskStatusEnum.values()) {
            Long count = detectionTaskMapper.selectCount(wrapper.clone().eq(DetectionTask::getStatus, status));
            Map<String, Object> item = new HashMap<>();
            item.put("status", status.getValue());
            item.put("count", count);
            list.add(item);
        }
        return list;
    }

    private void applyDefectFilters(LambdaQueryWrapper<Defect> wrapper, String startTime, String endTime, String regionCode) {
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(Defect::getDetectedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(Defect::getDetectedAt, endTime + " 23:59:59");
        }
        if (regionCode != null && !regionCode.isBlank()) {
            wrapper.inSql(Defect::getPipelineId,
                    "SELECT pipeline_id FROM pipeline WHERE region_code = '" + regionCode + "'");
        }
    }

    private List<Map<String, Object>> buildPipelineByRegion(String regionCode) {
        LambdaQueryWrapper<Pipeline> wrapper = new LambdaQueryWrapper<>();
        if (regionCode != null && !regionCode.isBlank()) {
            wrapper.eq(Pipeline::getRegionCode, regionCode);
        }

        List<Pipeline> pipelines = pipelineMapper.selectList(wrapper);
        Map<String, Long> grouped = pipelines.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getRegionCode() != null ? p.getRegionCode() : "未分配",
                        Collectors.counting()));

        List<Map<String, Object>> list = new ArrayList<>();
        grouped.forEach((region, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("region", region);
            item.put("count", count);
            list.add(item);
        });
        list.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return list;
    }

    private List<Map<String, Object>> buildPipelineByMaterial(String regionCode) {
        LambdaQueryWrapper<Pipeline> wrapper = new LambdaQueryWrapper<>();
        if (regionCode != null && !regionCode.isBlank()) {
            wrapper.eq(Pipeline::getRegionCode, regionCode);
        }

        List<Pipeline> pipelines = pipelineMapper.selectList(wrapper);
        Map<String, Long> grouped = pipelines.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getMaterialType() != null ? p.getMaterialType() : "未知",
                        Collectors.counting()));

        List<Map<String, Object>> list = new ArrayList<>();
        grouped.forEach((material, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("material", material);
            item.put("count", count);
            list.add(item);
        });
        list.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return list;
    }

    private Map<String, Object> buildDetectionRate(String startTime, String endTime) {
        Map<String, Object> rate = new HashMap<>();

        LambdaQueryWrapper<InspectionRecord> inspectionWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            inspectionWrapper.ge(InspectionRecord::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            inspectionWrapper.le(InspectionRecord::getCreatedAt, endTime + " 23:59:59");
        }
        long totalInspections = inspectionRecordMapper.selectCount(inspectionWrapper);

        LambdaQueryWrapper<DetectionTask> taskWrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            taskWrapper.ge(DetectionTask::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            taskWrapper.le(DetectionTask::getCreatedAt, endTime + " 23:59:59");
        }
        long completedTasks = detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.DONE));
        long failedTasks = detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.FAILED));
        long pendingTasks = detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.PENDING));
        long runningTasks = detectionTaskMapper.selectCount(taskWrapper.clone().eq(DetectionTask::getStatus, TaskStatusEnum.RUNNING));

        rate.put("totalInspections", totalInspections);
        rate.put("completedTasks", completedTasks);
        rate.put("failedTasks", failedTasks);
        rate.put("pendingTasks", pendingTasks);
        rate.put("runningTasks", runningTasks);

        double successRate = totalInspections > 0 ? (double) completedTasks / totalInspections * 100 : 0;
        rate.put("successRate", Math.round(successRate * 100) / 100.0);

        return rate;
    }

    private List<Map<String, Object>> buildRecentDefects(String startTime, String endTime, String regionCode) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        applyDefectFilters(wrapper, startTime, endTime, regionCode);
        wrapper.orderByDesc(Defect::getDetectedAt).last("LIMIT 10");

        List<Defect> defects = defectMapper.selectList(wrapper);
        List<Map<String, Object>> list = new ArrayList<>();

        for (Defect d : defects) {
            Map<String, Object> item = new HashMap<>();
            item.put("defectId", d.getDefectId());
            item.put("pipelineId", d.getPipelineId());
            item.put("defectType", d.getDefectType() != null ? d.getDefectType().getValue() : "unknown");
            item.put("severityLevel", d.getSeverityLevel());
            item.put("confidenceScore", d.getConfidenceScore());
            item.put("detectedAt", d.getDetectedAt());
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildRecentTasks(String startTime, String endTime) {
        LambdaQueryWrapper<DetectionTask> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(DetectionTask::getCreatedAt, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(DetectionTask::getCreatedAt, endTime + " 23:59:59");
        }
        wrapper.orderByDesc(DetectionTask::getCreatedAt).last("LIMIT 10");

        List<DetectionTask> tasks = detectionTaskMapper.selectList(wrapper);
        List<Map<String, Object>> list = new ArrayList<>();

        for (DetectionTask t : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskId", t.getTaskId());
            item.put("recordId", t.getRecordId());
            item.put("status", t.getStatus() != null ? t.getStatus().getValue() : "unknown");
            item.put("createdAt", t.getCreatedAt());
            item.put("finishedAt", t.getFinishedAt());

            if (t.getRecordId() != null) {
                InspectionRecord record = inspectionRecordMapper.selectById(t.getRecordId());
                if (record != null) {
                    item.put("pipelineId", record.getPipelineId());
                    item.put("detectionResult", record.getDetectionResult());
                    if (record.getPipelineId() != null) {
                        Pipeline pipeline = pipelineMapper.selectById(record.getPipelineId());
                        if (pipeline != null) {
                            item.put("pipelineName", pipeline.getPipelineName());
                        }
                    }
                }
            }
            list.add(item);
        }
        return list;
    }
}

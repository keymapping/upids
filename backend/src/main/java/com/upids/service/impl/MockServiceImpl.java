package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.upids.common.enums.AlertTypeEnum;
import com.upids.common.enums.DefectTypeEnum;
import com.upids.entity.*;
import com.upids.mapper.*;
import com.upids.service.MockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockServiceImpl implements MockService {

    private final PipelineMapper pipelineMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final DefectMapper defectMapper;
    private final DetectionTaskMapper detectionTaskMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final RiskReportMapper riskReportMapper;

    private static final String[] MATERIALS = {"PE", "PVC", "铸铁", "混凝土", "钢管", "铜管"};
    private static final String[] REGIONS = {"BJ001", "BJ002", "SH001", "SH002", "GZ001", "SZ001", "CD001", "WH001"};
    private static final DefectTypeEnum[] DEFECT_TYPES = DefectTypeEnum.values();
    private static final Random RANDOM = new Random();
    private static final String[] ERROR_MESSAGES = {
            "图像文件损坏或格式不支持，无法进行缺陷识别",
            "Python识别服务连接超时，请检查服务是否正常运行",
            "图像分辨率过低，无法进行有效检测",
            "检测记录关联的管线信息不存在",
            "AI模型加载失败，识别服务不可用",
            "网络连接异常，无法调用识别服务",
            "图像尺寸超出处理范围，最大支持2048x2048像素",
            "识别结果解析失败，返回数据格式异常",
            "服务端内部错误，请稍后重试",
            "图片路径不存在或无访问权限"
    };

    @Override
    @Transactional
    public Map<String, Object> generateMockData(int pipelineCount, int inspectionCount, double defectRatio) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        // 清理旧数据（按外键依赖顺序）
        clearAllData();

        int pc = generatePipelines(pipelineCount);
        result.put("pipelines", pc);

        int rc = generateInspectionRecords(inspectionCount);
        result.put("inspectionRecords", rc);

        int dc = generateDefects(rc, defectRatio);
        result.put("defects", dc);

        int tc = generateDetectionTasks(rc);
        result.put("detectionTasks", tc);

        int ac = generateAlerts();
        result.put("alerts", ac);

        int reportCount = generateReports();
        result.put("reports", reportCount);

        result.put("elapsed", System.currentTimeMillis() - start + "ms");
        log.info("Mock data generated: {}", result);
        return result;
    }

    /**
     * 清理所有业务数据（按外键依赖顺序）
     */
    private void clearAllData() {
        riskReportMapper.delete(null);
        alertRecordMapper.delete(null);
        defectMapper.delete(null);
        detectionTaskMapper.delete(null);
        inspectionRecordMapper.delete(null);
        pipelineMapper.delete(null);
        log.info("Cleared all existing business data");
    }

    private int generatePipelines(int count) {
        List<Pipeline> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Pipeline p = new Pipeline();
            p.setPipelineId(String.format("PL-%05d", i + 1));
            p.setPipelineName("管线-" + (i + 1));

            double lng = 116.0 + RANDOM.nextDouble() * 2.0;
            double lat = 39.0 + RANDOM.nextDouble() * 2.0;
            double lng2 = lng + (RANDOM.nextDouble() - 0.5) * 0.01;
            double lat2 = lat + (RANDOM.nextDouble() - 0.5) * 0.01;
            p.setGeoCoordinates(String.format("LINESTRING(%f %f, %f %f)", lng, lat, lng2, lat2));

            p.setMaterialType(MATERIALS[RANDOM.nextInt(MATERIALS.length)]);
            p.setDiameter(BigDecimal.valueOf(100 + RANDOM.nextInt(900)));
            p.setInstallTime(LocalDate.of(2000 + RANDOM.nextInt(24), 1 + RANDOM.nextInt(12), 1 + RANDOM.nextInt(28)));
            p.setRegionCode(REGIONS[RANDOM.nextInt(REGIONS.length)]);
            p.setStatus(1);
            p.setCreatedAt(LocalDateTime.now().minusDays(RANDOM.nextInt(365)));
            p.setUpdatedAt(p.getCreatedAt());
            batch.add(p);

            if (batch.size() >= 500) {
                for (Pipeline pipeline : batch) {
                    pipelineMapper.insert(pipeline);
                }
                batch.clear();
            }
        }
        for (Pipeline pipeline : batch) {
            pipelineMapper.insert(pipeline);
        }
        log.info("Generated {} pipelines", count);
        return count;
    }

    private int generateInspectionRecords(int count) {
        List<Pipeline> pipelines = pipelineMapper.selectList(null);
        if (pipelines.isEmpty()) return 0;

        List<InspectionRecord> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Pipeline p = pipelines.get(RANDOM.nextInt(pipelines.size()));

            InspectionRecord record = new InspectionRecord();
            record.setPipelineId(p.getPipelineId());
            record.setUserId((long) (1 + RANDOM.nextInt(2))); // 只使用存在的用户ID (1=admin, 2=user)
            record.setImagePath("mock/no_image.jpg");
            record.setImageName("img_" + (i + 1) + ".jpg");
            record.setDetectionResult(RANDOM.nextBoolean() ? getRandomDefectType() : "normal");
            record.setConfidenceScore(BigDecimal.valueOf(0.5 + RANDOM.nextDouble() * 0.5).setScale(2, RoundingMode.HALF_UP));
            record.setInspectTime(LocalDateTime.now().minusDays(RANDOM.nextInt(90)).minusHours(RANDOM.nextInt(24)));
            record.setCreatedAt(record.getInspectTime());
            batch.add(record);

            if (batch.size() >= 500) {
                for (InspectionRecord r : batch) {
                    inspectionRecordMapper.insert(r);
                }
                batch.clear();
            }
        }
        for (InspectionRecord r : batch) {
            inspectionRecordMapper.insert(r);
        }
        log.info("Generated {} inspection records", count);
        return count;
    }

    private int generateDefects(int recordCount, double defectRatio) {
        List<InspectionRecord> records = inspectionRecordMapper.selectList(null);
        int defectCount = 0;
        List<Defect> batch = new ArrayList<>();

        for (InspectionRecord record : records) {
            if (RANDOM.nextDouble() < defectRatio) {
                Pipeline pipeline = pipelineMapper.selectById(record.getPipelineId());

                Defect defect = new Defect();
                defect.setRecordId(record.getRecordId());
                defect.setPipelineId(record.getPipelineId());
                defect.setDefectType(DEFECT_TYPES[RANDOM.nextInt(DEFECT_TYPES.length)]);
                defect.setSeverityLevel(1 + RANDOM.nextInt(5));

                if (pipeline != null && pipeline.getGeoCoordinates() != null) {
                    double lng = 116.0 + RANDOM.nextDouble() * 2.0;
                    double lat = 39.0 + RANDOM.nextDouble() * 2.0;
                    defect.setLocation(String.format("POINT(%f %f)", lng, lat));
                }

                defect.setBbox(String.format("[%d,%d,%d,%d]",
                        RANDOM.nextInt(500), RANDOM.nextInt(500),
                        50 + RANDOM.nextInt(200), 50 + RANDOM.nextInt(200)));
                defect.setConfidenceScore(BigDecimal.valueOf(0.5 + RANDOM.nextDouble() * 0.5).setScale(2, RoundingMode.HALF_UP));
                defect.setSource(RANDOM.nextBoolean() ? "ai" : "manual");
                defect.setDetectedAt(record.getInspectTime());
                defect.setCreatedAt(LocalDateTime.now());
                batch.add(defect);
                defectCount++;

                if (batch.size() >= 500) {
                    for (Defect d : batch) {
                        defectMapper.insert(d);
                    }
                    batch.clear();
                }
            }
        }
        for (Defect d : batch) {
            defectMapper.insert(d);
        }
        log.info("Generated {} defects", defectCount);
        return defectCount;
    }

    private int generateDetectionTasks(int recordCount) {
        List<InspectionRecord> records = inspectionRecordMapper.selectList(null);
        int taskCount = 0;
        List<DetectionTask> batch = new ArrayList<>();

        for (InspectionRecord record : records) {
            if (RANDOM.nextDouble() < 0.5) {
                DetectionTask task = new DetectionTask();
                task.setRecordId(record.getRecordId());

                int r = RANDOM.nextInt(10);
                if (r < 6) {
                    task.setStatus(com.upids.common.enums.TaskStatusEnum.DONE);
                } else if (r < 8) {
                    task.setStatus(com.upids.common.enums.TaskStatusEnum.PENDING);
                } else if (r < 9) {
                    task.setStatus(com.upids.common.enums.TaskStatusEnum.RUNNING);
                } else {
                    task.setStatus(com.upids.common.enums.TaskStatusEnum.FAILED);
                }

                task.setRetryCount(task.getStatus() == com.upids.common.enums.TaskStatusEnum.FAILED ? RANDOM.nextInt(3) : 0);
                task.setErrorMessage(task.getStatus() == com.upids.common.enums.TaskStatusEnum.FAILED ? ERROR_MESSAGES[RANDOM.nextInt(ERROR_MESSAGES.length)] : null);
                task.setCreatedAt(LocalDateTime.now().minusDays(RANDOM.nextInt(30)));
                task.setStartedAt(task.getStatus() != com.upids.common.enums.TaskStatusEnum.PENDING ? task.getCreatedAt().plusSeconds(RANDOM.nextInt(60)) : null);
                task.setFinishedAt(task.getStatus() == com.upids.common.enums.TaskStatusEnum.DONE || task.getStatus() == com.upids.common.enums.TaskStatusEnum.FAILED
                        ? task.getStartedAt().plusSeconds(RANDOM.nextInt(120)) : null);
                batch.add(task);
                taskCount++;

                if (batch.size() >= 500) {
                    for (DetectionTask t : batch) {
                        detectionTaskMapper.insert(t);
                    }
                    batch.clear();
                }
            }
        }
        for (DetectionTask t : batch) {
            detectionTaskMapper.insert(t);
        }
        log.info("Generated {} detection tasks", taskCount);
        return taskCount;
    }

    private int generateAlerts() {
        List<Defect> defects = defectMapper.selectList(
                new LambdaQueryWrapper<Defect>()
                        .ge(Defect::getSeverityLevel, 4));

        int alertCount = 0;
        List<AlertRecord> batch = new ArrayList<>();
        for (Defect defect : defects) {
            AlertRecord alert = new AlertRecord();
            alert.setDefectId(defect.getDefectId());
            alert.setPipelineId(defect.getPipelineId());
            alert.setAlertLevel(defect.getSeverityLevel());
            alert.setAlertType(AlertTypeEnum.THRESHOLD);
            alert.setAlertMessage(String.format("管线 %s 检测到缺陷，严重等级: %d", defect.getPipelineId(), defect.getSeverityLevel()));
            alert.setIsRead(RANDOM.nextBoolean());
            alert.setTriggeredAt(defect.getDetectedAt());
            alert.setCreatedAt(LocalDateTime.now());
            batch.add(alert);
            alertCount++;

            if (batch.size() >= 500) {
                for (AlertRecord a : batch) {
                    alertRecordMapper.insert(a);
                }
                batch.clear();
            }
        }
        for (AlertRecord a : batch) {
            alertRecordMapper.insert(a);
        }
        log.info("Generated {} alerts", alertCount);
        return alertCount;
    }

    private int generateReports() {
        LocalDateTime now = LocalDateTime.now();
        String[][] reportConfigs = {
                {"全区域风险评估报告", null},
                {"BJ001区域风险评估报告", "BJ001"},
                {"SH001区域风险评估报告", "SH001"},
        };

        int count = 0;
        for (String[] config : reportConfigs) {
            LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
            if (config[1] != null) {
                wrapper.eq(Defect::getPipelineId, config[1]);
            }
            List<Defect> defects = defectMapper.selectList(wrapper);
            long highRisk = defects.stream().filter(d -> d.getSeverityLevel() != null && d.getSeverityLevel() >= 4).count();

            Map<String, Object> content = new HashMap<>();
            content.put("totalDefects", defects.size());
            content.put("highRiskCount", highRisk);

            Map<String, Long> byType = defects.stream()
                    .filter(d -> d.getDefectType() != null)
                    .collect(Collectors.groupingBy(d -> d.getDefectType().getValue(), Collectors.counting()));
            content.put("defectsByType", byType);

            List<String> recommendations = new ArrayList<>();
            if (highRisk > 0) recommendations.add("存在 " + highRisk + " 个高风险缺陷，建议立即安排检修");
            recommendations.add("建议定期更新管线检测数据，确保风险评估准确性");
            content.put("recommendations", recommendations);

            RiskReport report = new RiskReport();
            report.setReportTitle(config[0]);
            report.setRegionCode(config[1]);
            report.setStartTime(now.minusDays(30));
            report.setEndTime(now);
            report.setTotalDefects(defects.size());
            report.setHighRiskCount((int) highRisk);
            report.setReportContent(content);
            report.setCreatedBy(1L);
            report.setCreatedAt(now.minusDays(RANDOM.nextInt(7)));
            riskReportMapper.insert(report);
            count++;
        }

        log.info("Generated {} reports", count);
        return count;
    }

    private String getRandomDefectType() {
        String[] types = {"crack", "corrosion", "fracture"};
        return types[RANDOM.nextInt(types.length)];
    }
}

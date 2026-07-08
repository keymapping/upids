package com.upids.controller;

import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.entity.RiskReport;
import com.upids.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报告控制器
 */
@Tag(name = "风险报告", description = "风险报告管理接口")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "生成报告")
    @PostMapping("/generate")
    public Result<RiskReport> generateReport(@RequestBody(required = false) java.util.Map<String, String> body) {
        String regionCode = body != null ? body.get("regionCode") : null;
        String reportTitle = body != null ? body.get("reportTitle") : null;
        String startTime = body != null ? body.get("startTime") : null;
        String endTime = body != null ? body.get("endTime") : null;
        return Result.success(reportService.generateReport(regionCode, reportTitle, startTime, endTime));
    }

    @Operation(summary = "报告列表")
    @GetMapping
    public Result<PageResult<RiskReport>> listReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(reportService.listReports(page, pageSize, keyword, regionCode, startTime, endTime));
    }

    @Operation(summary = "报告详情")
    @GetMapping("/{reportId}")
    public Result<RiskReport> getReportDetail(@PathVariable Long reportId) {
        return Result.success(reportService.getReportDetail(reportId));
    }
}

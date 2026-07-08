package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.entity.RiskReport;

import java.util.List;
import java.util.Map;

/**
 * 报告服务接口
 */
public interface ReportService extends IService<RiskReport> {

    /**
     * 生成风险报告
     */
    RiskReport generateReport(String regionCode, String reportTitle, String startTime, String endTime);

    /**
     * 报告列表（分页，支持筛选）
     */
    PageResult<RiskReport> listReports(Integer page, Integer pageSize, String keyword, String regionCode, String startTime, String endTime);

    /**
     * 报告详情
     */
    RiskReport getReportDetail(Long reportId);
}

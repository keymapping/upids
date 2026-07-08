import { get, post } from '@/utils/request'
import type { ReportVO, ReportParams, PageQuery, PageResult, ApiResult } from '@/types/api'

// 获取报告列表
export function getReportList(params?: PageQuery & { 
  keyword?: string;
  regionCode?: string;
  startTime?: string;
  endTime?: string;
}): Promise<ApiResult<PageResult<ReportVO>>> {
  return get<PageResult<ReportVO>>('/api/reports', params)
}

// 获取报告详情
export function getReportById(id: number): Promise<ApiResult<ReportVO>> {
  return get<ReportVO>(`/api/reports/${id}`)
}

// 生成报告
export function generateReport(data: ReportParams): Promise<ApiResult<ReportVO>> {
  return post<ReportVO>('/api/reports/generate', data)
}

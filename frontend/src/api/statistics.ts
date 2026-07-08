import { get } from '@/utils/request';
import type { StatisticsOverviewVO, ApiResult } from '@/types/api';

// 获取统计概览（ECharts 数据源）
export function getStatisticsOverview(params?: {
  startTime?: string;
  endTime?: string;
  regionCode?: string;
}): Promise<ApiResult<StatisticsOverviewVO>> {
  return get<StatisticsOverviewVO>('/api/statistics/overview', params);
}

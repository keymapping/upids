import { get } from '@/utils/request'
import type { LogVO, PageQuery, PageResult, ApiResult } from '@/types/api'

// 获取操作日志列表（管理员）
export function getLogList(params?: PageQuery & { 
  operation?: string;
  username?: string;
  module?: string;
  startTime?: string;
  endTime?: string;
}): Promise<ApiResult<PageResult<LogVO>>> {
  return get<PageResult<LogVO>>('/api/logs', params)
}

// 获取当前用户操作日志
export function getMyLogs(params?: PageQuery & { 
  operation?: string;
  module?: string;
  startTime?: string;
  endTime?: string;
}): Promise<ApiResult<PageResult<LogVO>>> {
  return get<PageResult<LogVO>>('/api/logs/mine', params)
}

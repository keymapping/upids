import { get, put } from '@/utils/request';
import type { AlertVO, PageQuery, PageResult, ApiResult } from '@/types/api';

// 获取预警列表
export function getAlertList(
  params?: PageQuery & {
    isRead?: boolean;
    alertType?: string;
    pipelineId?: string;
    minLevel?: number;
    startTime?: string;
    endTime?: string;
  }
): Promise<ApiResult<PageResult<AlertVO>>> {
  return get<PageResult<AlertVO>>('/api/alerts', params);
}

// 获取未读预警数量
export function getUnreadCount(): Promise<ApiResult<{ count: number }>> {
  return get<{ count: number }>('/api/alerts/unread-count');
}

// 标记单条预警已读
export function markAlertAsRead(alertId: number): Promise<ApiResult<void>> {
  return put<void>(`/api/alerts/${alertId}/read`);
}

// 全部标记已读
export function markAllAlertsAsRead(): Promise<
  ApiResult<{ updatedCount: number }>
> {
  return put<{ updatedCount: number }>('/api/alerts/read-all');
}

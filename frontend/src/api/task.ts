import { get, post } from '@/utils/request'
import type { TaskVO, PageQuery, PageResult, ApiResult } from '@/types/api'

// 获取任务列表
export function getTaskList(params?: PageQuery & { 
  status?: string; 
  pipelineName?: string;
  detectionResult?: string;
  startTime?: string;
  endTime?: string;
}): Promise<ApiResult<PageResult<TaskVO>>> {
  return get<PageResult<TaskVO>>('/api/tasks', params)
}

// 获取任务详情
export function getTaskById(taskId: number): Promise<ApiResult<TaskVO>> {
  return get<TaskVO>(`/api/tasks/${taskId}`)
}

// 重试失败任务
export function retryTask(taskId: number): Promise<ApiResult<TaskVO>> {
  return post<TaskVO>(`/api/tasks/${taskId}/retry`)
}

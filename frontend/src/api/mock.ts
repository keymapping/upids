import { post, get } from '@/utils/request';
import type { ApiResult, MockGenerateVO, MockProgressVO } from '@/types/api';

// 生成模拟数据（调用后端API）
export function generateMockData(params?: {
  pipelineCount?: number;
  inspectionCount?: number;
  defectRatio?: number;
  historyYears?: number;
}): Promise<ApiResult<MockGenerateVO>> {
  return post<MockGenerateVO>('/api/mock/generate', params);
}

// 查询模拟数据生成进度
export function getMockProgress(
  jobId: string
): Promise<ApiResult<MockProgressVO>> {
  return get<MockProgressVO>(`/api/mock/generate/${jobId}`);
}

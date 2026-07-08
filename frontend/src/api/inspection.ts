import { get, post } from '@/utils/request';
import type {
  InspectionVO,
  InspectionUploadVO,
  PageQuery,
  PageResult,
  ApiResult,
} from '@/types/api';

// 获取检测记录列表
export function getInspectionList(
  params?: PageQuery & {
    keyword?: string;
    detectionResult?: string;
    taskStatus?: string;
    startTime?: string;
    endTime?: string;
  }
): Promise<ApiResult<PageResult<InspectionVO>>> {
  return get<PageResult<InspectionVO>>('/api/inspections', params);
}

// 获取检测记录详情
export function getInspectionById(
  id: number
): Promise<ApiResult<InspectionVO>> {
  return get<InspectionVO>(`/api/inspections/${id}`);
}

// 上传巡检图像（单张）
export function uploadInspectionImage(
  file: File,
  pipelineId: string,
  inspectTime?: string
): Promise<ApiResult<InspectionUploadVO>> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('pipelineId', pipelineId);
  if (inspectTime) {
    formData.append('inspectTime', inspectTime);
  }
  return post<InspectionUploadVO>('/api/inspections/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

// 批量上传（ZIP）
export function uploadInspectionBatch(
  file: File,
  pipelineId: string
): Promise<
  ApiResult<{
    batchId: string;
    totalFiles: number;
    successCount: number;
    taskIds: number[];
  }>
> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('pipelineId', pipelineId);
  return post('/api/inspections/upload-batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

// 获取图像文件 URL
export function getImageUrl(recordId: number): string {
  return `/api/files/${recordId}`;
}

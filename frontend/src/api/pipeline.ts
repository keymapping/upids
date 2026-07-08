import { get, post, put, del } from '@/utils/request';
import type {
  PipelineVO,
  PipelineImportVO,
  PageQuery,
  PageResult,
  ApiResult,
  GeoJSONFeatureCollection,
} from '@/types/api';

// 获取管道列表
export function getPipelineList(
  params: PageQuery
): Promise<ApiResult<PageResult<PipelineVO>>> {
  return get<PageResult<PipelineVO>>('/api/pipelines', params);
}

// 获取所有管道（不分页）
export function getAllPipelines(): Promise<ApiResult<PipelineVO[]>> {
  return get<PipelineVO[]>('/api/pipelines/all');
}

// 获取管道详情
export function getPipelineById(id: number): Promise<ApiResult<PipelineVO>> {
  return get<PipelineVO>(`/api/pipelines/${id}`);
}

// 创建管道
export function createPipeline(
  data: Partial<PipelineVO>
): Promise<ApiResult<PipelineVO>> {
  return post<PipelineVO>('/api/pipelines', data);
}

// 更新管道
export function updatePipeline(
  id: number,
  data: Partial<PipelineVO>
): Promise<ApiResult<PipelineVO>> {
  return put<PipelineVO>(`/api/pipelines/${id}`, data);
}

// 删除管道
export function deletePipeline(id: number): Promise<ApiResult<void>> {
  return del<void>(`/api/pipelines/${id}`);
}

// 批量删除管道
export function batchDeletePipelines(ids: number[]): Promise<ApiResult<void>> {
  return post<void>('/api/pipelines/batch-delete', { ids });
}

// 获取管道统计信息
export function getPipelineStatistics(): Promise<
  ApiResult<{
    total: number;
    normal: number;
    abnormal: number;
  }>
> {
  return get('/api/pipelines/statistics');
}

// 获取管线 GeoJSON 图层数据（GIS 地图用）
export function getPipelinesGeoJSON(params: {
  minLng: number;
  minLat: number;
  maxLng: number;
  maxLat: number;
  regionCode?: string;
}): Promise<ApiResult<GeoJSONFeatureCollection>> {
  return get<GeoJSONFeatureCollection>('/api/pipelines/geojson', params);
}

// 导入管线数据
export function importPipeline(
  file: File,
  fileType: 'geojson' | 'excel'
): Promise<ApiResult<PipelineImportVO>> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('fileType', fileType);
  return post<PipelineImportVO>('/api/pipelines/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

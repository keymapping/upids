import { get } from '@/utils/request'
import type { DefectVO, PageQuery, PageResult, ApiResult, GeoJSONFeatureCollection } from '@/types/api'

// 获取缺陷列表
export function getDefectList(params?: PageQuery & { pipelineId?: string; defectType?: string; severityLevel?: number; minSeverity?: number }): Promise<ApiResult<PageResult<DefectVO>>> {
  return get<PageResult<DefectVO>>('/api/defects', params)
}

// 获取缺陷详情
export function getDefectById(defectId: number): Promise<ApiResult<DefectVO>> {
  return get<DefectVO>(`/api/defects/${defectId}`)
}

// 获取缺陷 GeoJSON 图层数据（GIS 地图用）
export function getDefectsGeoJSON(params: {
  minLng: number
  minLat: number
  maxLng: number
  maxLat: number
  defectType?: string
  minSeverity?: number
}): Promise<ApiResult<GeoJSONFeatureCollection>> {
  return get<GeoJSONFeatureCollection>('/api/defects/geojson', params)
}

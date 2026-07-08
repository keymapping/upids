/**
 * 统一响应结构（与接口文档一致）
 */
export interface ApiResult<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * 分页结果（与接口文档一致）
 */
export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

/**
 * 用户信息（与后端 UserInfoResponse 一致）
 */
export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  role: 'admin' | 'user';
  status: number;
  createdAt?: string;
}

/**
 * 登录响应（与接口文档 §3 一致）
 */
export interface LoginVO {
  token: string;
  expiresIn: number;
  userInfo: UserInfo;
}

/**
 * 登录参数
 */
export interface LoginParams {
  username: string;
  password: string;
}

/**
 * 管线信息（与接口文档 §5 一致）
 */
export interface PipelineVO {
  pipelineId: string;
  pipelineName: string;
  materialType: string;
  diameter: number;
  installTime: string;
  regionCode: string;
  status: number;
  defectCount?: number;
  createdAt: string;
}

/**
 * 管线详情（含空间数据）
 */
export interface PipelineDetailVO extends PipelineVO {
  geometry: {
    type: 'LineString';
    coordinates: number[][];
  };
}

/**
 * 缺陷信息（与接口文档 §8 一致）
 */
export interface DefectVO {
  defectId: number;
  recordId: number;
  pipelineId: string;
  defectType: 'none' | 'crack' | 'corrosion' | 'fracture';
  severityLevel: number;
  confidenceScore: number;
  source: 'rule' | 'ai' | 'fusion';
  location?: { lng: number; lat: number };
  detectedAt: string;
}

/**
 * 识别任务（与接口文档 §7 一致）
 */
export interface TaskVO {
  taskId: number;
  recordId: number;
  pipelineId?: string;
  pipelineName?: string;
  materialType?: string;
  diameter?: number;
  status: 'pending' | 'running' | 'done' | 'failed';
  retryCount: number;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
  createdAtStr?: string;
  startedAtStr?: string;
  finishedAtStr?: string;
  durationSeconds?: number;
  imageName?: string;
  detectionResult?: string;
  confidenceScore?: number;
  defectCount?: number;
  highRiskCount?: number;
}

/**
 * 巡检记录（与接口文档 §6 一致）
 */
export interface InspectionVO {
  recordId: number;
  pipelineId: string;
  imageName: string;
  imagePath?: string;
  imageUrl?: string;
  detectionResult?: string;
  confidenceScore?: number;
  inspectTime: string;
  taskStatus?: string;
}

/**
 * 巡检记录详情
 */
export interface InspectionDetailVO extends InspectionVO {
  task?: TaskVO;
  defects?: DefectVO[];
}

/**
 * 告警信息（与接口文档 §10 一致）
 */
export interface AlertVO {
  alertId: number;
  defectId: number;
  pipelineId: string;
  alertLevel: number;
  alertType: 'threshold' | 'anomaly';
  alertMessage: string;
  isRead: boolean;
  triggeredAt: string;
}

/**
 * 统计概览（与接口文档 §9 一致）
 */
export interface StatisticsOverviewVO {
  summary: {
    pipelineCount: number;
    inspectionCount: number;
    defectCount: number;
    alertCount: number;
    highRiskCount: number;
    unreadAlerts?: number;
    pendingTasks?: number;
    runningTasks?: number;
    completedTasks?: number;
    failedTasks?: number;
  };
  defectByType: Array<{ type: string; count: number }>;
  defectBySeverity: Array<{ level: number; count: number }>;
  defectTrend: Array<{ date: string; count: number }>;
  taskStatusDistribution: Array<{ status: string; count: number }>;
  pipelineByRegion: Array<{ region: string; count: number }>;
  pipelineByMaterial: Array<{ material: string; count: number }>;
  detectionRate: {
    totalInspections: number;
    completedTasks: number;
    failedTasks: number;
    pendingTasks: number;
    runningTasks: number;
    successRate: number;
  };
  recentDefects: Array<{
    defectId: number;
    pipelineId: string;
    defectType: string;
    severityLevel: number;
    confidenceScore: number;
    detectedAt: string;
  }>;
  recentTasks: Array<{
    taskId: number;
    recordId: number;
    status: string;
    pipelineId: string;
    pipelineName?: string;
    detectionResult?: string;
    createdAt: string;
    finishedAt?: string;
  }>;
}

/**
 * 报告信息（与接口文档 §11 一致）
 */
export interface ReportVO {
  reportId: number;
  reportTitle: string;
  regionCode?: string;
  startTime: string;
  endTime: string;
  totalDefects: number;
  highRiskCount: number;
  reportContent?: any;
  createdAt: string;
}

/**
 * 报告生成参数
 */
export interface ReportParams {
  reportTitle: string;
  regionCode?: string;
  startTime: string;
  endTime: string;
}

/**
 * 操作日志（与接口文档 §13 一致）
 */
export interface LogVO {
  logId: number;
  userId: number;
  username?: string;
  module: string;
  operation: string;
  requestUri?: string;
  result: 'success' | 'fail';
  ipAddress?: string;
  createdAt: string;
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  page: number;
  pageSize: number;
  keyword?: string;
  username?: string;
  role?: string;
  status?: number | string;
  startTime?: string;
  endTime?: string;
}

/**
 * 用户创建/更新参数
 */
export interface UserParams {
  username: string;
  password?: string;
  realName?: string;
  role?: 'admin' | 'user';
}

/**
 * 管线导入响应
 */
export interface PipelineImportVO {
  importId: string;
  totalCount: number;
  successCount: number;
  failCount: number;
  failDetails?: Array<{ row: number; reason: string }>;
}

/**
 * 图像上传响应
 */
export interface InspectionUploadVO {
  recordId: number;
  taskId: number;
  imagePath: string;
  taskStatus: string;
}

/**
 * 模拟数据生成响应
 */
export interface MockGenerateVO {
  jobId: string;
  status: string;
  message: string;
  pipelineCount?: number;
  inspectionCount?: number;
  defectCount?: number;
}

/**
 * 模拟数据生成进度
 */
export interface MockProgressVO {
  jobId: string;
  status: string;
  pipelineCount: number;
  inspectionCount: number;
  defectCount: number;
  progress: number;
}

/**
 * 统计信息（供 dashboard 使用）
 */
export interface StatisticsVO {
  pipelineTotal: number;
  pipelineNormal: number;
  pipelineAbnormal: number;
  defectTotal: number;
  defectSerious: number;
  defectModerate: number;
  defectMinor: number;
  taskTotal: number;
  taskPending: number;
  taskInProgress: number;
  taskCompleted: number;
  alertTotal: number;
  alertUnhandled: number;
}

/**
 * 巡检记录参数
 */
export interface InspectionParams {
  pipelineId: string;
  imageName?: string;
  imageUrl?: string;
  detectionResult?: string;
  confidenceScore?: number;
  inspectTime?: string;
}

/**
 * 任务参数
 */
export interface TaskParams {
  recordId?: number;
  pipelineId?: string;
  status?: string;
}

/**
 * 预警配置
 */
export interface AlertConfig {
  severityThreshold: number;
  anomalyWindowDays: number;
  anomalyDefectCount: number;
}

/**
 * GeoJSON FeatureCollection
 */
export interface GeoJSONFeatureCollection {
  type: 'FeatureCollection';
  features: GeoJSONFeature[];
}

export interface GeoJSONFeature {
  type: 'Feature';
  geometry: {
    type: 'LineString' | 'Point';
    coordinates: number[] | number[][];
  };
  properties: Record<string, any>;
}

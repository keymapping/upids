<template>
  <div class="statistics-page">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
          />
        </el-form-item>
        <el-form-item label="区域">
          <el-input
            v-model="filters.regionCode"
            placeholder="区域编码"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="4" :offset="1">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ summary.pipelineCount }}</div>
          <div class="summary-label">管线总数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ summary.inspectionCount }}</div>
          <div class="summary-label">检测次数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ summary.defectCount }}</div>
          <div class="summary-label">缺陷总数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ summary.alertCount }}</div>
          <div class="summary-label">预警数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card highlight">
          <div class="summary-value">{{ summary.highRiskCount }}</div>
          <div class="summary-label">高风险数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 检测率卡片 -->
    <el-row :gutter="16" class="summary-row" style="margin-top: 16px">
      <el-col :span="4" :offset="1">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ detectionRate.completedTasks }}</div>
          <div class="summary-label">已完成任务</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ detectionRate.failedTasks }}</div>
          <div class="summary-label">失败任务</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ detectionRate.runningTasks }}</div>
          <div class="summary-label">运行中任务</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-value">{{ detectionRate.pendingTasks }}</div>
          <div class="summary-label">待处理任务</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="summary-card success">
          <div class="summary-value">{{ detectionRate.successRate }}%</div>
          <div class="summary-label">检测成功率</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <DefectChart
            type="pie"
            :data="defectTypeData"
            title="缺陷类型分布"
            height="380px"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <DefectChart
            type="bar"
            :data="defectSeverityData"
            title="缺陷严重等级分布"
            height="380px"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <DefectChart
            type="line"
            :data="defectTrendData"
            title="缺陷趋势（按日）"
            height="380px"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <DefectChart
            type="ring"
            :data="taskStatusData"
            title="任务状态分布"
            height="380px"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <DefectChart
            type="bar"
            :data="pipelineRegionData"
            title="管线区域分布"
            height="380px"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <DefectChart
            type="pie"
            :data="pipelineMaterialData"
            title="管线材质分布"
            height="380px"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 最新缺陷和任务列表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">最新缺陷</span>
          </template>
          <el-table :data="recentDefects" stripe size="small" max-height="300">
            <el-table-column prop="defectId" label="缺陷ID" width="80" />
            <el-table-column prop="pipelineId" label="管线ID" width="120" />
            <el-table-column label="缺陷类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getDefectTagType(row.defectType)" size="small">
                  {{ defectTypeLabel[row.defectType] || row.defectType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="严重等级" width="80">
              <template #default="{ row }">
                <el-tag :type="getSeverityTagType(row.severityLevel)" size="small">
                  {{ row.severityLevel }}级
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="90">
              <template #default="{ row }">
                {{ row.confidenceScore ? (row.confidenceScore * 100).toFixed(0) + '%' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="detectedAt" label="检测时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.detectedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">最新任务</span>
          </template>
          <el-table :data="recentTasks" stripe size="small" max-height="300">
            <el-table-column prop="taskId" label="任务ID" width="80" />
            <el-table-column prop="pipelineId" label="管线ID" width="120" />
            <el-table-column prop="pipelineName" label="管线名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ statusLabel[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.detectionResult" :type="getDefectTagType(row.detectionResult)" size="small">
                  {{ defectTypeLabel[row.detectionResult] || row.detectionResult }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import DefectChart from '@/components/DefectChart.vue';
import { getStatisticsOverview } from '@/api/statistics';
import type { StatisticsOverviewVO } from '@/types/api';
import { formatDateTime } from '@/utils/format';
import './style.css';

const loading = ref(false);

const filters = reactive({
  dateRange: [] as string[],
  regionCode: '',
});

const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const e = new Date();
      const s = new Date();
      s.setDate(s.getDate() - 7);
      return [s, e];
    },
  },
  {
    text: '最近一月',
    value: () => {
      const e = new Date();
      const s = new Date();
      s.setMonth(s.getMonth() - 1);
      return [s, e];
    },
  },
  {
    text: '最近三月',
    value: () => {
      const e = new Date();
      const s = new Date();
      s.setMonth(s.getMonth() - 3);
      return [s, e];
    },
  },
];

const summary = reactive({
  pipelineCount: 0,
  inspectionCount: 0,
  defectCount: 0,
  alertCount: 0,
  highRiskCount: 0,
});

const detectionRate = reactive({
  totalInspections: 0,
  completedTasks: 0,
  failedTasks: 0,
  pendingTasks: 0,
  runningTasks: 0,
  successRate: 0,
});

const defectTypeData = ref<Array<{ name: string; value: number }>>([]);
const defectSeverityData = ref<Array<{ name: string; value: number }>>([]);
const defectTrendData = ref<Array<{ name: string; value: number }>>([]);
const taskStatusData = ref<Array<{ name: string; value: number }>>([]);
const pipelineRegionData = ref<Array<{ name: string; value: number }>>([]);
const pipelineMaterialData = ref<Array<{ name: string; value: number }>>([]);
const recentDefects = ref<any[]>([]);
const recentTasks = ref<any[]>([]);

const statusLabel: Record<string, string> = {
  pending: '待处理',
  running: '处理中',
  done: '已完成',
  failed: '失败',
};

const defectTypeLabel: Record<string, string> = {
  crack: '裂缝',
  corrosion: '腐蚀',
  fracture: '断裂',
  none: '无缺陷',
  normal: '正常',
};

const severityLabel: Record<number, string> = {
  1: '1级',
  2: '2级',
  3: '3级',
  4: '4级',
  5: '5级',
};

const getStatusTagType = (s: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    running: '',
    done: 'success',
    failed: 'danger',
  };
  return map[s] || 'info';
};

const getDefectTagType = (t: string) => {
  const map: Record<string, string> = {
    crack: 'warning',
    corrosion: 'warning',
    fracture: 'danger',
    none: 'success',
    normal: 'success',
  };
  return map[t] || 'info';
};

const getSeverityTagType = (level: number) => {
  const map: Record<number, string> = {
    1: 'info',
    2: '',
    3: 'warning',
    4: 'danger',
    5: 'danger',
  };
  return map[level] || 'info';
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = {};
    if (filters.dateRange?.length === 2) {
      params.startTime = filters.dateRange[0];
      params.endTime = filters.dateRange[1];
    }
    if (filters.regionCode) {
      params.regionCode = filters.regionCode;
    }

    const res = await getStatisticsOverview(params);
    const data: StatisticsOverviewVO = res.data;

    Object.assign(summary, data.summary);

    if (data.detectionRate) {
      Object.assign(detectionRate, data.detectionRate);
    }

    defectTypeData.value = data.defectByType.map((item) => ({
      name: defectTypeLabel[item.type] || item.type,
      value: item.count,
    }));

    defectSeverityData.value = data.defectBySeverity.map((item) => ({
      name: severityLabel[item.level] || `${item.level}级`,
      value: item.count,
    }));

    defectTrendData.value = data.defectTrend.map((item) => ({
      name: item.date,
      value: item.count,
    }));

    taskStatusData.value = data.taskStatusDistribution.map((item) => ({
      name: statusLabel[item.status] || item.status,
      value: item.count,
    }));

    pipelineRegionData.value = (data.pipelineByRegion || []).map((item) => ({
      name: item.region,
      value: item.count,
    }));

    pipelineMaterialData.value = (data.pipelineByMaterial || []).map((item) => ({
      name: item.material,
      value: item.count,
    }));

    recentDefects.value = data.recentDefects || [];
    recentTasks.value = data.recentTasks || [];
  } catch {
    ElMessage.error('获取统计数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>
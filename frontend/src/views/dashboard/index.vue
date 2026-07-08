<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon pipeline">
              <el-icon :size="32"><Connection /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">管线总数</div>
              <div class="card-value">{{ summary.pipelineCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon inspection">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">检测总数</div>
              <div class="card-value">{{ summary.inspectionCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon defect">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">缺陷总数</div>
              <div class="card-value">{{ summary.defectCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon alert">
              <el-icon :size="32"><Bell /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">未读预警</div>
              <div class="card-value alert-value">{{ summary.unreadAlerts || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行统计卡片 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon pending">
              <el-icon :size="32"><Clock /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">待处理任务</div>
              <div class="card-value pending-value">{{ summary.pendingTasks || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon running">
              <el-icon :size="32"><Loading /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">运行中任务</div>
              <div class="card-value running-value">{{ summary.runningTasks || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon completed">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">已完成任务</div>
              <div class="card-value success-value">{{ summary.completedTasks || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="card-content">
            <div class="card-icon failed">
              <el-icon :size="32"><CircleClose /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">失败任务</div>
              <div class="card-value danger-value">{{ summary.failedTasks || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表和列表区域 -->
    <el-row :gutter="16" class="content-row">
      <!-- 缺陷趋势图 -->
      <el-col :span="14">
        <el-card shadow="hover" class="trend-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">缺陷趋势</span>
              <el-tag type="info" size="small">近30天</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" class="trend-chart" />
        </el-card>
      </el-col>

      <!-- 最新预警 -->
      <el-col :span="10">
        <el-card shadow="hover" class="list-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最新预警</span>
              <el-button type="primary" link @click="router.push('/alert')">查看全部</el-button>
            </div>
          </template>
          <div class="recent-list">
            <div v-for="item in recentAlerts" :key="item.alertId" class="list-item">
              <div class="item-left">
                <el-tag :type="getAlertLevelType(item.alertLevel)" size="small" class="level-tag">
                  {{ getAlertLevelLabel(item.alertLevel) }}
                </el-tag>
                <span class="item-text" :title="item.alertMessage">{{ item.alertMessage }}</span>
              </div>
              <span class="item-time">{{ formatRelativeTime(item.triggeredAt) }}</span>
            </div>
            <el-empty v-if="recentAlerts.length === 0" description="暂无预警" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 最新任务 -->
      <el-col :span="14">
        <el-card shadow="hover" class="list-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最新任务</span>
              <el-button type="primary" link @click="router.push('/task')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentTasks" stripe size="small" max-height="260">
            <el-table-column prop="taskId" label="任务ID" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="getTaskStatusType(row.status)" size="small">
                  {{ getTaskStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="pipelineName" label="管线名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 快捷操作 -->
      <el-col :span="10">
        <el-card shadow="hover" class="quick-card">
          <template #header>
            <span class="card-title">快捷操作</span>
          </template>
          <div class="quick-actions">
            <div class="action-item" @click="router.push('/pipeline')">
              <el-icon :size="28" color="#409eff"><Connection /></el-icon>
              <span>管线管理</span>
            </div>
            <div class="action-item" @click="router.push('/inspection')">
              <el-icon :size="28" color="#67c23a"><Document /></el-icon>
              <span>巡检管理</span>
            </div>
            <div class="action-item" @click="router.push('/gis')">
              <el-icon :size="28" color="#e6a23c"><MapLocation /></el-icon>
              <span>GIS地图</span>
            </div>
            <div class="action-item" @click="router.push('/statistics')">
              <el-icon :size="28" color="#909399"><DataAnalysis /></el-icon>
              <span>数据分析</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { Connection, Document, Warning, Bell, MapLocation, DataAnalysis, Clock, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { getStatisticsOverview } from '@/api/statistics';
import { getAlertList } from '@/api/alert';
import { getTaskList } from '@/api/task';
import type { AlertVO, TaskVO } from '@/types/api';
import { formatDateTime, formatRelativeTime } from '@/utils/format';
import './style.css';

const router = useRouter();

const summary = reactive({
  pipelineCount: 0,
  inspectionCount: 0,
  defectCount: 0,
  alertCount: 0,
  unreadAlerts: 0,
  pendingTasks: 0,
  runningTasks: 0,
  completedTasks: 0,
  failedTasks: 0,
});

const recentAlerts = ref<AlertVO[]>([]);
const recentTasks = ref<TaskVO[]>([]);

const trendChartRef = ref<HTMLElement>();
let trendChart: echarts.ECharts | null = null;

const getAlertLevelType = (level: number) => {
  const map: Record<number, string> = {
    1: 'info',
    2: '',
    3: 'warning',
    4: 'danger',
    5: 'danger',
  };
  return (map[level] || 'info') as any;
};

const getAlertLevelLabel = (level: number) => {
  const map: Record<number, string> = {
    1: '低',
    2: '中低',
    3: '中',
    4: '高',
    5: '极高',
  };
  return map[level] || `${level}级`;
};

const getTaskStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'info',
    running: '',
    done: 'success',
    failed: 'danger',
  };
  return (map[status] || 'info') as any;
};

const getTaskStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    running: '运行中',
    done: '已完成',
    failed: '失败',
  };
  return map[status] || status;
};

const initTrendChart = (data: Array<{ date: string; count: number }>) => {
  if (!trendChartRef.value) return;
  trendChart = echarts.init(trendChartRef.value);
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((d) => d.date),
      axisLabel: { fontSize: 11 },
    },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    series: [
      {
        type: 'line',
        data: data.map((d) => d.count),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#667eea', width: 2 },
        itemStyle: { color: '#667eea' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102,126,234,0.35)' },
            { offset: 1, color: 'rgba(102,126,234,0.05)' },
          ]),
        },
      },
    ],
  });
};

const handleResize = () => {
  trendChart?.resize();
};

const fetchData = async () => {
  try {
    const [statsRes, alertsRes, tasksRes] = await Promise.all([
      getStatisticsOverview(),
      getAlertList({ page: 1, pageSize: 5 }),
      getTaskList({ page: 1, pageSize: 5 }),
    ]);

    const statsData = statsRes.data;
    Object.assign(summary, statsData.summary);

    await nextTick();
    initTrendChart(statsData.defectTrend);

    recentAlerts.value = alertsRes.data.list || [];
    recentTasks.value = tasksRes.data.list || [];
  } catch (error) {
    console.error('获取首页数据失败:', error);
  }
};

onMounted(() => {
  fetchData();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  trendChart?.dispose();
  window.removeEventListener('resize', handleResize);
});
</script>

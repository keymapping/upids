<template>
  <div class="page-container">
    <el-card shadow="hover">
      <!-- 搜索条件 -->
      <div class="filter-section">
        <el-select
          v-model="statusFilter"
          placeholder="状态筛选"
          clearable
          style="width: 120px"
          @change="handleSearch"
        >
          <el-option label="待处理" value="pending" />
          <el-option label="运行中" value="running" />
          <el-option label="已完成" value="done" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-select
          v-model="detectionResultFilter"
          placeholder="检测结果"
          clearable
          style="width: 120px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="无缺陷" value="none" />
          <el-option label="正常" value="normal" />
          <el-option label="裂缝" value="crack" />
          <el-option label="腐蚀" value="corrosion" />
          <el-option label="断裂" value="fracture" />
        </el-select>
        <el-input
          v-model="pipelineNameFilter"
          placeholder="管线名称"
          clearable
          style="width: 160px; margin-left: 10px"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="dateRangeFilter"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px; margin-left: 10px"
          @change="handleSearch"
        />
        <el-button
          type="primary"
          style="margin-left: 10px"
          @click="handleSearch"
        >
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-tag
          v-if="hasActiveTasks"
          type="warning"
          style="margin-left: auto"
        >
          <el-icon class="is-loading"><Loading /></el-icon>
          有任务正在处理中，自动刷新中...
        </el-tag>
      </div>

      <!-- 表格 -->
      <div class="table-wrapper">
        <el-table
          :data="taskList"
          v-loading="loading"
          stripe
          border
          style="width: 100%"
          :height="tableHeight"
        >
          <el-table-column
            prop="taskId"
            label="任务ID"
            width="80"
            align="center"
          />
          <el-table-column
            prop="recordId"
            label="记录ID"
            width="80"
            align="center"
          />
          <el-table-column
            prop="pipelineName"
            label="管线名称"
            min-width="120"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.pipelineName || row.pipelineId || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            prop="status"
            label="状态"
            width="120"
            align="center"
          >
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="180" align="center">
            <template #default="{ row }">
              <el-progress
                v-if="row.status === 'pending'"
                :percentage="0"
                :stroke-width="14"
                status="warning"
                :format="() => '排队中...'"
              />
              <el-progress
                v-else-if="row.status === 'running'"
                :percentage="60"
                :stroke-width="14"
                :indeterminate="true"
                status=""
                :format="() => '识别中...'"
              />
              <el-progress
                v-else-if="row.status === 'done'"
                :percentage="100"
                :stroke-width="14"
                status="success"
                :format="() => '完成'"
              />
              <el-progress
                v-else-if="row.status === 'failed'"
                :percentage="100"
                :stroke-width="14"
                status="exception"
                :format="() => '失败'"
              />
            </template>
          </el-table-column>
          <el-table-column
            prop="retryCount"
            label="重试次数"
            width="90"
            align="center"
          />
          <el-table-column
            prop="imageName"
            label="检测图片"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column
            prop="detectionResult"
            label="检测结果"
            width="100"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                v-if="row.detectionResult"
                :type="resultTagType(row.detectionResult)"
                size="small"
              >
                {{ resultLabel(row.detectionResult) }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="confidenceScore"
            label="置信度"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              {{ row.confidenceScore ? (row.confidenceScore * 100).toFixed(0) + '%' : '-' }}
            </template>
          </el-table-column>
          <el-table-column
            prop="defectCount"
            label="缺陷数"
            width="80"
            align="center"
          >
            <template #default="{ row }">
              <span :class="{ 'text-danger': (row.defectCount || 0) > 0 }">
                {{ row.defectCount || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            prop="highRiskCount"
            label="高风险"
            width="80"
            align="center"
          >
            <template #default="{ row }">
              <span :class="{ 'text-danger': (row.highRiskCount || 0) > 0 }">
                {{ row.highRiskCount || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            prop="durationSeconds"
            label="耗时"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              {{ formatDuration(row.durationSeconds) }}
            </template>
          </el-table-column>
          <el-table-column
            prop="errorMessage"
            label="错误信息"
            min-width="180"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span :class="{ 'text-danger': row.errorMessage }">
                {{ row.errorMessage || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{
              formatDateTime(row.createdAt)
            }}</template>
          </el-table-column>
          <el-table-column prop="startedAt" label="开始时间" width="170">
            <template #default="{ row }">{{
              formatDateTime(row.startedAt)
            }}</template>
          </el-table-column>
          <el-table-column prop="finishedAt" label="结束时间" width="170">
            <template #default="{ row }">{{
              formatDateTime(row.finishedAt)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleView(row)"
                >详情</el-button
              >
              <el-button
                v-if="row.status === 'failed'"
                type="warning"
                link
                @click="handleRetry(row)"
              >
                重试
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        class="pagination"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="任务详情" width="760px">
      <el-descriptions :column="3" border v-if="currentTask">
        <el-descriptions-item label="任务ID">{{
          currentTask.taskId
        }}</el-descriptions-item>
        <el-descriptions-item label="记录ID">{{
          currentTask.recordId
        }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentTask.status)">{{
            statusLabel(currentTask.status)
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="管线名称">{{
          currentTask.pipelineName || currentTask.pipelineId || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="管线ID">{{
          currentTask.pipelineId || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="材质">{{
          currentTask.materialType || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="管径(mm)">{{
          currentTask.diameter || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="检测图片">{{
          currentTask.imageName || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="检测结果">
          <el-tag
            v-if="currentTask.detectionResult"
            :type="resultTagType(currentTask.detectionResult)"
          >
            {{ resultLabel(currentTask.detectionResult) }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="置信度">{{
          currentTask.confidenceScore
            ? (currentTask.confidenceScore * 100).toFixed(1) + '%'
            : '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="缺陷数量">
          <span :class="{ 'text-danger font-bold': (currentTask.defectCount || 0) > 0 }">
            {{ currentTask.defectCount || 0 }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="高风险数">
          <span :class="{ 'text-danger font-bold': (currentTask.highRiskCount || 0) > 0 }">
            {{ currentTask.highRiskCount || 0 }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="重试次数">{{
          currentTask.retryCount
        }}</el-descriptions-item>
        <el-descriptions-item label="处理耗时">{{
          formatDuration(currentTask.durationSeconds)
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          formatDateTime(currentTask.createdAt)
        }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{
          formatDateTime(currentTask.startedAt) || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{
          formatDateTime(currentTask.finishedAt) || '-'
        }}</el-descriptions-item>
      </el-descriptions>
      
      <template v-if="currentTask && currentTask.errorMessage">
        <el-divider />
        <div class="error-section">
          <h5 style="margin-bottom: 8px; font-size: 14px; font-weight: 600">错误详情</h5>
          <el-alert type="error" :title="currentTask.errorMessage" show-icon :closable="false" />
        </div>
      </template>
      
      <el-divider />
      <div class="dialog-actions" v-if="currentTask">
        <el-button type="primary" @click="goToInspection(currentTask.recordId)">
          <el-icon><ArrowRight /></el-icon>
          查看巡检记录
        </el-button>
        <el-button @click="goToPipeline(currentTask.pipelineId)">
          <el-icon><Search /></el-icon>
          查看管线详情
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Loading, ArrowRight } from '@element-plus/icons-vue';
import { getTaskList, getTaskById, retryTask } from '@/api/task';
import type { TaskVO } from '@/types/api';
import { useTableHeight } from '@/composables/useTableHeight';
import { formatDateTime } from '@/utils/format';
import './style.css';
const { tableHeight, updateHeight } = useTableHeight('.page-container');
const router = useRouter();

const loading = ref(false);
const taskList = ref<TaskVO[]>([]);
const statusFilter = ref('');
const detectionResultFilter = ref('');
const pipelineNameFilter = ref('');
const dateRangeFilter = ref<[string, string] | null>(null);
const pageNum = ref(1);
const pageSize = ref(20);
const total = ref(0);

const detailDialogVisible = ref(false);
const currentTask = ref<TaskVO | null>(null);

let refreshTimer: ReturnType<typeof setInterval> | null = null;

const hasActiveTasks = computed(() =>
  taskList.value.some((t) => t.status === 'pending' || t.status === 'running')
);

const statusLabel = (s: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    running: '运行中',
    done: '已完成',
    failed: '失败',
  };
  return map[s] || s;
};

const statusTagType = (s: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    running: '',
    done: 'success',
    failed: 'danger',
  };
  return (map[s] || 'info') as any;
};

const resultLabel = (r: string) => {
  const map: Record<string, string> = {
    none: '无缺陷',
    crack: '裂缝',
    corrosion: '腐蚀',
    fracture: '断裂',
    normal: '正常',
  };
  return map[r] || r;
};

const resultTagType = (r: string) => {
  const map: Record<string, string> = {
    none: 'success',
    crack: 'warning',
    corrosion: 'warning',
    fracture: 'danger',
    normal: 'success',
  };
  return (map[r] || 'info') as any;
};

const formatDuration = (seconds?: number) => {
  if (!seconds || seconds < 0) return '-';
  if (seconds < 60) return seconds + '秒';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return mins + '分' + secs + '秒';
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pageNum.value,
      pageSize: pageSize.value,
      status: statusFilter.value || undefined,
      pipelineName: pipelineNameFilter.value || undefined,
      detectionResult: detectionResultFilter.value || undefined,
    };
    if (dateRangeFilter.value) {
      params.startTime = dateRangeFilter.value[0];
      params.endTime = dateRangeFilter.value[1];
    }
    const res = await getTaskList(params);
    taskList.value = res.data.list;
    total.value = res.data.total;

    startAutoRefresh();
  } catch {
    ElMessage.error('获取任务列表失败');
  } finally {
    loading.value = false;
  }
};

const startAutoRefresh = () => {
  stopAutoRefresh();
  if (hasActiveTasks.value) {
    refreshTimer = setInterval(() => {
      fetchList();
    }, 3000);
  }
};

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchList();
};

const handleReset = () => {
  statusFilter.value = '';
  detectionResultFilter.value = '';
  pipelineNameFilter.value = '';
  dateRangeFilter.value = null;
  handleSearch();
};

const handleView = async (row: TaskVO) => {
  try {
    const res = await getTaskById(row.taskId);
    currentTask.value = res.data;
  } catch {
    currentTask.value = row;
  }
  detailDialogVisible.value = true;
};

const handleRetry = (row: TaskVO) => {
  ElMessageBox.confirm(`确定重试任务 #${row.taskId} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await retryTask(row.taskId);
        ElMessage.success('任务已重新提交');
        fetchList();
      } catch {
        ElMessage.error('重试失败');
      }
    })
    .catch(() => {});
};

const goToInspection = (recordId?: number) => {
  if (recordId) {
    router.push(`/inspection/detail/${recordId}`);
    detailDialogVisible.value = false;
  }
};

const goToPipeline = (pipelineId?: string) => {
  if (pipelineId) {
    router.push(`/pipeline/detail/${pipelineId}`);
    detailDialogVisible.value = false;
  }
};

onMounted(() => {
  fetchList();
  setTimeout(updateHeight, 100);
});

onBeforeUnmount(() => {
  stopAutoRefresh();
});
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-badge
              :value="alertStore.unreadCount"
              :hidden="alertStore.unreadCount === 0"
              class="unread-badge"
            >
              <el-tag type="danger" size="small">未读</el-tag>
            </el-badge>
          </div>
          <div class="header-right">
            <el-button
              type="primary"
              plain
              size="small"
              @click="markAllAsRead"
              :disabled="alertStore.unreadCount === 0"
            >
              全部已读
            </el-button>
            <el-button type="primary" size="small" @click="goToGis">
              <el-icon><Location /></el-icon>
              跳转GIS
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选栏 -->
      <el-form
        :inline="true"
        :model="filters"
        class="filter-section"
        size="default"
      >
        <el-form-item label="已读状态">
          <el-select
            v-model="filters.isRead"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option label="未读" :value="false" />
            <el-option label="已读" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警类型">
          <el-select
            v-model="filters.alertType"
            placeholder="全部类型"
            clearable
            style="width: 150px"
          >
            <el-option label="阈值预警" value="threshold" />
            <el-option label="异常预警" value="anomaly" />
          </el-select>
        </el-form-item>
        <el-form-item label="管线">
          <el-input
            v-model="filters.pipelineId"
            placeholder="管线编号"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="最低等级">
          <el-input-number
            v-model="filters.minLevel"
            :min="1"
            :max="5"
            controls-position="right"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <div class="table-wrapper">
        <el-table
          :data="alertList"
          v-loading="loading"
          stripe
          border
          row-key="alertId"
          style="width: 100%"
          :height="tableHeight"
        >
          <el-table-column prop="alertId" label="预警ID" width="80" />
          <el-table-column label="预警类型" width="120">
            <template #default="{ row }">
              <el-tag
                :type="row.alertType === 'threshold' ? 'danger' : 'warning'"
                size="small"
              >
                {{ row.alertType === 'threshold' ? '阈值预警' : '异常预警' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="等级" width="80">
            <template #default="{ row }">
              <el-tag
                :type="getLevelTagType(row.alertLevel)"
                :effect="row.alertLevel >= 5 ? 'dark' : 'light'"
                size="small"
              >
                {{ getLevelLabel(row.alertLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pipelineId" label="管线编号" width="140" />
          <el-table-column
            prop="alertMessage"
            label="预警内容"
            min-width="200"
            show-overflow-tooltip
          />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isRead ? 'info' : 'danger'" size="small">
                {{ row.isRead ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="triggeredAt" label="触发时间" width="180">
            <template #default="{ row }">{{
              formatDateTime(row.triggeredAt)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="!row.isRead"
                type="primary"
                link
                size="small"
                @click="handleMarkRead(row)"
              >
                标记已读
              </el-button>
              <el-button
                type="info"
                link
                size="small"
                @click="handleViewDetail(row)"
              >
                详情
              </el-button>
              <el-button
                type="success"
                link
                size="small"
                @click="goToDefect(row)"
              >
                查看缺陷
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        class="pagination"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预警详情" width="480px">
      <template v-if="selectedAlert">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="预警ID">{{
            selectedAlert.alertId
          }}</el-descriptions-item>
          <el-descriptions-item label="关联缺陷ID">{{
            selectedAlert.defectId
          }}</el-descriptions-item>
          <el-descriptions-item label="管线编号">{{
            selectedAlert.pipelineId
          }}</el-descriptions-item>
          <el-descriptions-item label="预警类型">
            {{
              selectedAlert.alertType === 'threshold' ? '阈值预警' : '异常预警'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="预警等级"
            >{{ selectedAlert.alertLevel }}级</el-descriptions-item
          >
          <el-descriptions-item label="预警内容">{{
            selectedAlert.alertMessage
          }}</el-descriptions-item>
          <el-descriptions-item label="是否已读">
            <el-tag
              :type="selectedAlert.isRead ? 'success' : 'danger'"
              size="small"
            >
              {{ selectedAlert.isRead ? '已读' : '未读' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发时间">{{
            selectedAlert.triggeredAt
          }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, Location } from '@element-plus/icons-vue';
import {
  getAlertList,
  markAlertAsRead,
  markAllAlertsAsRead,
} from '@/api/alert';
import { useAlertStore } from '@/stores/alert';
import type { AlertVO } from '@/types/api';
import { useTableHeight } from '@/composables/useTableHeight';
import { formatDateTime } from '@/utils/format';
import './style.css';
const { tableHeight, updateHeight } = useTableHeight('.page-container');

const router = useRouter();
const alertStore = useAlertStore();

const loading = ref(false);
const alertList = ref<AlertVO[]>([]);

const filters = reactive({
  isRead: undefined as boolean | undefined,
  alertType: '',
  pipelineId: '',
  minLevel: 1,
  dateRange: null as [string, string] | null,
});

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
});

const detailVisible = ref(false);
const selectedAlert = ref<AlertVO | null>(null);

const getLevelTagType = (level: number) => {
  const map: Record<number, string> = {
    1: 'info', // 低
    2: '', // 中低 - 默认蓝色
    3: 'warning', // 中
    4: 'danger', // 高
    5: 'danger', // 极高
  };
  return (map[level] || 'info') as any;
};

const getLevelLabel = (level: number) => {
  const map: Record<number, string> = {
    1: '低',
    2: '中低',
    3: '中',
    4: '高',
    5: '极高',
  };
  return map[level] || `${level}级`;
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    };
    if (filters.isRead !== undefined && filters.isRead !== null)
      params.isRead = filters.isRead;
    if (filters.alertType) params.alertType = filters.alertType;
    if (filters.pipelineId) params.pipelineId = filters.pipelineId;
    if (filters.minLevel > 1) params.minLevel = filters.minLevel;
    if (filters.dateRange) {
      params.startTime = filters.dateRange[0];
      params.endTime = filters.dateRange[1];
    }

    const res = await getAlertList(params);
    alertList.value = res.data.list;
    pagination.total = res.data.total;

    alertStore.fetchUnreadCount();
  } catch {
    ElMessage.error('获取预警列表失败');
  } finally {
    loading.value = false;
  }
};

const handleReset = () => {
  filters.isRead = undefined;
  filters.alertType = '';
  filters.pipelineId = '';
  filters.minLevel = 1;
  filters.dateRange = null;
  pagination.page = 1;
  fetchList();
};

const handleMarkRead = async (row: AlertVO) => {
  try {
    await markAlertAsRead(row.alertId);
    row.isRead = true;
    alertStore.unreadCount = Math.max(0, alertStore.unreadCount - 1);
    ElMessage.success('已标记为已读');
  } catch {
    ElMessage.error('操作失败');
  }
};

const markAllAsRead = async () => {
  try {
    await markAllAlertsAsRead();
    alertList.value.forEach((a) => (a.isRead = true));
    alertStore.unreadCount = 0;
    ElMessage.success('已全部标记已读');
  } catch {
    ElMessage.error('操作失败');
  }
};

const handleViewDetail = (row: AlertVO) => {
  selectedAlert.value = row;
  detailVisible.value = true;
};

const goToGis = () => {
  router.push('/gis');
};

const goToDefect = (row: AlertVO) => {
  if (row.defectId) {
    router.push(`/inspection/detail/${row.defectId}`);
  } else {
    ElMessage.warning('该预警未关联缺陷信息');
  }
};

onMounted(() => {
  fetchList();
  alertStore.fetchUnreadCount();
  setTimeout(updateHeight, 100);
});
</script>

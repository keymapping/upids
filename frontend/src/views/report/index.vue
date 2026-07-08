<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="page-title">报告管理</span>
          <el-button type="primary" @click="showGenerateDialog">
            <el-icon><DocumentAdd /></el-icon>
            生成报告
          </el-button>
        </div>
      </template>

      <!-- 搜索条件 -->
      <div class="filter-section">
        <el-input
          v-model="searchForm.keyword"
          placeholder="报告标题"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="searchForm.regionCode"
          placeholder="区域编码"
          clearable
          style="width: 140px; margin-left: 10px"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="searchForm.dateRange"
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
      </div>

      <!-- 表格 -->
      <div class="table-wrapper">
        <el-table
          :data="reportList"
          v-loading="loading"
          stripe
          border
          style="width: 100%"
          :height="tableHeight"
        >
          <el-table-column prop="reportId" label="报告ID" width="80" />
          <el-table-column
            prop="reportTitle"
            label="报告标题"
            min-width="200"
            show-overflow-tooltip
          />
          <el-table-column prop="regionCode" label="区域" width="120">
            <template #default="{ row }">
              {{ row.regionCode || '全局' }}
            </template>
          </el-table-column>
          <el-table-column label="时间范围" width="220">
            <template #default="{ row }">
              {{ row.startTime }} ~ {{ row.endTime }}
            </template>
          </el-table-column>
          <el-table-column prop="totalDefects" label="缺陷总数" width="100" />
          <el-table-column prop="highRiskCount" label="高风险" width="80">
            <template #default="{ row }">
              <el-tag type="danger" size="small" v-if="row.highRiskCount > 0">{{
                row.highRiskCount
              }}</el-tag>
              <span v-else>0</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="生成时间" width="180">
            <template #default="{ row }">{{
              formatDateTime(row.createdAt)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleViewDetail(row)"
                >查看</el-button
              >
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

    <!-- 生成报告弹窗 -->
    <el-dialog v-model="generateVisible" title="生成风险报告" width="520px">
      <el-form
        :model="generateForm"
        :rules="generateRules"
        ref="generateFormRef"
        label-width="100px"
      >
        <el-form-item label="报告标题" prop="reportTitle">
          <el-input
            v-model="generateForm.reportTitle"
            placeholder="请输入报告标题"
          />
        </el-form-item>
        <el-form-item label="区域编码" prop="regionCode">
          <el-input
            v-model="generateForm.regionCode"
            placeholder="留空则为全局报告"
            clearable
          />
        </el-form-item>
        <el-form-item label="时间范围" prop="dateRange">
          <el-date-picker
            v-model="generateForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate"
          >确认生成</el-button
        >
      </template>
    </el-dialog>

    <!-- 报告详情弹窗 -->
    <el-dialog v-model="detailVisible" title="报告详情" width="720px">
      <template v-if="selectedReport">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="报告ID">{{
            selectedReport.reportId
          }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{
            selectedReport.reportTitle
          }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{
            selectedReport.regionCode || '全局'
          }}</el-descriptions-item>
          <el-descriptions-item label="时间范围"
            >{{ selectedReport.startTime }} ~
            {{ selectedReport.endTime }}</el-descriptions-item
          >
          <el-descriptions-item label="生成时间">{{
            formatDateTime(selectedReport.createdAt)
          }}</el-descriptions-item>
          <el-descriptions-item label="缺陷总数">
            <span class="text-danger font-bold">{{ selectedReport.totalDefects }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="高风险" :span="2">
            <span class="text-danger font-bold">{{ selectedReport.reportContent?.highRiskCount || selectedReport.highRiskCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="中风险">
            <span class="text-warning font-bold">{{ selectedReport.reportContent?.mediumRiskCount || 0 }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="低风险">
            <span class="text-info font-bold">{{ selectedReport.reportContent?.lowRiskCount || 0 }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="检测率">
            <span class="font-bold">{{ selectedReport.reportContent?.defectRate || 0 }}%</span>
          </el-descriptions-item>
          <el-descriptions-item label="检测次数">{{
            selectedReport.reportContent?.totalInspections || 0
          }}</el-descriptions-item>
          <el-descriptions-item label="成功任务">{{
            selectedReport.reportContent?.completedTasks || 0
          }}</el-descriptions-item>
          <el-descriptions-item label="失败任务">
            <span class="text-danger">{{ selectedReport.reportContent?.failedTasks || 0 }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="selectedReport.reportContent">
          <el-divider />
          <h4 style="margin-bottom: 12px">报告内容</h4>

          <el-row :gutter="16" style="margin-bottom: 16px">
            <el-col :span="12">
              <div class="report-section">
                <h5 style="margin-bottom: 8px">缺陷类型分布</h5>
                <el-table
                  :data="defectTypeTableData"
                  size="small"
                  border
                >
                  <el-table-column prop="type" label="类型" />
                  <el-table-column prop="count" label="数量" />
                </el-table>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="report-section">
                <h5 style="margin-bottom: 8px">缺陷等级分布</h5>
                <el-table
                  :data="defectSeverityTableData"
                  size="small"
                  border
                >
                  <el-table-column prop="level" label="等级" />
                  <el-table-column prop="count" label="数量" />
                </el-table>
              </div>
            </el-col>
          </el-row>

          <template
            v-if="selectedReport.reportContent.topRiskPipelines?.length"
          >
            <div class="report-section">
              <h5 style="margin-bottom: 8px">高风险管线</h5>
              <el-table
                :data="selectedReport.reportContent.topRiskPipelines"
                size="small"
                border
                style="margin-bottom: 12px"
              >
                <el-table-column prop="pipelineId" label="管线编号" />
                <el-table-column prop="defectCount" label="缺陷数" />
                <el-table-column label="最高等级">
                  <template #default="{ row }">
                    <el-tag :type="getSeverityTagType(row.maxSeverity)" size="small">
                      {{ row.maxSeverity }}级
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <template v-if="selectedReport.reportContent.defectsByRegion && Object.keys(selectedReport.reportContent.defectsByRegion).length">
            <div class="report-section">
              <h5 style="margin-bottom: 8px">区域缺陷分布</h5>
              <el-table
                :data="defectsByRegionTableData"
                size="small"
                border
                style="margin-bottom: 12px"
              >
                <el-table-column prop="region" label="区域" />
                <el-table-column prop="count" label="缺陷数" />
              </el-table>
            </div>
          </template>

          <template v-if="selectedReport.reportContent.defectDetails?.length">
            <div class="report-section">
              <h5 style="margin-bottom: 8px">缺陷详情</h5>
              <el-table
                :data="selectedReport.reportContent.defectDetails"
                size="small"
                border
                style="margin-bottom: 12px"
                max-height="200"
              >
                <el-table-column prop="defectId" label="ID" width="60" />
                <el-table-column prop="pipelineId" label="管线" width="100" />
                <el-table-column label="类型" width="80">
                  <template #default="{ row }">
                    <el-tag :type="getDefectTagType(row.defectType)" size="small">
                      {{ defectTypeLabel(row.defectType) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="等级" width="60">
                  <template #default="{ row }">
                    <el-tag :type="getSeverityTagType(row.severityLevel)" size="small">
                      {{ row.severityLevel }}级
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="置信度" width="80">
                  <template #default="{ row }">
                    {{ row.confidenceScore ? (row.confidenceScore * 100).toFixed(0) + '%' : '-' }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>

          <template v-if="selectedReport.reportContent.recommendations?.length">
            <div class="report-section">
              <h5 style="margin-bottom: 8px">建议措施</h5>
              <ul style="padding-left: 20px; margin: 0">
                <li
                  v-for="(rec, i) in selectedReport.reportContent.recommendations"
                  :key="i"
                  style="margin-bottom: 6px; font-size: 14px; color: #606266"
                >
                  <span class="text-primary">•</span> {{ rec }}
                </li>
              </ul>
            </div>
          </template>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { DocumentAdd, Search } from '@element-plus/icons-vue';
import { getReportList, generateReport } from '@/api/report';
import type { ReportVO } from '@/types/api';
import { useTableHeight } from '@/composables/useTableHeight';
import { formatDateTime } from '@/utils/format';
import './style.css';
const { tableHeight, updateHeight } = useTableHeight('.page-container');

const loading = ref(false);
const reportList = ref<ReportVO[]>([]);

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
});

const searchForm = reactive({
  keyword: '',
  regionCode: '',
  dateRange: null as [string, string] | null,
});

const generateVisible = ref(false);
const generating = ref(false);
const generateFormRef = ref<FormInstance>();
const generateForm = reactive({
  reportTitle: '',
  regionCode: '',
  dateRange: [] as string[],
});

const generateRules: FormRules = {
  reportTitle: [{ required: true, message: '请输入报告标题', trigger: 'blur' }],
  dateRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }],
};

const detailVisible = ref(false);
const selectedReport = ref<ReportVO | null>(null);

const defectTypeTableData = computed(() => {
  if (!selectedReport.value?.reportContent?.defectsByType) return [];
  const types = selectedReport.value.reportContent.defectsByType as Record<string, number>;
  const labelMap: Record<string, string> = {
    crack: '裂缝',
    corrosion: '腐蚀',
    fracture: '断裂',
    unknown: '未知',
  };
  return Object.entries(types).map(([type, count]) => ({
    type: labelMap[type] || type,
    count,
  }));
});

const defectSeverityTableData = computed(() => {
  if (!selectedReport.value?.reportContent?.defectsBySeverity) return [];
  const levels = selectedReport.value.reportContent.defectsBySeverity as Record<number, number>;
  return Object.entries(levels).map(([level, count]) => ({
    level: `${level}级`,
    count,
  }));
});

const defectsByRegionTableData = computed(() => {
  if (!selectedReport.value?.reportContent?.defectsByRegion) return [];
  const regions = selectedReport.value.reportContent.defectsByRegion as Record<string, number>;
  return Object.entries(regions).map(([region, count]) => ({
    region: region === '未知' ? '未分配' : region,
    count,
  }));
});

const defectTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    crack: '裂缝',
    corrosion: '腐蚀',
    fracture: '断裂',
    none: '无缺陷',
    unknown: '未知',
  };
  return map[type] || type;
};

const getDefectTagType = (type: string) => {
  const map: Record<string, string> = {
    crack: 'warning',
    corrosion: 'warning',
    fracture: 'danger',
    none: 'success',
  };
  return map[type] || 'info';
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

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      regionCode: searchForm.regionCode || undefined,
    };
    if (searchForm.dateRange) {
      params.startTime = searchForm.dateRange[0];
      params.endTime = searchForm.dateRange[1];
    }
    const res = await getReportList(params);
    const data = res.data;
    if (Array.isArray(data)) {
      reportList.value = data;
      pagination.total = data.length;
    } else {
      reportList.value = data.list;
      pagination.total = data.total;
    }
  } catch {
    ElMessage.error('获取报告列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  fetchList();
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.regionCode = '';
  searchForm.dateRange = null;
  handleSearch();
};

const showGenerateDialog = () => {
  generateForm.reportTitle = '';
  generateForm.regionCode = '';
  generateForm.dateRange = [];
  generateVisible.value = true;
};

const handleGenerate = async () => {
  if (!generateFormRef.value) return;
  await generateFormRef.value.validate();

  if (!generateForm.dateRange?.length) return;

  generating.value = true;
  try {
    await generateReport({
      reportTitle: generateForm.reportTitle,
      regionCode: generateForm.regionCode || undefined,
      startTime: generateForm.dateRange[0] + ' 00:00:00',
      endTime: generateForm.dateRange[1] + ' 23:59:59',
    });
    ElMessage.success('报告生成成功');
    generateVisible.value = false;
    fetchList();
  } catch {
    ElMessage.error('报告生成失败');
  } finally {
    generating.value = false;
  }
};

const handleViewDetail = (row: ReportVO) => {
  selectedReport.value = row;
  detailVisible.value = true;
};

onMounted(() => {
  fetchList();
  setTimeout(updateHeight, 100);
});
</script>

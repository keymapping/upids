<template>
  <div class="page-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-button
            v-if="userStore.hasRole(['admin'])"
            type="primary"
            @click="uploadDialogVisible = true"
          >
            <el-icon><Upload /></el-icon>
            上传图像
          </el-button>
        </div>
      </template>

      <!-- 搜索条件 -->
      <div class="filter-section">
        <el-input
          v-model="searchForm.keyword"
          placeholder="管线ID"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.detectionResult"
          placeholder="检测结果"
          clearable
          style="width: 160px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="正常" value="normal" />
          <el-option label="裂缝" value="crack" />
          <el-option label="腐蚀" value="corrosion" />
          <el-option label="断裂" value="fracture" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px; margin-left: 10px"
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
          :data="inspectionList"
          v-loading="loading"
          stripe
          border
          style="width: 100%"
          :height="tableHeight"
        >
          <el-table-column
            prop="recordId"
            label="记录ID"
            width="80"
            align="center"
          />
          <el-table-column prop="pipelineId" label="管线ID" width="120" />
          <el-table-column
            prop="imageName"
            label="图像名称"
            min-width="160"
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
                :type="getResultType(row.detectionResult)"
                size="small"
              >
                {{ resultLabel(row.detectionResult) }}
              </el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="confidenceScore"
            label="置信度"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              {{
                row.confidenceScore != null
                  ? (row.confidenceScore * 100).toFixed(1) + '%'
                  : '-'
              }}
            </template>
          </el-table-column>

          <el-table-column prop="inspectTime" label="巡检时间" width="170">
            <template #default="{ row }">{{
              formatDateTime(row.inspectTime)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleView(row)"
                >详情</el-button
              >
              <el-button type="success" link @click="handleViewImage(row)"
                >查看图像</el-button
              >
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

    <!-- 上传弹窗 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传巡检图像"
      width="500px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="管线ID">
          <el-input
            v-model="uploadForm.pipelineId"
            placeholder="请输入管线ID"
          />
        </el-form-item>
        <el-form-item label="图像文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleUploadFileChange"
            :on-exceed="() => ElMessage.warning('只能上传一个文件')"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将图像拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 jpg / png / bmp 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload"
          >确认上传</el-button
        >
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="巡检记录详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录ID">{{
          currentRecord.recordId
        }}</el-descriptions-item>
        <el-descriptions-item label="管线ID">{{
          currentRecord.pipelineId
        }}</el-descriptions-item>
        <el-descriptions-item label="图像名称" :span="2">{{
          currentRecord.imageName
        }}</el-descriptions-item>
        <el-descriptions-item label="检测结果">
          <el-tag
            v-if="currentRecord.detectionResult"
            :type="getResultType(currentRecord.detectionResult)"
          >
            {{ resultLabel(currentRecord.detectionResult) }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="置信度">
          {{
            currentRecord.confidenceScore != null
              ? (currentRecord.confidenceScore * 100).toFixed(1) + '%'
              : '-'
          }}
        </el-descriptions-item>
        <el-descriptions-item label="巡检时间">{{
          currentRecord.inspectTime
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 图像预览弹窗 -->
    <el-dialog
      v-model="imageDialogVisible"
      title="巡检图像"
      :width="'90vw'"
      :style="{ maxWidth: '1200px' }"
      destroy-on-close
      align-center
    >
      <div v-if="previewImageUrl" class="image-preview">
        <img
          :src="previewImageUrl"
          style="
            width: 100%;
            max-height: 80vh;
            object-fit: contain;
            display: block;
            margin: 0 auto;
          "
        />
      </div>
      <div v-else class="image-error">暂无图像</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue';
import type { UploadFile } from 'element-plus';
import {
  getInspectionList,
  getInspectionById,
  uploadInspectionImage,
  getImageUrl,
} from '@/api/inspection';
import type { InspectionVO } from '@/types/api';
import { useUserStore } from '@/stores/user';
import { useTableHeight } from '@/composables/useTableHeight';
import { formatDateTime } from '@/utils/format';
import './style.css';

const userStore = useUserStore();
const { tableHeight, updateHeight } = useTableHeight('.page-container');

const loading = ref(false);
const inspectionList = ref<InspectionVO[]>([]);
const pageNum = ref(1);
const pageSize = ref(20);
const total = ref(0);

const searchForm = reactive({
  keyword: '',
  detectionResult: '',
  dateRange: null as [string, string] | null,
});

const uploadDialogVisible = ref(false);
const uploading = ref(false);
const uploadForm = reactive({
  pipelineId: '',
  file: null as File | null,
});

const detailDialogVisible = ref(false);
const currentRecord = ref<InspectionVO | null>(null);

const imageDialogVisible = ref(false);
const previewImageUrl = ref('');

const resultLabel = (r: string) => {
  const map: Record<string, string> = {
    normal: '正常',
    crack: '裂缝',
    corrosion: '腐蚀',
    fracture: '断裂',
    正常: '正常',
    检测到缺陷: '检测到缺陷',
  };
  return map[r] || r;
};

const getResultType = (r: string) => {
  const map: Record<string, string> = {
    normal: 'success',
    crack: 'warning',
    corrosion: 'danger',
    fracture: 'danger',
    正常: 'success',
    检测到缺陷: 'warning',
  };
  return (map[r] || 'info') as any;
};



const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword || undefined,
      detectionResult: searchForm.detectionResult || undefined,
    };
    if (searchForm.dateRange) {
      params.startTime = searchForm.dateRange[0];
      params.endTime = searchForm.dateRange[1];
    }
    const res = await getInspectionList(params);
    inspectionList.value = res.data.list;
    total.value = res.data.total;
  } catch {
    ElMessage.error('获取巡检记录失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchList();
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.detectionResult = '';
  searchForm.dateRange = null;
  handleSearch();
};

const handleView = async (row: InspectionVO) => {
  try {
    const res = await getInspectionById(row.recordId);
    currentRecord.value = res.data;
  } catch {
    currentRecord.value = row;
  }
  detailDialogVisible.value = true;
};

const handleViewImage = async (row: InspectionVO) => {
  previewImageUrl.value = '';
  imageDialogVisible.value = true;
  try {
    const res = await fetch(getImageUrl(row.recordId), {
      headers: { Authorization: `Bearer ${userStore.token}` },
    });
    if (res.ok) {
      const blob = await res.blob();
      previewImageUrl.value = URL.createObjectURL(blob);
    }
  } catch {
    // ignore
  }
};

const handleUploadFileChange = (uploadFile: UploadFile) => {
  uploadForm.file = uploadFile.raw ?? null;
};

const handleUpload = async () => {
  if (!uploadForm.pipelineId.trim()) {
    ElMessage.warning('请输入管线ID');
    return;
  }
  if (!uploadForm.file) {
    ElMessage.warning('请选择图像文件');
    return;
  }
  uploading.value = true;
  try {
    await uploadInspectionImage(uploadForm.file, uploadForm.pipelineId);
    ElMessage.success('上传成功');
    uploadDialogVisible.value = false;
    uploadForm.file = null;
    uploadForm.pipelineId = '';
    fetchList();
  } catch {
    ElMessage.error('上传失败');
  } finally {
    uploading.value = false;
  }
};

onMounted(() => {
  fetchList();
  setTimeout(updateHeight, 100);
});
</script>

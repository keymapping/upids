<template>
  <div class="page-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-button
            v-if="userStore.hasRole(['admin'])"
            type="primary"
            @click="importDialogVisible = true"
          >
            <el-icon><Upload /></el-icon>
            导入数据
          </el-button>
        </div>
      </template>

      <!-- 搜索条件 -->
      <div class="filter-section">
        <el-input
          v-model="searchForm.keyword"
          placeholder="管线编号/名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.materialType"
          placeholder="材质"
          clearable
          style="width: 140px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="钢管" value="steel" />
          <el-option label="铸铁管" value="cast_iron" />
          <el-option label="PE管" value="PE" />
          <el-option label="PVC管" value="PVC" />
          <el-option label="混凝土管" value="concrete" />
        </el-select>
        <el-input
          v-model="searchForm.regionCode"
          placeholder="区域编码"
          clearable
          style="width: 140px; margin-left: 10px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 120px; margin-left: 10px"
          @change="handleSearch"
        >
          <el-option label="正常" :value="1" />
          <el-option label="异常" :value="0" />
        </el-select>
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
          :data="pipelineList"
          v-loading="loading"
          stripe
          border
          style="width: 100%"
          :height="tableHeight"
        >
          <el-table-column
            prop="pipelineId"
            label="管线编号"
            width="140"
            show-overflow-tooltip
          />
          <el-table-column
            prop="pipelineName"
            label="管线名称"
            min-width="140"
            show-overflow-tooltip
          />
          <el-table-column prop="materialType" label="材质" width="100" />
          <el-table-column
            prop="diameter"
            label="管径(mm)"
            width="100"
            align="center"
          />
          <el-table-column prop="regionCode" label="区域编码" width="110" />
          <el-table-column prop="installTime" label="安装时间" width="120">
            <template #default="{ row }">{{
              formatDate(row.installTime)
            }}</template>
          </el-table-column>
          <el-table-column
            prop="defectCount"
            label="缺陷数"
            width="80"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                :type="row.defectCount > 0 ? 'danger' : 'success'"
                size="small"
              >
                {{ row.defectCount ?? 0 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 1 ? 'success' : 'danger'"
                size="small"
              >
                {{ row.status === 1 ? '正常' : '异常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{
              formatDateTime(row.createdAt)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleView(row)"
                >详情</el-button
              >
              <el-button
                v-if="userStore.hasRole(['admin'])"
                type="danger"
                link
                @click="handleDelete(row)"
                >删除</el-button
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

    <!-- 导入弹窗 -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入管线数据"
      width="500px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="文件类型">
          <el-select v-model="importForm.fileType" style="width: 100%">
            <el-option label="GeoJSON" value="geojson" />
            <el-option label="Excel" value="excel" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :accept="
              importForm.fileType === 'geojson'
                ? '.json,.geojson'
                : '.xlsx,.xls'
            "
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('只能上传一个文件')"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                {{
                  importForm.fileType === 'geojson'
                    ? '支持 .json / .geojson 格式'
                    : '支持 .xlsx / .xls 格式'
                }}
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport"
          >确定导入</el-button
        >
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="管线详情" width="600px">
      <el-descriptions :column="2" border v-if="currentPipeline">
        <el-descriptions-item label="管线编号">{{
          currentPipeline.pipelineId
        }}</el-descriptions-item>
        <el-descriptions-item label="管线名称">{{
          currentPipeline.pipelineName
        }}</el-descriptions-item>
        <el-descriptions-item label="材质">{{
          currentPipeline.materialType
        }}</el-descriptions-item>
        <el-descriptions-item label="管径(mm)">{{
          currentPipeline.diameter
        }}</el-descriptions-item>
        <el-descriptions-item label="区域编码">{{
          currentPipeline.regionCode
        }}</el-descriptions-item>
        <el-descriptions-item label="安装时间">{{
          currentPipeline.installTime
        }}</el-descriptions-item>
        <el-descriptions-item label="缺陷数量">{{
          currentPipeline.defectCount ?? 0
        }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentPipeline.status === 1 ? 'success' : 'danger'">
            {{ currentPipeline.status === 1 ? '正常' : '异常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{
          currentPipeline.createdAt
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue';
import type { UploadFile } from 'element-plus';
import {
  getPipelineList,
  deletePipeline,
  importPipeline,
} from '@/api/pipeline';
import type { PipelineVO } from '@/types/api';
import { useUserStore } from '@/stores/user';
import { useTableHeight } from '@/composables/useTableHeight';
import { formatDateTime, formatDate } from '@/utils/format';
import './style.css';

const userStore = useUserStore();
const { tableHeight, updateHeight } = useTableHeight('.page-container');

const loading = ref(false);
const pipelineList = ref<PipelineVO[]>([]);
const pageNum = ref(1);
const pageSize = ref(20);
const total = ref(0);

const searchForm = reactive({
  keyword: '',
  materialType: '',
  regionCode: '',
  status: undefined as number | undefined,
  dateRange: null as [string, string] | null,
});

const importDialogVisible = ref(false);
const importing = ref(false);
const importForm = reactive({
  fileType: 'geojson' as 'geojson' | 'excel',
  file: null as File | null,
});

const detailDialogVisible = ref(false);
const currentPipeline = ref<PipelineVO | null>(null);

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword || undefined,
      materialType: searchForm.materialType || undefined,
      regionCode: searchForm.regionCode || undefined,
    };
    if (searchForm.status !== undefined) params.status = searchForm.status;
    if (searchForm.dateRange) {
      params.startTime = searchForm.dateRange[0];
      params.endTime = searchForm.dateRange[1];
    }
    const res = await getPipelineList(params);
    pipelineList.value = res.data.list;
    total.value = res.data.total;
  } catch {
    ElMessage.error('获取管线列表失败');
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
  searchForm.materialType = '';
  searchForm.regionCode = '';
  searchForm.status = undefined;
  searchForm.dateRange = null;
  handleSearch();
};

const handleView = (row: PipelineVO) => {
  currentPipeline.value = row;
  detailDialogVisible.value = true;
};

const handleDelete = (row: PipelineVO) => {
  ElMessageBox.confirm(`确定删除管线「${row.pipelineName}」吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await deletePipeline(Number(row.pipelineId));
        ElMessage.success('删除成功');
        fetchList();
      } catch {
        ElMessage.error('删除失败');
      }
    })
    .catch(() => {});
};

const handleFileChange = (uploadFile: UploadFile) => {
  importForm.file = uploadFile.raw ?? null;
};

const handleImport = async () => {
  if (!importForm.file) {
    ElMessage.warning('请先选择文件');
    return;
  }
  importing.value = true;
  try {
    const res = await importPipeline(importForm.file, importForm.fileType);
    const { totalCount, successCount, failCount } = res.data;
    ElMessage.success(
      `导入完成：共 ${totalCount} 条，成功 ${successCount} 条，失败 ${failCount} 条`
    );
    importDialogVisible.value = false;
    importForm.file = null;
    fetchList();
  } catch {
    ElMessage.error('导入失败');
  } finally {
    importing.value = false;
  }
};

onMounted(() => {
  fetchList();
  setTimeout(updateHeight, 100);
});
</script>

<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="user">
        <div class="filter-section">
          <el-input
            v-model="userSearch"
            placeholder="搜索用户名"
            clearable
            style="width: 200px"
            @keyup.enter="handleUserSearch"
          />
          <el-select
            v-model="userRoleFilter"
            placeholder="角色筛选"
            clearable
            style="width: 120px; margin-left: 10px"
            @change="handleUserSearch"
          >
            <el-option label="管理员" value="admin" />
            <el-option label="用户" value="user" />
          </el-select>
          <el-select
            v-model="userStatusFilter"
            placeholder="状态筛选"
            clearable
            style="width: 120px; margin-left: 10px"
            @change="handleUserSearch"
          >
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button
            type="primary"
            style="margin-left: 10px"
            @click="handleUserSearch"
          >
            搜索
          </el-button>
          <el-button @click="handleUserReset">重置</el-button>
          <el-button
            type="success"
            style="margin-left: auto"
            @click="showCreateUserDialog"
          >
            <el-icon style="margin-right: 4px"><Plus /></el-icon>创建用户
          </el-button>
        </div>
        <div class="table-wrapper">
          <el-table
            :data="userList"
            v-loading="userLoading"
            stripe
            border
            style="width: 100%"
            :height="tableHeight"
          >
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="username" label="用户名" min-width="40" />
            <el-table-column label="角色" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.role === 'admin' ? 'danger' : 'info'"
                  size="small"
                >
                  {{ row.role === 'admin' ? '管理员' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 1 ? 'success' : 'danger'"
                  size="small"
                >
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="createdAt"
              label="创建时间"
              width="170"
              align="center"
            >
              <template #default="{ row }">{{
                formatDateTime(row.createdAt)
              }}</template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="200"
              fixed="right"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  :type="row.status === 1 ? 'danger' : 'success'"
                  link
                  size="small"
                  @click="handleToggleStatus(row)"
                >
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button
                  type="warning"
                  link
                  size="small"
                  @click="handleResetPassword(row)"
                >
                  重置密码
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-pagination
          v-model:current-page="userPage"
          v-model:page-size="userPageSize"
          :total="userTotal"
          :page-sizes="[10, 20, 50]"
          class="pagination"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchUserList"
          @current-change="fetchUserList"
        />
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="log">
        <div class="filter-section">
          <el-input
            v-model="logSearch"
            placeholder="搜索操作"
            clearable
            style="width: 180px"
            @keyup.enter="fetchLogList"
          />
          <el-input
            v-model="logUserSearch"
            placeholder="操作人"
            clearable
            style="width: 140px; margin-left: 10px"
            @keyup.enter="fetchLogList"
          />
          <el-select
            v-model="logModuleFilter"
            placeholder="模块"
            clearable
            style="width: 120px; margin-left: 10px"
            @change="fetchLogList"
          >
            <el-option label="管线管理" value="pipeline" />
            <el-option label="巡检管理" value="inspection" />
            <el-option label="任务管理" value="task" />
            <el-option label="用户管理" value="user" />
            <el-option label="预警中心" value="alert" />
          </el-select>
          <el-date-picker
            v-model="logDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px; margin-left: 10px"
            @change="fetchLogList"
          />
          <el-button
            type="primary"
            style="margin-left: 10px"
            @click="fetchLogList"
          >
            搜索
          </el-button>
          <el-button @click="handleLogReset">重置</el-button>
        </div>
        <div class="table-wrapper">
          <el-table
            :data="logList"
            v-loading="logLoading"
            stripe
            border
            style="width: 100%"
            :height="tableHeight"
          >
            <el-table-column prop="logId" label="ID" width="60" />
            <el-table-column prop="module" label="模块" width="100" />
            <el-table-column prop="operation" label="操作" width="120" />
            <el-table-column prop="username" label="操作人" width="100" />
            <el-table-column
              prop="requestUri"
              label="请求路径"
              min-width="200"
              show-overflow-tooltip
            />
            <el-table-column label="结果" width="80">
              <template #default="{ row }">
                <el-tag
                  :type="row.result === 'success' ? 'success' : 'danger'"
                  size="small"
                >
                  {{ row.result === 'success' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ipAddress" label="IP地址" width="130" />
            <el-table-column prop="createdAt" label="操作时间" width="180">
              <template #default="{ row }">{{
                formatDateTime(row.createdAt)
              }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 模拟数据 -->
      <el-tab-pane label="模拟数据" name="mock">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600">生成模拟数据（开发/演示用）</span>
          </template>
          <el-form
            :model="mockForm"
            label-width="120px"
            style="max-width: 500px"
          >
            <el-form-item label="管线数量">
              <el-input-number
                v-model="mockForm.pipelineCount"
                :min="10"
                :max="10000"
                :step="100"
              />
            </el-form-item>
            <el-form-item label="检测记录数">
              <el-input-number
                v-model="mockForm.inspectionCount"
                :min="10"
                :max="50000"
                :step="100"
              />
            </el-form-item>
            <el-form-item label="缺陷比例">
              <el-slider
                v-model="mockForm.defectRatio"
                :min="0"
                :max="1"
                :step="0.01"
                show-input
                :format-tooltip="(v: number) => (v * 100).toFixed(0) + '%'"
              />
            </el-form-item>
            <el-form-item label="历史跨度(年)">
              <el-input-number
                v-model="mockForm.historyYears"
                :min="1"
                :max="10"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="mockGenerating"
                @click="handleGenerateMock"
              >
                生成模拟数据
              </el-button>
            </el-form-item>
          </el-form>

          <template v-if="mockProgress">
            <el-divider />
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="任务ID">{{
                mockProgress.jobId
              }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag
                  :type="mockProgress.status === 'done' ? 'success' : 'warning'"
                  size="small"
                >
                  {{ mockProgress.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="管线数">{{
                mockProgress.pipelineCount
              }}</el-descriptions-item>
              <el-descriptions-item label="检测记录数">{{
                mockProgress.inspectionCount
              }}</el-descriptions-item>
              <el-descriptions-item label="缺陷数">{{
                mockProgress.defectCount
              }}</el-descriptions-item>
              <el-descriptions-item label="进度">
                <el-progress
                  :percentage="mockProgress.progress"
                  :status="mockProgress.progress >= 100 ? 'success' : undefined"
                />
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建用户对话框 -->
    <el-dialog
      v-model="createUserVisible"
      title="创建用户"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="createForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input
            v-model="createForm.realName"
            placeholder="请输入真实姓名（可选）"
          />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="createForm.role">
            <el-radio value="user">普通用户</el-radio>
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createUserVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="createUserLoading"
          @click="handleCreateUser"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import {
  getUserList,
  createUser,
  updateUserStatus,
  resetUserPassword,
} from '@/api/user';
import { getLogList } from '@/api/log';
import { generateMockData } from '@/api/mock';
import type { UserInfo, LogVO, MockProgressVO } from '@/types/api';
import { useTableHeight } from '@/composables/useTableHeight';
import { formatDateTime } from '@/utils/format';
import './style.css';
const { tableHeight, updateHeight } = useTableHeight('.page-container');

const activeTab = ref('user');

const userLoading = ref(false);
const userList = ref<UserInfo[]>([]);
const userSearch = ref('');
const userRoleFilter = ref('');
const userStatusFilter = ref<number | ''>('');
const userPage = ref(1);
const userPageSize = ref(20);
const userTotal = ref(0);

const fetchUserList = async () => {
  userLoading.value = true;
  try {
    const res = await getUserList({
      page: userPage.value,
      pageSize: userPageSize.value,
      username: userSearch.value || undefined,
      role: userRoleFilter.value || undefined,
      status:
        userStatusFilter.value !== '' ? userStatusFilter.value : undefined,
    });
    userList.value = res.data.list;
    userTotal.value = res.data.total;
  } catch {
    ElMessage.error('获取用户列表失败');
  } finally {
    userLoading.value = false;
  }
};

const handleUserSearch = () => {
  userPage.value = 1;
  fetchUserList();
};

const handleUserReset = () => {
  userSearch.value = '';
  userRoleFilter.value = '';
  userStatusFilter.value = '';
  userPage.value = 1;
  fetchUserList();
};

// 创建用户
const createUserVisible = ref(false);
const createUserLoading = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'user',
});
const createRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为6-50个字符', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
};

const showCreateUserDialog = () => {
  createForm.username = '';
  createForm.password = '';
  createForm.realName = '';
  createForm.role = 'user';
  createUserVisible.value = true;
};

const handleCreateUser = async () => {
  if (!createFormRef.value) return;
  const valid = await createFormRef.value.validate().catch(() => false);
  if (!valid) return;

  createUserLoading.value = true;
  try {
    await createUser({
      username: createForm.username,
      password: createForm.password,
      realName: createForm.realName || undefined,
      role: createForm.role,
    });
    ElMessage.success('用户创建成功');
    createUserVisible.value = false;
    fetchUserList();
  } catch {
    ElMessage.error('用户创建失败');
  } finally {
    createUserLoading.value = false;
  }
};

const handleToggleStatus = async (row: UserInfo) => {
  const newStatus = row.status === 1 ? 0 : 1;
  const actionText = newStatus === 1 ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}用户「${row.username}」吗？`,
      '提示',
      { type: 'warning' }
    );
    await updateUserStatus(row.id, newStatus);
    ElMessage.success(`用户已${actionText}`);
    fetchUserList();
  } catch {}
};

const handleResetPassword = (row: UserInfo) => {
  ElMessageBox.prompt(`重置用户「${row.username}」的密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{6,50}$/,
    inputErrorMessage: '密码长度6-50字符',
    inputPlaceholder: '请输入新密码',
    type: 'warning',
  }).then(async ({ value: newPassword }) => {
    try {
      await resetUserPassword(row.id, newPassword);
      ElMessage.success('密码重置成功');
    } catch {
      ElMessage.error('重置密码失败');
    }
  });
};

const logLoading = ref(false);
const logList = ref<LogVO[]>([]);
const logSearch = ref('');
const logUserSearch = ref('');
const logModuleFilter = ref('');
const logDateRange = ref<[string, string] | null>(null);

const fetchLogList = async () => {
  logLoading.value = true;
  try {
    const params: any = { page: 1, pageSize: 100 };
    if (logSearch.value) params.operation = logSearch.value;
    if (logUserSearch.value) params.username = logUserSearch.value;
    if (logModuleFilter.value) params.module = logModuleFilter.value;
    if (logDateRange.value) {
      params.startTime = logDateRange.value[0];
      params.endTime = logDateRange.value[1];
    }
    const res = await getLogList(params);
    logList.value = res.data.list;
  } catch {
    ElMessage.error('获取日志列表失败');
  } finally {
    logLoading.value = false;
  }
};

const handleLogReset = () => {
  logSearch.value = '';
  logUserSearch.value = '';
  logModuleFilter.value = '';
  logDateRange.value = null;
  fetchLogList();
};

const mockGenerating = ref(false);
const mockForm = reactive({
  pipelineCount: 100,
  inspectionCount: 500,
  defectRatio: 0.15,
  historyYears: 3,
});
const mockProgress = ref<MockProgressVO | null>(null);

const handleGenerateMock = async () => {
  mockGenerating.value = true;
  try {
    const res = await generateMockData({
      pipelineCount: mockForm.pipelineCount,
      inspectionCount: mockForm.inspectionCount,
      defectRatio: mockForm.defectRatio,
      historyYears: mockForm.historyYears,
    });
    const data = res.data;
    mockProgress.value = {
      jobId: '',
      status: data.status || 'done',
      pipelineCount: data.pipelineCount || 0,
      inspectionCount: data.inspectionCount || 0,
      defectCount: data.defectCount || 0,
      progress: 100,
    };
    ElMessage.success(data.message || '模拟数据生成完成');
    mockGenerating.value = false;
  } catch {
    ElMessage.error('模拟数据生成失败');
    mockGenerating.value = false;
  }
};

watch(activeTab, (tab) => {
  if (tab === 'log') fetchLogList();
  setTimeout(updateHeight, 100);
});

onMounted(() => {
  fetchUserList();
  setTimeout(updateHeight, 100);
});
</script>

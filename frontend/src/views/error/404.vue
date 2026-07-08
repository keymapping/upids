<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-illustration">
        <span class="error-code">404</span>
        <div class="search-icon">
          <el-icon :size="64"><Search /></el-icon>
        </div>
      </div>
      <h2 class="error-title">页面走丢了</h2>
      <p class="error-desc">您访问的页面不存在或已被移除</p>
      <div class="error-actions">
        <el-button type="primary" size="large" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button size="large" @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Search, HomeFilled } from '@element-plus/icons-vue';

const router = useRouter();

const goHome = () => router.push('/');
const goBack = () => router.back();
</script>

<style scoped>
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
}

.error-content {
  text-align: center;
  padding: 40px;
}

.error-illustration {
  position: relative;
  display: inline-block;
  margin-bottom: 24px;
}

.error-code {
  font-size: 140px;
  font-weight: 800;
  color: transparent;
  background: linear-gradient(135deg, #909399, #409eff);
  -webkit-background-clip: text;
  background-clip: text;
  line-height: 1;
}

.search-icon {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  color: #409eff;
  animation: searchMove 2.5s ease-in-out infinite;
}

@keyframes searchMove {
  0%,
  100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(-8px);
  }
}

.error-title {
  font-size: 24px;
  color: #303133;
  margin: 20px 0 12px;
}

.error-desc {
  font-size: 15px;
  color: #909399;
  margin-bottom: 32px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>

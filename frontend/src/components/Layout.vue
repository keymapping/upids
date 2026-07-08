<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <img src="@/assets/logo.svg" alt="UPIDS" />
        <span v-show="!isCollapse">水下管线检测与缺陷识别系统</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          class="menu"
          :collapse="isCollapse"
          :unique-opened="true"
          router
        >
          <template v-for="item in menuItems" :key="item.path">
            <el-menu-item :index="item.path">
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="toggleCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item
              v-if="
                currentRoute.meta.title && currentRoute.path !== '/dashboard'
              "
            >
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge
            :value="alertStore.unreadCount"
            :hidden="alertStore.unreadCount === 0"
            class="badge-item"
          >
            <el-icon :size="20" class="header-icon" @click="goToAlert"
              ><Bell
            /></el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <span class="username">{{
                userStore.userInfo?.realName || userStore.userInfo?.username
              }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-tag
                    :type="
                      userStore.userInfo?.role === 'admin' ? 'danger' : 'info'
                    "
                    size="small"
                  >
                    {{
                      userStore.userInfo?.role === 'admin'
                        ? '管理员'
                        : '普通用户'
                    }}
                  </el-tag>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout"
                  >退出登录</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useAlertStore } from '@/stores/alert';
import { ElMessageBox } from 'element-plus';
import { Expand, Fold, ArrowDown, Bell } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const alertStore = useAlertStore();

const isCollapse = ref(false);
const currentRoute = computed(() => route);
const activeMenu = computed(() => route.path);

// 根据路由配置生成菜单
const menuItems = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.name === 'Layout');
  if (!layoutRoute?.children) return [];

  return layoutRoute.children
    .filter((child) => {
      if (child.meta?.hidden) return false;
      const roles = child.meta?.roles as string[] | undefined;
      if (roles && roles.length > 0) {
        return userStore.hasRole(roles);
      }
      return true;
    })
    .map((child) => ({
      path: `/${child.path}`,
      title: (child.meta?.title as string) || '',
      icon: (child.meta?.icon as string) || 'Document',
    }));
});

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value;
};

const goToAlert = () => {
  router.push('/alert');
};

const handleCommand = (command: string) => {
  switch (command) {
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        userStore.logout();
      });
      break;
  }
};

onMounted(() => {
  userStore.initUser();
  alertStore.fetchUnreadCount();
});
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #263445;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.logo img {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}

.menu {
  border: none;
  background-color: #304156;
}

.menu:not(.el-menu--collapse) {
  width: 220px;
}

:deep(.el-menu-item) {
  color: #bfcbd9;
}

:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background-color: #263445 !important;
  color: #409eff;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  margin-right: 15px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  cursor: pointer;
  transition: color 0.3s;
}

.header-icon:hover {
  color: #409eff;
}

.badge-item {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.main {
  background-color: #f0f2f5;
  padding: 0;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
</style>

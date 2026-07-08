import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';

// 路由元信息类型
declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    roles?: string[]; // 允许访问的角色
    icon?: string; // 菜单图标
    hidden?: boolean; // 是否隐藏菜单
  }
}

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
    beforeEnter: (_to, _from, next) => {
      const userStore = useUserStore();
      if (userStore.token) {
        next('/');
      } else {
        next();
      }
    },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', roles: ['admin', 'user'] },
      },
      {
        path: 'pipeline',
        name: 'Pipeline',
        component: () => import('@/views/pipeline/index.vue'),
        meta: {
          title: '管线管理',
          icon: 'Connection',
          roles: ['admin', 'user'],
        },
      },
      {
        path: 'inspection',
        name: 'Inspection',
        component: () => import('@/views/inspection/index.vue'),
        meta: { title: '检测记录', icon: 'Document', roles: ['admin', 'user'] },
      },
      {
        path: 'gis',
        name: 'GIS',
        component: () => import('@/views/gis/index.vue'),
        meta: { title: 'GIS地图', icon: 'Location', roles: ['admin', 'user'] },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: {
          title: '统计分析',
          icon: 'DataAnalysis',
          roles: ['admin', 'user'],
        },
      },
      {
        path: 'alert',
        name: 'Alert',
        component: () => import('@/views/alert/index.vue'),
        meta: { title: '预警中心', icon: 'Bell', roles: ['admin', 'user'] },
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '报告管理', icon: 'Document', roles: ['admin', 'user'] },
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/task/index.vue'),
        meta: { title: '任务管理', icon: 'List', roles: ['admin', 'user'] },
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置', icon: 'Setting', roles: ['admin'] },
      },
    ],
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', hidden: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 全局前置守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore();

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - UPIDS`;
  }

  // 白名单路由（无需登录）
  const whiteList = ['/login', '/403', '/404'];
  if (whiteList.includes(to.path)) {
    next();
    return;
  }

  // 检查Token
  const token = userStore.token;
  if (!token) {
    ElMessage.warning('请先登录');
    next('/login');
    return;
  }

  // 初始化用户信息
  if (!userStore.userInfo) {
    userStore.initUser();
  }

  // 检查角色权限
  if (to.meta.roles && to.meta.roles.length > 0) {
    const userRole = userStore.userInfo?.role;
    if (!userRole || !to.meta.roles.includes(userRole)) {
      ElMessage.error('无权限访问');
      next('/403');
      return;
    }
  }

  next();
});

// 全局后置钩子
router.afterEach(() => {
  // 可以在这里添加进度条结束等逻辑
});

export default router;

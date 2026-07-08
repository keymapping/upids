import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { UserInfo, LoginParams } from '@/types/api';
import { post } from '@/utils/request';
import router from '@/router';

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 初始化，刷新页面时保持登录状态
  const token = ref<string>(localStorage.getItem('token') || '');

  const getSavedUserInfo = (): UserInfo | null => {
    const saved = localStorage.getItem('userInfo');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return null;
      }
    }
    return null;
  };

  const userInfo = ref<UserInfo | null>(getSavedUserInfo());

  // 从 localStorage 恢复状态
  const initUser = () => {
    const savedToken = localStorage.getItem('token');
    const savedUserInfo = localStorage.getItem('userInfo');

    if (savedToken) {
      token.value = savedToken;
    }

    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo);
      } catch (e) {
        console.error('解析用户信息失败:', e);
        userInfo.value = null;
      }
    }
  };

  // 登录
  const login = async (loginParams: LoginParams): Promise<void> => {
    try {
      const res = await post<any>('/api/auth/login', loginParams);

      // 后端返回格式: { token, userId, username, role, realName }
      const data = res.data;
      token.value = data.token;
      userInfo.value = {
        id: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role,
        status: 1,
      };

      // 保存到 localStorage
      localStorage.setItem('token', data.token);
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value));

      router.push('/');
    } catch (error) {
      throw error;
    }
  };

  // 登出
  const logout = () => {
    token.value = '';
    userInfo.value = null;
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    router.push('/login');
  };

  // 更新用户信息
  const updateUserInfo = (info: UserInfo) => {
    userInfo.value = info;
    localStorage.setItem('userInfo', JSON.stringify(info));
  };

  // 检查是否有权限
  const hasRole = (roles: string[]): boolean => {
    if (!userInfo.value) return false;
    return roles.includes(userInfo.value.role);
  };

  return {
    token,
    userInfo,
    initUser,
    login,
    logout,
    updateUserInfo,
    hasRole,
  };
});

import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { AlertVO } from '@/types/api';
import { get } from '@/utils/request';

export const useAlertStore = defineStore('alert', () => {
  const alerts = ref<AlertVO[]>([]);
  const unreadCount = ref<number>(0);

  // 获取未读预警数量
  const fetchUnreadCount = async () => {
    try {
      const res = await get<{ count: number }>('/api/alerts/unread-count');
      unreadCount.value = res.data.count;
    } catch (error) {
      console.error('获取未读预警数量失败:', error);
    }
  };

  // 获取预警列表
  const fetchAlerts = async (params?: {
    isRead?: boolean;
    alertType?: string;
    page?: number;
    pageSize?: number;
  }) => {
    try {
      const res = await get<{ list: AlertVO[]; total: number }>(
        '/api/alerts',
        params
      );
      alerts.value = res.data.list;
    } catch (error) {
      console.error('获取预警列表失败:', error);
    }
  };

  // 标记单条已读
  const markAsRead = (alertId: number) => {
    const alert = alerts.value.find((a) => a.alertId === alertId);
    if (alert && !alert.isRead) {
      alert.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  };

  // 全部标记已读
  const markAllAsRead = () => {
    alerts.value.forEach((alert) => {
      alert.isRead = true;
    });
    unreadCount.value = 0;
  };

  return {
    alerts,
    unreadCount,
    fetchUnreadCount,
    fetchAlerts,
    markAsRead,
    markAllAsRead,
  };
});

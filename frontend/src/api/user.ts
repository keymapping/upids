import { get, put, post } from '@/utils/request';
import type { UserInfo, PageQuery, PageResult, ApiResult } from '@/types/api';

// 获取用户列表（管理员）
export function getUserList(
  params: PageQuery
): Promise<ApiResult<PageResult<UserInfo>>> {
  return get<PageResult<UserInfo>>('/api/users', params);
}

// 创建用户（管理员）
export function createUser(data: {
  username: string;
  password: string;
  realName?: string;
  role?: string;
}): Promise<ApiResult<UserInfo>> {
  return post<UserInfo>('/api/auth/register', data);
}

// 更新用户状态（启用/禁用）
export function updateUserStatus(
  id: number,
  status: number
): Promise<ApiResult<void>> {
  return put<void>(`/api/users/${id}/status`, { status });
}

// 重置用户密码
export function resetUserPassword(
  id: number,
  newPassword: string
): Promise<ApiResult<void>> {
  return put<void>(`/api/users/${id}/reset-password`, { newPassword });
}

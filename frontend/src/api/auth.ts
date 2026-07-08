import { post, get } from '@/utils/request'
import type { LoginParams, LoginVO, UserInfo } from '@/types/api'
import type { ApiResult } from '@/types/api'

// 登录
export function login(data: LoginParams): Promise<ApiResult<LoginVO>> {
  return post<LoginVO>('/api/auth/login', data)
}

// 注册
export function register(data: {
  username: string;
  password: string;
  realName?: string;
}): Promise<ApiResult<UserInfo>> {
  return post<UserInfo>('/api/auth/register', data)
}

// 获取当前用户信息
export function getUserInfo(): Promise<ApiResult<UserInfo>> {
  return get<UserInfo>('/api/auth/me')
}
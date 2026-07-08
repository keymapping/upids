package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.dto.request.LoginRequest;
import com.upids.dto.request.RegisterRequest;
import com.upids.dto.response.LoginResponse;
import com.upids.dto.response.UserInfoResponse;
import com.upids.entity.SysUser;

/**
 * 系统用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);

    /**
     * 注册用户（管理员）
     */
    UserInfoResponse register(RegisterRequest request);

    /**
     * 获取用户信息
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 分页查询用户
     */
    PageResult<UserInfoResponse> listUsers(Integer page, Integer pageSize, String username, String role, Integer status);

    /**
     * 更新用户状态（启用/禁用）
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);
}

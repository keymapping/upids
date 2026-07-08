package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.common.enums.RoleEnum;
import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.common.util.JwtUtil;
import com.upids.dto.request.LoginRequest;
import com.upids.dto.request.RegisterRequest;
import com.upids.dto.response.LoginResponse;
import com.upids.dto.response.UserInfoResponse;
import com.upids.entity.SysUser;
import com.upids.mapper.SysUserMapper;
import com.upids.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = getByUsername(request.getUsername());
        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw BusinessException.forbidden("账户已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().getValue());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().getValue());
        response.setRealName(user.getRealName());

        return response;
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public UserInfoResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        SysUser existingUser = getByUsername(request.getUsername());
        if (existingUser != null) {
            throw BusinessException.conflict("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        // 角色默认为 user
        String role = (request.getRole() != null && !request.getRole().isEmpty()) ? request.getRole() : "user";
        user.setRole(RoleEnum.fromValue(role));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        save(user);

        return toUserInfoResponse(user);
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toUserInfoResponse(user);
    }

    @Override
    public PageResult<UserInfoResponse> listUsers(Integer page, Integer pageSize, String username, String role, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, RoleEnum.fromValue(role));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> pageParam = new Page<>(page, pageSize);
        Page<SysUser> result = page(pageParam, wrapper);

        List<UserInfoResponse> list = result.getRecords().stream()
                .map(this::toUserInfoResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), page, pageSize);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        SysUser user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (status != 0 && status != 1) {
            throw BusinessException.badRequest("状态值只能为 0 或 1");
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    /**
     * SysUser -> UserInfoResponse
     */
    private UserInfoResponse toUserInfoResponse(SysUser user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole() != null ? user.getRole().getValue() : null);
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}

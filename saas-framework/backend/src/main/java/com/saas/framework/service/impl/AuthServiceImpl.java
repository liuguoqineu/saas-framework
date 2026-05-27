package com.saas.framework.service.impl;

import com.saas.framework.common.dto.LoginRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.common.util.JwtUtil;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysTenant;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.mapper.SysTenantMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysTenantMapper sysTenantMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public Map<String, Object> login(LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());

        // 查询用户
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        // 检查租户状态（非超级账户需验证所属租户是否被禁用）
        if (user.getTenantId() != null && user.getTenantId() != 0) {
            SysTenant tenant = sysTenantMapper.selectById(user.getTenantId());
            if (tenant != null && tenant.getStatus() != null && tenant.getStatus() == 0) {
                throw new BusinessException("所属租户已被禁用，请联系平台管理员");
            }
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());

        // 查询用户权限
        List<String> permissions = sysRolePermissionMapper.selectPermissionCodesByRoleId(user.getRoleId());

        // 查询角色名称
        String roleName = "普通用户";
        if (user.getTenantId() != null && user.getTenantId() == 0) {
            roleName = "超级管理员";
        } else if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null && role.getName() != null) {
                roleName = role.getName();
            }
        }

        // 构建返回数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("roleName", roleName);
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("permissions", permissions);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);

        log.info("用户 {} 登录成功, tenantId={}", user.getUsername(), user.getTenantId());
        return result;
    }

    @Override
    public Map<String, Object> getUserInfo() {
        Long userId = com.saas.framework.common.context.UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        List<String> permissions = sysRolePermissionMapper.selectPermissionCodesByRoleId(user.getRoleId());

        // 查询角色名称
        String roleName = "普通用户";
        if (user.getTenantId() != null && user.getTenantId() == 0) {
            roleName = "超级管理员";
        } else if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null && role.getName() != null) {
                roleName = role.getName();
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("roleName", roleName);
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("permissions", permissions);

        return userInfo;
    }
}

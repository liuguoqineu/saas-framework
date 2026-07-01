package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.UserCreateRequest;
import com.saas.framework.common.dto.UserUpdateRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.PermissionService;
import com.saas.framework.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 员工（用户）服务实现
 * 仅租户管理员可操作本租户员工
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private PermissionService permissionService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public IPage<SysUser> page(int page, int size, String realName) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (UserContext.isSuperAdmin()) {
            // 超级管理员可查看所有员工（排除自己）
            wrapper.ne(SysUser::getTenantId, 0);
        } else {
            // 租户管理员只能查看本租户员工
            wrapper.eq(SysUser::getTenantId, UserContext.getTenantId());
        }

        if (StringUtils.hasText(realName)) {
            wrapper.like(SysUser::getRealName, realName);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SysUser> listByTenant() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(SysUser::getTenantId, UserContext.getTenantId());
        }
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByAsc(SysUser::getRealName);
        return sysUserMapper.selectList(wrapper);
    }

    @Override
    public List<SysUser> listByTenantAndRoleName(String roleName) {
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getName, roleName);
        if (!UserContext.isSuperAdmin()) {
            roleWrapper.eq(SysRole::getTenantId, UserContext.getTenantId());
        }
        SysRole role = sysRoleMapper.selectOne(roleWrapper);
        if (role == null) {
            return List.of();
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(SysUser::getTenantId, UserContext.getTenantId());
        }
        wrapper.eq(SysUser::getRoleId, role.getId());
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByAsc(SysUser::getRealName);
        return sysUserMapper.selectList(wrapper);
    }

    @Override
    public List<SysUser> listByTenantAndPostType(String postType) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(SysUser::getTenantId, UserContext.getTenantId());
        }
        wrapper.eq(SysUser::getPostType, postType);
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByAsc(SysUser::getRealName);
        return sysUserMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserCreateRequest request) {
        // 仅超级管理员可直接创建员工，租户管理员需通过员工申请流程
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "租户管理员无法直接创建员工，请提交员工申请等待超级管理员审核");
        }

        log.info("超级管理员创建员工: username={}, realName={}", request.getUsername(), request.getRealName());

        // 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名 " + request.getUsername() + " 已存在");
        }

        // 创建员工
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        // 密码：不提供则默认 123456
        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(password));
        user.setRoleId(request.getRoleId());
        // 超级管理员创建员工时需要指定租户
        user.setTenantId(request.getTenantId() != null ? request.getTenantId() : 0L);
        user.setRealName(request.getRealName());
        user.setPostType(request.getPostType());
        user.setZhizhiContent(request.getZhizhiContent());
        user.setZhizhiImageUrl(request.getZhizhiImageUrl());
        user.setStatus(1);
        sysUserMapper.insert(user);

        log.info("员工创建成功: userId={}, username={}, tenantId={}", user.getId(), request.getUsername(), user.getTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateRequest request) {
        log.info("修改员工: userId={}, realName={}, roleId={}", id, request.getRealName(), request.getRoleId());

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "员工不存在");
        }

        // 校验是否属于同一租户
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(user.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的员工");
        }

        if (!UserContext.isSuperAdmin() && !user.getRoleId().equals(request.getRoleId())) {
            List<Long> rolePermissionIds = sysRolePermissionMapper.selectPermissionIdsByRoleId(request.getRoleId());
            permissionService.checkPermissionsWithin(rolePermissionIds);
        }

        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getPostType() != null) {
            user.setPostType(request.getPostType());
        }
        sysUserMapper.updateById(user);

        log.info("员工修改成功: userId={}", id);
    }

    @Override
    public void resetPassword(Long id) {
        log.info("重置密码: userId={}", id);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "员工不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(user.getTenantId())) {
            throw new BusinessException(403, "无权重置其他租户员工的密码");
        }

        user.setPassword(passwordEncoder.encode("123456"));
        sysUserMapper.updateById(user);

        log.info("密码重置成功: userId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除员工: userId={}", id);

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "员工不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(user.getTenantId())) {
            throw new BusinessException(403, "无权删除其他租户的员工");
        }

        sysUserMapper.deleteById(id);
        log.info("员工删除成功: userId={}", id);
    }
}

package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.RoleCreateRequest;
import com.saas.framework.common.dto.RoleResponse;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.SysRole;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.PermissionService;
import com.saas.framework.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 角色服务实现
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private PermissionService permissionService;

    @Override
    public RoleResponse getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }

        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setTenantId(role.getTenantId());
        response.setCreateTime(role.getCreateTime());
        response.setUpdateTime(role.getUpdateTime());

        java.util.List<Long> permissionIds = sysRolePermissionMapper.selectPermissionIdsByRoleId(id);
        response.setPermissionIds(permissionIds);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            response.setPermissions(permissionService.listByIds(permissionIds));
        }

        LambdaQueryWrapper<com.saas.framework.entity.SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(com.saas.framework.entity.SysUser::getRoleId, id);
        userWrapper.eq(com.saas.framework.entity.SysUser::getDeleted, 0);
        Long userCount = sysUserMapper.selectCount(userWrapper);
        response.setUserCount(userCount);

        return response;
    }

    @Override
    public IPage<SysRole> page(int page, int size) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        // 租户管理员只能看到本租户角色 + 平台角色
        if (!UserContext.isSuperAdmin()) {
            wrapper.and(w -> w.eq(SysRole::getTenantId, UserContext.getTenantId())
                    .or().eq(SysRole::getTenantId, 0));
        } else {
            // 超级账户可以看到所有角色（包括平台角色和租户角色）
            wrapper.eq(SysRole::getTenantId, 0)
                    .or()
                    .ne(SysRole::getTenantId, 0);
        }

        wrapper.orderByDesc(SysRole::getCreateTime);
        return sysRoleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RoleCreateRequest request) {
        log.info("创建角色: name={}, permissionIds={}", request.getName(), request.getPermissionIds());

        // 只有超级管理员可以创建角色
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "只有超级管理员可以创建角色");
        }

        // 校验权限范围
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissionService.checkPermissionsWithin(request.getPermissionIds());
        }

        // 保存角色
        SysRole role = new SysRole();
        role.setName(request.getName());

        // 超级管理员创建的角色为平台角色（tenant_id=0），所有租户可用
        role.setTenantId(0L);

        sysRoleMapper.insert(role);

        // 保存角色-权限关联
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            // 先删除旧的（刚创建没有旧的），再插入新的
            for (Long permId : request.getPermissionIds()) {
                com.saas.framework.entity.SysRolePermission rp = new com.saas.framework.entity.SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }

        log.info("角色创建成功: roleId={}, name={}", role.getId(), request.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleCreateRequest request) {
        log.info("修改角色: roleId={}, name={}, permissionIds={}", id, request.getName(), request.getPermissionIds());

        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }

        // 只有超级管理员可以修改角色
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "只有超级管理员可以修改角色");
        }

        // 校验权限范围
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissionService.checkPermissionsWithin(request.getPermissionIds());
        }

        // 更新角色名称
        role.setName(request.getName());
        sysRoleMapper.updateById(role);

        // 更新权限关联：先删后增
        sysRolePermissionMapper.deleteByRoleId(id);
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (Long permId : request.getPermissionIds()) {
                com.saas.framework.entity.SysRolePermission rp = new com.saas.framework.entity.SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }

        log.info("角色修改成功: roleId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("删除角色: roleId={}", id);

        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }

        // 只有超级管理员可以删除角色
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "只有超级管理员可以删除角色");
        }

        // 检查是否有用户正在使用此角色
        LambdaQueryWrapper<com.saas.framework.entity.SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(com.saas.framework.entity.SysUser::getRoleId, id);
        userWrapper.eq(com.saas.framework.entity.SysUser::getDeleted, 0);
        Long userCount = sysUserMapper.selectCount(userWrapper);
        if (userCount != null && userCount > 0) {
            throw new BusinessException("该角色下还有 " + userCount + " 个用户，无法删除");
        }

        // 删除角色-权限关联
        sysRolePermissionMapper.deleteByRoleId(id);

        // 逻辑删除角色
        sysRoleMapper.deleteById(id);

        log.info("角色删除成功: roleId={}", id);
    }
}

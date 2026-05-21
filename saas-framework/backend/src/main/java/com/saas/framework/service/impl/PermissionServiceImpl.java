package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.SysPermission;
import com.saas.framework.mapper.SysPermissionMapper;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<Map<String, Object>> getPermissionTree() {
        // 查询所有权限
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort));

        // 如果当前用户是租户管理员，只返回自己权限范围内的权限
        if (!UserContext.isSuperAdmin()) {
            List<String> userPermissions = UserContext.getPermissions();
            if (userPermissions != null && !userPermissions.isEmpty()) {
                Set<String> permissionSet = new HashSet<>(userPermissions);
                allPermissions = allPermissions.stream()
                        .filter(p -> permissionSet.contains(p.getCode()))
                        .collect(Collectors.toList());
            }
        }

        // 构建树：先找出根节点（parentId = 0），再递归找子节点
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysPermission perm : allPermissions) {
            if (perm.getParentId() == null || perm.getParentId() == 0) {
                tree.add(buildTreeNode(perm, allPermissions));
            }
        }
        return tree;
    }

    /**
     * 递归构建权限树节点
     */
    private Map<String, Object> buildTreeNode(SysPermission permission, List<SysPermission> allPermissions) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", permission.getId());
        node.put("name", permission.getName());
        node.put("code", permission.getCode());
        node.put("type", permission.getType());
        node.put("parentId", permission.getParentId());
        node.put("sort", permission.getSort());

        // 递归找子节点
        List<Map<String, Object>> children = new ArrayList<>();
        for (SysPermission perm : allPermissions) {
            if (permission.getId().equals(perm.getParentId())) {
                children.add(buildTreeNode(perm, allPermissions));
            }
        }
        node.put("children", children);
        return node;
    }

    @Override
    public void checkPermissionsWithin(List<Long> permissionIds) {
        // 超级账户拥有所有权限，直接通过
        if (UserContext.isSuperAdmin()) {
            return;
        }

        // 获取当前用户的权限编码列表（由 JwtAuthFilter 在登录时设置）
        List<String> userPermissionCodes = UserContext.getPermissions();
        if (userPermissionCodes == null || userPermissionCodes.isEmpty()) {
            throw new BusinessException(403, "当前用户没有分配任何权限");
        }

        // 查询所有权限对应的ID
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        Set<Long> userPermissionIds = allPermissions.stream()
                .filter(p -> userPermissionCodes.contains(p.getCode()))
                .map(SysPermission::getId)
                .collect(Collectors.toSet());

        // 检查待分配的权限是否都在用户权限范围内
        for (Long permId : permissionIds) {
            if (!userPermissionIds.contains(permId)) {
                SysPermission perm = sysPermissionMapper.selectById(permId);
                String permName = perm != null ? perm.getName() : String.valueOf(permId);
                throw new BusinessException(403, "权限「" + permName + "」超出了您的权限范围，无法分配");
            }
        }
    }

    @Override
    public List<Map<String, Object>> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(ids);
        return permissions.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("code", p.getCode());
            map.put("type", p.getType());
            return map;
        }).collect(Collectors.toList());
    }
}

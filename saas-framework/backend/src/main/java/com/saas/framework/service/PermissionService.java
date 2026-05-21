package com.saas.framework.service;

import java.util.List;
import java.util.Map;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取权限树（用于前端 el-tree 展示）
     * @return 树形结构的权限列表
     */
    List<Map<String, Object>> getPermissionTree();

    /**
     * 校验权限集合是否在当前用户权限范围内
     * 超级账户直接通过
     *
     * @param permissionIds 待分配的权限ID列表
     */
    void checkPermissionsWithin(List<Long> permissionIds);

    List<Map<String, Object>> listByIds(List<Long> ids);
}

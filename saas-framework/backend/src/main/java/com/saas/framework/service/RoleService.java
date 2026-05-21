package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.RoleCreateRequest;
import com.saas.framework.common.dto.RoleResponse;
import com.saas.framework.entity.SysRole;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 根据ID查询角色详情（含权限ID列表）
     */
    RoleResponse getById(Long id);

    /**
     * 分页查询角色列表
     * 超级账户看到所有角色，租户管理员只看到本租户角色
     */
    IPage<SysRole> page(int page, int size);

    /**
     * 创建角色并分配权限
     * 租户管理员创建时会校验权限范围
     */
    void create(RoleCreateRequest request);

    /**
     * 修改角色及权限
     */
    void update(Long id, RoleCreateRequest request);

    /**
     * 删除角色
     */
    void delete(Long id);
}

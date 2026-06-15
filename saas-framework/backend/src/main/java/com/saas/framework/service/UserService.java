package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.UserCreateRequest;
import com.saas.framework.common.dto.UserUpdateRequest;
import com.saas.framework.entity.SysUser;

import java.util.List;
import java.util.Map;

/**
 * 员工（用户）服务接口
 */
public interface UserService {

    /**
     * 分页查询本租户员工
     */
    IPage<SysUser> page(int page, int size, String realName);

    List<SysUser> listByTenant();

    List<SysUser> listByTenantAndRoleName(String roleName);

    List<SysUser> listByTenantAndPostType(String postType);

    /**
     * 新增员工
     * 自动设置 tenant_id 为当前租户ID
     */
    void create(UserCreateRequest request);

    /**
     * 修改员工信息
     */
    void update(Long id, UserUpdateRequest request);

    /**
     * 重置密码为 123456
     */
    void resetPassword(Long id);

    /**
     * 删除员工（逻辑删除）
     */
    void delete(Long id);
}

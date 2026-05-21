package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.TenantCreateRequest;
import com.saas.framework.entity.SysTenant;

import java.util.Map;

/**
 * 租户服务接口
 */
public interface TenantService {

    /**
     * 分页查询租户列表
     */
    IPage<SysTenant> page(int page, int size);

    /**
     * 创建租户（同时创建租户管理员账号）
     * @return 包含管理员用户名和明文密码
     */
    Map<String, String> create(TenantCreateRequest request);

    /**
     * 修改租户状态
     */
    void updateStatus(Long id, Integer status);
}

package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.AppVersionCheckRequest;
import com.saas.framework.common.dto.AppVersionRequest;
import com.saas.framework.entity.AppVersion;

import java.util.Map;

/**
 * APP版本管理服务接口
 */
public interface AppVersionService {

    /**
     * 检查更新
     * @return 更新信息，null表示无需更新
     */
    Map<String, Object> checkUpdate(AppVersionCheckRequest request);

    /**
     * 创建版本
     */
    void create(AppVersionRequest request);

    /**
     * 修改版本
     */
    void update(Long id, AppVersionRequest request);

    /**
     * 删除版本
     */
    void delete(Long id);

    /**
     * 分页查询版本列表
     */
    IPage<AppVersion> page(int page, int size, String platform);

    /**
     * 获取版本详情
     */
    AppVersion getById(Long id);
}

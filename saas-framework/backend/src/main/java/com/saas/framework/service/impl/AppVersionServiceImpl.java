package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.AppVersionCheckRequest;
import com.saas.framework.common.dto.AppVersionRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.AppVersion;
import com.saas.framework.mapper.AppVersionMapper;
import com.saas.framework.service.AppVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * APP版本管理服务实现
 */
@Slf4j
@Service
public class AppVersionServiceImpl implements AppVersionService {

    @Resource
    private AppVersionMapper appVersionMapper;

    @Override
    public Map<String, Object> checkUpdate(AppVersionCheckRequest request) {
        log.info("检查更新: currentVersionCode={}, platform={}", request.getCurrentVersionCode(), request.getPlatform());

        // 查询该平台最新启用的版本
        LambdaQueryWrapper<AppVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppVersion::getPlatform, request.getPlatform());
        wrapper.eq(AppVersion::getStatus, 1);
        // 全局版本（tenant_id=0）或当前租户版本（如果已登录）
        Long tenantId = UserContext.getTenantId();
        if (tenantId != null && tenantId != 0) {
            wrapper.and(w -> w.eq(AppVersion::getTenantId, 0).or().eq(AppVersion::getTenantId, tenantId));
        } else {
            wrapper.eq(AppVersion::getTenantId, 0);
        }
        wrapper.orderByDesc(AppVersion::getVersionCode);
        wrapper.last("LIMIT 1");

        AppVersion latestVersion = appVersionMapper.selectOne(wrapper);
        if (latestVersion == null) {
            return null;
        }

        // 比较版本号
        if (latestVersion.getVersionCode() <= request.getCurrentVersionCode()) {
            return null;
        }

        // 有新版本
        Map<String, Object> result = new HashMap<>();
        result.put("hasUpdate", true);
        result.put("versionCode", latestVersion.getVersionCode());
        result.put("versionName", latestVersion.getVersionName());
        result.put("downloadUrl", latestVersion.getDownloadUrl());
        result.put("fileSize", latestVersion.getFileSize());
        result.put("md5", latestVersion.getMd5());
        result.put("updateContent", latestVersion.getUpdateContent());
        result.put("forceUpdate", latestVersion.getForceUpdate());

        log.info("发现新版本: versionName={}, forceUpdate={}", latestVersion.getVersionName(), latestVersion.getForceUpdate());
        return result;
    }

    @Override
    public void create(AppVersionRequest request) {
        log.info("创建APP版本: versionName={}, platform={}", request.getVersionName(), request.getPlatform());

        AppVersion version = new AppVersion();
        version.setVersionCode(request.getVersionCode());
        version.setVersionName(request.getVersionName());
        version.setPlatform(request.getPlatform());
        version.setDownloadUrl(request.getDownloadUrl());
        version.setFileSize(request.getFileSize());
        version.setMd5(request.getMd5());
        version.setUpdateContent(request.getUpdateContent());
        version.setForceUpdate(request.getForceUpdate() != null ? request.getForceUpdate() : 0);
        version.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        version.setTenantId(UserContext.isSuperAdmin() ? 0L : UserContext.getTenantId());

        appVersionMapper.insert(version);
        log.info("APP版本创建成功: id={}", version.getId());
    }

    @Override
    public void update(Long id, AppVersionRequest request) {
        log.info("修改APP版本: id={}", id);

        AppVersion version = appVersionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }

        version.setVersionCode(request.getVersionCode());
        version.setVersionName(request.getVersionName());
        version.setPlatform(request.getPlatform());
        version.setDownloadUrl(request.getDownloadUrl());
        version.setFileSize(request.getFileSize());
        version.setMd5(request.getMd5());
        version.setUpdateContent(request.getUpdateContent());
        version.setForceUpdate(request.getForceUpdate() != null ? request.getForceUpdate() : version.getForceUpdate());
        if (request.getStatus() != null) {
            version.setStatus(request.getStatus());
        }

        appVersionMapper.updateById(version);
        log.info("APP版本修改成功: id={}", id);
    }

    @Override
    public void delete(Long id) {
        log.info("删除APP版本: id={}", id);

        AppVersion version = appVersionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }

        appVersionMapper.deleteById(id);
        log.info("APP版本删除成功: id={}", id);
    }

    @Override
    public IPage<AppVersion> page(int page, int size, String platform) {
        LambdaQueryWrapper<AppVersion> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(platform)) {
            wrapper.eq(AppVersion::getPlatform, platform);
        }
        if (!UserContext.isSuperAdmin()) {
            wrapper.and(w -> w.eq(AppVersion::getTenantId, 0).or().eq(AppVersion::getTenantId, UserContext.getTenantId()));
        }
        wrapper.orderByDesc(AppVersion::getVersionCode);
        return appVersionMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public AppVersion getById(Long id) {
        AppVersion version = appVersionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        return version;
    }
}

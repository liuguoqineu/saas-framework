package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.entity.SysOperationLog;
import com.saas.framework.mapper.SysOperationLogMapper;
import com.saas.framework.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Resource
    private SysOperationLogMapper sysOperationLogMapper;

    @Override
    public IPage<SysOperationLog> page(int page, int size, String username, String operation, String module, String ip, String startTime, String endTime) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(SysOperationLog::getTenantId, UserContext.getTenantId());
        }

        if (StringUtils.hasText(username)) {
            wrapper.like(SysOperationLog::getUsername, username);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.eq(SysOperationLog::getOperation, operation);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysOperationLog::getModule, module);
        }
        if (StringUtils.hasText(ip)) {
            wrapper.like(SysOperationLog::getIp, ip);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(SysOperationLog::getCreateTime, startTime);
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(SysOperationLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(SysOperationLog::getCreateTime);
        return sysOperationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void save(SysOperationLog operationLog) {
        sysOperationLogMapper.insert(operationLog);
    }
}

package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.entity.SysOperationLog;

public interface OperationLogService {

    IPage<SysOperationLog> page(int page, int size, String username, String operation, String module, String ip, String startTime, String endTime);

    void save(SysOperationLog operationLog);
}

package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.SysOperationLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE tenant_id = #{tenantId} AND create_time >= #{startTime}")
    int countByTenantToday(@Param("tenantId") Long tenantId, @Param("startTime") String startTime);
}

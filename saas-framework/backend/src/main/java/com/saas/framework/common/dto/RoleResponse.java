package com.saas.framework.common.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RoleResponse {

    private Long id;
    private String name;
    private Long tenantId;
    private List<Long> permissionIds;
    private List<Map<String, Object>> permissions;
    private Long userCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

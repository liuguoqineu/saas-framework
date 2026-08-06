package com.saas.framework.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardOverview {
    private String period;
    private Map<String, Object> fillRate;
    private Map<String, Object> approvalRate;
    private List<Map<String, Object>> overdueList;
}

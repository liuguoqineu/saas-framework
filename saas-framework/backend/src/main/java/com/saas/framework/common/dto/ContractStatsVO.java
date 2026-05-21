package com.saas.framework.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ContractStatsVO {

    private long totalCount;

    private long activeCount;

    private long terminatedCount;

    private List<Map<String, Object>> byType;

    private List<Map<String, Object>> byMonth;

    private List<Map<String, Object>> byStatus;

    private List<Map<String, Object>> revenueByMonth;
}

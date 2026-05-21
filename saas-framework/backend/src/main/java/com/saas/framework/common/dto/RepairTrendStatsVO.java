package com.saas.framework.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RepairTrendStatsVO {

    private long totalCount;

    private long resolvedCount;

    private long unresolvedCount;

    private long processingCount;

    private long exceptionCount;

    private List<Map<String, Object>> byMonth;

    private List<Map<String, Object>> byCustomerType;

    private List<Map<String, Object>> byFaultType;

    private List<Map<String, Object>> highFrequencyCustomers;

    private List<Map<String, Object>> highFrequencyFaultTypes;
}

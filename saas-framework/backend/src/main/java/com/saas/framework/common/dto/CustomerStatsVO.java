package com.saas.framework.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CustomerStatsVO {

    private long totalCount;

    private long validCount;

    private long invalidCount;

    private CustomerSubStats validCustomers;

    private CustomerSubStats invalidCustomers;

    @Data
    public static class CustomerSubStats {
        private List<Map<String, Object>> byBusinessCategory;
        private List<Map<String, Object>> byCooperationStatus;
        private List<Map<String, Object>> byRegion;
        private List<Map<String, Object>> byBusinessType;
    }
}

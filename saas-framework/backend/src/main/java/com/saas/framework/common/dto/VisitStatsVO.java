package com.saas.framework.common.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class VisitStatsVO {

    private long totalVisits;

    private long completedVisits;

    private double completionRate;

    private long coveredCustomers;

    private long totalCustomers;

    private double coverageRate;

    private List<Map<String, Object>> byPerson;

    private List<Map<String, Object>> byMethod;

    private List<Map<String, Object>> byMonth;

    private List<Map<String, Object>> byCustomerType;
}

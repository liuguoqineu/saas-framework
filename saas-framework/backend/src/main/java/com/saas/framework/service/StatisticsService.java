package com.saas.framework.service;

import com.saas.framework.common.dto.*;

import javax.servlet.http.HttpServletResponse;

public interface StatisticsService {

    CustomerStatsVO customerStats(String startDate, String endDate);

    RepairTrendStatsVO repairStats(String startDate, String endDate, String period);

    VisitStatsVO visitStats(String startDate, String endDate);

    ContractStatsVO contractStats(String startDate, String endDate);

    void exportCustomerStats(HttpServletResponse response, String startDate, String endDate);

    void exportRepairStats(HttpServletResponse response, String startDate, String endDate, String period);

    void exportVisitStats(HttpServletResponse response, String startDate, String endDate);

    void exportContractStats(HttpServletResponse response, String startDate, String endDate);
}

package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.ApprovalChainItemDTO;
import com.saas.framework.common.dto.ApprovalRequest;
import com.saas.framework.common.dto.DashboardOverview;
import com.saas.framework.common.dto.ReportRequest;
import com.saas.framework.entity.report.RpApproval;
import com.saas.framework.entity.report.RpTemplate;
import com.saas.framework.entity.report.RpReport;
import com.saas.framework.entity.report.RpReportRevision;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ReportService {

    List<RpTemplate> getTemplates();

    RpTemplate getTemplate(Long id);

    IPage<RpReport> page(int page, int size, Long userId, Long deptId, String reportType, String reportPeriod, String status, String startDate, String endDate);

    RpReport getReport(Long id);

    RpReport createOrSave(ReportRequest request);

    RpReport update(Long id, ReportRequest request);

    void deleteDraft(Long id);

    void submit(Long id);

    void resubmit(Long id, ReportRequest request);

    List<RpApproval> getPendingApprovals();

    void approve(Long approvalId);

    void reject(Long approvalId, ApprovalRequest request);

    List<ApprovalChainItemDTO> getApprovalChain(Long reportId);

    List<RpReportRevision> getRevisions(Long reportId);

    void exportExcel(HttpServletResponse response, Long userId, Long deptId, String reportType, String reportPeriod, String status, String startDate, String endDate);

    void exportPdf(HttpServletResponse response, Long reportId);

    DashboardOverview getDashboardOverview();

    Map<String, Object> getDashboardByPost(String postType);

    List<Map<String, Object>> getOverdueList();

    void exportOverdueExcel(HttpServletResponse response);
}

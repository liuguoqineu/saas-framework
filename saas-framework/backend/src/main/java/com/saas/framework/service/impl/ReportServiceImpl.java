package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.*;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.common.util.PdfExportUtil;
import com.saas.framework.entity.SysUser;
import com.saas.framework.entity.SysTenant;
import com.saas.framework.entity.report.*;
import com.saas.framework.mapper.*;
import com.saas.framework.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final int MAX_APPROVAL_LEVEL = 10;

    @Resource private RpTemplateMapper rpTemplateMapper;
    @Resource private RpReportMapper rpReportMapper;
    @Resource private RpApprovalMapper rpApprovalMapper;
    @Resource private RpReportRevisionMapper rpReportRevisionMapper;
    @Resource private RpOverdueMapper rpOverdueMapper;
    @Resource private RpConfigMapper rpConfigMapper;
    @Resource private SysUserMapper sysUserMapper;
    @Resource private SysTenantMapper sysTenantMapper;

    // ========== Templates ==========

    @Override
    public List<RpTemplate> getTemplates() {
        SysUser user = sysUserMapper.selectById(UserContext.getUserId());
        String postType = user.getPostType();
        if (!StringUtils.hasText(postType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<RpTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpTemplate::getPostType, postType)
               .eq(RpTemplate::getIsEnabled, 1)
               .orderByAsc(RpTemplate::getReportType, RpTemplate::getTemplateCode);
        return rpTemplateMapper.selectList(wrapper);
    }

    @Override
    public RpTemplate getTemplate(Long id) {
        RpTemplate template = rpTemplateMapper.selectById(id);
        if (template == null) throw new BusinessException(404, "模板不存在");
        return template;
    }

    // ========== Report CRUD ==========

    @Override
    public IPage<RpReport> page(int page, int size, Long userId, Long deptId, String reportType,
                                String reportPeriod, String status, String startDate, String endDate) {
        LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(RpReport::getTenantId, UserContext.getTenantId());
            if (userId == null && !isTenantManager()) {
                wrapper.eq(RpReport::getUserId, UserContext.getUserId());
            }
        }
        if (userId != null) wrapper.eq(RpReport::getUserId, userId);
        if (deptId != null) wrapper.eq(RpReport::getDeptId, deptId);
        if (StringUtils.hasText(reportType)) wrapper.eq(RpReport::getReportType, reportType);
        if (StringUtils.hasText(reportPeriod)) wrapper.eq(RpReport::getReportPeriod, reportPeriod);
        if (StringUtils.hasText(status)) wrapper.eq(RpReport::getStatus, status);
        if (StringUtils.hasText(startDate)) wrapper.ge(RpReport::getCreateTime, startDate);
        if (StringUtils.hasText(endDate)) wrapper.le(RpReport::getCreateTime, endDate);
        wrapper.orderByDesc(RpReport::getCreateTime);
        return rpReportMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public RpReport getReport(Long id) {
        RpReport report = rpReportMapper.selectById(id);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!UserContext.isSuperAdmin() && !report.getTenantId().equals(UserContext.getTenantId()))
            throw new BusinessException(403, "无权限查看此报表");
        checkNotDraft(report);
        return report;
    }

    @Override
    @Transactional
    public RpReport createOrSave(ReportRequest request) {
        Long userId = UserContext.getUserId();
        Long tenantId = UserContext.getTenantId();
        
        Long templateId = request.getTemplateId();
        if (templateId == null) {
            templateId = resolveDefaultTemplate(userId, request.getReportType());
        }
        
        RpReport report = new RpReport();
        report.setUserId(userId);
        report.setTemplateId(templateId);
        report.setReportType(request.getReportType());
        report.setReportPeriod(request.getReportPeriod());
        report.setContentText(request.getContentText());
        report.setStatus("DRAFT");
        report.setTenantId(tenantId != null ? tenantId : 0L);
        rpReportMapper.insert(report);
        log.info("report draft created: id={}, templateId={}", report.getId(), templateId);
        return report;
    }

    private Long resolveDefaultTemplate(Long userId, String reportType) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getPostType())) {
            return null;
        }
        LambdaQueryWrapper<RpTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpTemplate::getPostType, user.getPostType())
               .eq(RpTemplate::getReportType, reportType)
               .eq(RpTemplate::getIsEnabled, 1)
               .last("LIMIT 1");
        RpTemplate template = rpTemplateMapper.selectOne(wrapper);
        return template != null ? template.getId() : null;
    }

    @Override
    @Transactional
    public RpReport update(Long id, ReportRequest request) {
        RpReport report = rpReportMapper.selectById(id);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!report.getUserId().equals(UserContext.getUserId()) && !UserContext.isSuperAdmin())
            throw new BusinessException(403, "只能编辑自己的报表");
        if ("DRAFT".equals(report.getStatus()) || "REJECTED".equals(report.getStatus())) {
            report.setContentText(request.getContentText());
            if (StringUtils.hasText(request.getReportPeriod())) {
                report.setReportPeriod(request.getReportPeriod());
            }
            if (StringUtils.hasText(request.getReportType())) {
                report.setReportType(request.getReportType());
            }
            rpReportMapper.updateById(report);
        } else {
            throw new BusinessException("只能编辑草稿或被驳回的报表");
        }
        return report;
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        RpReport report = rpReportMapper.selectById(id);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!"DRAFT".equals(report.getStatus())) throw new BusinessException("只能删除草稿状态的报表");
        if (!report.getUserId().equals(UserContext.getUserId()) && !UserContext.isSuperAdmin())
            throw new BusinessException(403, "只能删除自己的草稿");
        rpReportMapper.deleteById(id);
        log.info("draft deleted: id={}", id);
    }

    // ========== Submit & Approval ==========

    @Override
    @Transactional
    public void submit(Long id) {
        RpReport report = rpReportMapper.selectById(id);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!report.getUserId().equals(UserContext.getUserId())) throw new BusinessException(403, "只能提交自己的报表");
        report.setStatus("SUBMITTED");
        report.setSubmitTime(LocalDateTime.now());
        rpReportMapper.updateById(report);

        saveRevision(report.getId(), "SUBMITTED", report.getContentText());

        SysUser user = sysUserMapper.selectById(report.getUserId());
        Long adminId = getTenantAdminId(report.getTenantId());
        if (adminId != null) {
            RpApproval approval = new RpApproval();
            approval.setReportId(report.getId());
            approval.setApproverId(adminId);
            approval.setApprovalLevel(1);
            approval.setStatus("PENDING");
            rpApprovalMapper.insert(approval);

            SysUser admin = sysUserMapper.selectById(adminId);
            log.info("审批任务已创建: reportId={}, approverId={}", report.getId(), adminId);
        } else {
            report.setStatus("APPROVED");
            rpReportMapper.updateById(report);

            saveRevision(report.getId(), "APPROVED", report.getContentText());

            log.info("no tenant admin, auto-approved: id={}", report.getId());
        }
        log.info("report submitted: id={}", report.getId());
    }

    @Override
    @Transactional
    public void resubmit(Long id, ReportRequest request) {
        RpReport report = rpReportMapper.selectById(id);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!report.getUserId().equals(UserContext.getUserId()) && !UserContext.isSuperAdmin())
            throw new BusinessException(403, "只能重新提交自己的报表");
        if (!"REJECTED".equals(report.getStatus())) throw new BusinessException("只有被驳回的报表才能重新提交");

        LambdaQueryWrapper<RpApproval> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RpApproval::getReportId, id)
                .eq(RpApproval::getStatus, "PENDING");
        List<RpApproval> pendingApprovals = rpApprovalMapper.selectList(pendingWrapper);
        for (RpApproval old : pendingApprovals) {
            old.setStatus("SUPERSEDED");
            rpApprovalMapper.updateById(old);
        }

        report.setContentText(request.getContentText());
        report.setStatus("SUBMITTED");
        report.setSubmitTime(LocalDateTime.now());
        rpReportMapper.updateById(report);

        saveRevision(report.getId(), "RESUBMIT", report.getContentText());

        SysUser user = sysUserMapper.selectById(report.getUserId());
        Long adminId = getTenantAdminId(report.getTenantId());
        if (adminId != null) {
            RpApproval approval = new RpApproval();
            approval.setReportId(report.getId());
            approval.setApproverId(adminId);
            approval.setApprovalLevel(1);
            approval.setStatus("PENDING");
            rpApprovalMapper.insert(approval);

            log.info("审批任务已重新创建: reportId={}, approverId={}", report.getId(), adminId);
        } else {
            report.setStatus("APPROVED");
            rpReportMapper.updateById(report);
            saveRevision(report.getId(), "APPROVED", report.getContentText());
            log.info("no tenant admin, auto-approved on resubmit: id={}", report.getId());
        }
        log.info("report resubmitted: id={}", report.getId());
    }

    @Override
    public List<RpApproval> getPendingApprovals() {
        return rpApprovalMapper.selectPendingByApproverId(UserContext.getUserId());
    }

    @Override
    public List<ApprovalChainItemDTO> getApprovalChain(Long reportId) {
        RpReport report = rpReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!UserContext.isSuperAdmin() && !report.getTenantId().equals(UserContext.getTenantId()))
            throw new BusinessException(403, "无权限查看此报表审批链");

        LambdaQueryWrapper<RpApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpApproval::getReportId, reportId)
                .orderByAsc(RpApproval::getApprovalLevel)
                .orderByAsc(RpApproval::getCreateTime);
        List<RpApproval> approvals = rpApprovalMapper.selectList(wrapper);
        return approvals.stream().map(a -> {
            ApprovalChainItemDTO dto = new ApprovalChainItemDTO();
            BeanUtils.copyProperties(a, dto);
            SysUser approver = sysUserMapper.selectById(a.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approve(Long approvalId) {
        RpApproval approval = rpApprovalMapper.selectById(approvalId);
        if (approval == null) throw new BusinessException(404, "审批任务不存在");
        if (!approval.getApproverId().equals(UserContext.getUserId())) throw new BusinessException(403, "这不是您的审批任务");
        if (!"PENDING".equals(approval.getStatus())) throw new BusinessException("该审批任务已处理");
        approval.setStatus("APPROVED");
        approval.setApproveTime(LocalDateTime.now());
        rpApprovalMapper.updateById(approval);

        RpReport report = rpReportMapper.selectById(approval.getReportId());
        if (report == null) throw new BusinessException(404, "报表不存在");

        SysUser currentApprover = sysUserMapper.selectById(approval.getApproverId());
        Long nextLeaderId = (currentApprover != null) ? currentApprover.getLeaderId() : null;

        boolean hasNextLevel = false;
        if (nextLeaderId != null && approval.getApprovalLevel() < MAX_APPROVAL_LEVEL) {
            if (!nextLeaderId.equals(report.getUserId())) {
                LambdaQueryWrapper<RpApproval> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(RpApproval::getReportId, report.getId())
                        .eq(RpApproval::getApproverId, nextLeaderId);
                if (rpApprovalMapper.selectCount(checkWrapper) == 0) {
                    RpApproval nextApproval = new RpApproval();
                    nextApproval.setReportId(report.getId());
                    nextApproval.setApproverId(nextLeaderId);
                    nextApproval.setApprovalLevel(approval.getApprovalLevel() + 1);
                    nextApproval.setStatus("PENDING");
                    rpApprovalMapper.insert(nextApproval);
                    hasNextLevel = true;

                    log.info("下一级审批任务已创建: reportId={}, nextApproverId={}, level={}", report.getId(), nextLeaderId, approval.getApprovalLevel() + 1);
                }
            }
        }

        if (!hasNextLevel) {
            report.setStatus("APPROVED");
            rpReportMapper.updateById(report);
            saveRevision(report.getId(), "APPROVED", report.getContentText());
            log.info("报表已通过全部审批: reportId={}, levels={}", report.getId(), approval.getApprovalLevel());
        }
        log.info("report approval level {} approved: reportId={}, hasNextLevel={}",
            approval.getApprovalLevel(), approval.getReportId(), hasNextLevel);
    }

    @Override
    @Transactional
    public void reject(Long approvalId, ApprovalRequest request) {
        RpApproval approval = rpApprovalMapper.selectById(approvalId);
        if (approval == null) throw new BusinessException(404, "审批任务不存在");
        if (!approval.getApproverId().equals(UserContext.getUserId())) throw new BusinessException(403, "这不是您的审批任务");
        if (!"PENDING".equals(approval.getStatus())) throw new BusinessException("该审批任务已处理");
        approval.setStatus("REJECTED");
        approval.setComment(request.getComment());
        approval.setApproveTime(LocalDateTime.now());
        rpApprovalMapper.updateById(approval);

        LambdaQueryWrapper<RpApproval> otherPendingWrapper = new LambdaQueryWrapper<>();
        otherPendingWrapper.eq(RpApproval::getReportId, approval.getReportId())
                .eq(RpApproval::getStatus, "PENDING")
                .ne(RpApproval::getId, approvalId);
        List<RpApproval> otherPending = rpApprovalMapper.selectList(otherPendingWrapper);
        for (RpApproval other : otherPending) {
            other.setStatus("SUPERSEDED");
            rpApprovalMapper.updateById(other);
        }

        RpReport report = rpReportMapper.selectById(approval.getReportId());
        if (report != null) {
            report.setStatus("REJECTED");
            rpReportMapper.updateById(report);

            saveRevision(report.getId(), "REJECTED", report.getContentText());

            log.info("报表被驳回: reportId={}, level={}, reason={}", report.getId(), approval.getApprovalLevel(), request.getComment());
        }
        log.info("report rejected: reportId={}, level={}, reason={}", approval.getReportId(), approval.getApprovalLevel(), request.getComment());
    }

    // ========== Revisions ==========

    @Override
    public List<RpReportRevision> getRevisions(Long reportId) {
        RpReport report = rpReportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(404, "报表不存在");
        if (!UserContext.isSuperAdmin() && !report.getTenantId().equals(UserContext.getTenantId()))
            throw new BusinessException(403, "无权限查看此报表修改记录");

        LambdaQueryWrapper<RpReportRevision> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpReportRevision::getReportId, reportId).orderByDesc(RpReportRevision::getCreateTime);
        return rpReportRevisionMapper.selectList(wrapper);
    }

    // ========== Export ==========

    @Override
    public void exportExcel(HttpServletResponse response, Long userId, Long deptId, String reportType,
                            String reportPeriod, String status, String startDate, String endDate) {
        IPage<RpReport> page = page(1, 500, userId, deptId, reportType, reportPeriod, status, startDate, endDate);
        List<RpReport> reports = page.getRecords();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reports");
            Row header = sheet.createRow(0);
            String[] titles = {"ID", "UserID", "Type", "Period", "Status", "SubmitTime", "Content"};
            for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
            int rowIdx = 1;
            for (RpReport r : reports) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getUserId());
                row.createCell(2).setCellValue(r.getReportType());
                row.createCell(3).setCellValue(r.getReportPeriod());
                row.createCell(4).setCellValue(r.getStatus());
                row.createCell(5).setCellValue(r.getSubmitTime() != null ? r.getSubmitTime().toString() : "");
                row.createCell(6).setCellValue(r.getContentText());
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("Reports.xlsx", "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportPdf(HttpServletResponse response, Long reportId) {
        try {
            RpReport report = getReport(reportId);
            SysUser user = sysUserMapper.selectById(report.getUserId());
            RpTemplate template = report.getTemplateId() != null ? rpTemplateMapper.selectById(report.getTemplateId()) : null;

            String fileName = getReportTypeName(report.getReportType()) + "_" + report.getReportPeriod() + ".pdf";
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

            PdfExportUtil.exportReportPdf(report, template, user, response.getOutputStream());

            log.info("PDF exported successfully: reportId={}, fileName={}", reportId, fileName);
        } catch (Exception e) {
            log.error("PDF export failed for reportId={}: {}", reportId, e.getMessage(), e);
            throw new BusinessException("PDF导出失败: " + e.getMessage());
        }
    }

    private String getReportTypeName(String reportType) {
        if (reportType == null) return "工作报表";
        switch (reportType.toUpperCase()) {
            case "DAILY": return "日报";
            case "WEEKLY": return "周报";
            case "MONTHLY": return "月报";
            default: return "工作报表";
        }
    }

    private void checkNotDraft(RpReport report) {
        if ("DRAFT".equals(report.getStatus())) {
            throw new BusinessException("草稿状态的报表不能查看或导出，请先提交报表");
        }
    }

    private boolean isTenantManager() {
        List<String> permissions = UserContext.getPermissions();
        return permissions != null && 
               (permissions.contains("user:list") || permissions.contains("report:approve"));
    }

    private String getSubmitterName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知";
    }

    private Long getTenantAdminId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) return null;
        SysTenant tenant = sysTenantMapper.selectById(tenantId);
        if (tenant != null && tenant.getAdminUserId() != null) {
            return tenant.getAdminUserId();
        }
        log.warn("未找到租户管理员: tenantId={}", tenantId);
        return null;
    }

    // ========== Dashboard ==========

    @Override
    public DashboardOverview getDashboardOverview() {
        DashboardOverview result = new DashboardOverview();
        LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) wrapper.eq(RpReport::getTenantId, UserContext.getTenantId());
        List<RpReport> all = rpReportMapper.selectList(wrapper);
        long total = all.size();
        long filled = all.stream().filter(r -> !"DRAFT".equals(r.getStatus())).count();
        long submitted = all.stream().filter(r -> "SUBMITTED".equals(r.getStatus()) || "APPROVED".equals(r.getStatus())).count();
        long approved = all.stream().filter(r -> "APPROVED".equals(r.getStatus())).count();
        Map<String, Object> fill = new HashMap<>();
        fill.put("total", total);
        fill.put("filled", filled);
        fill.put("rate", total > 0 ? Math.round(filled * 10000.0 / total) / 100.0 : 0);
        Map<String, Object> appr = new HashMap<>();
        appr.put("submitted", submitted);
        appr.put("approved", approved);
        appr.put("rate", submitted > 0 ? Math.round(approved * 10000.0 / submitted) / 100.0 : 0);
        result.setPeriod(LocalDateTime.now().toLocalDate().toString());
        result.setFillRate(fill);
        result.setApprovalRate(appr);
        result.setOverdueList(getOverdueList());
        return result;
    }

    @Override
    public Map<String, Object> getDashboardByPost(String postType) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) wrapper.eq(RpReport::getTenantId, UserContext.getTenantId());
        List<RpReport> all = rpReportMapper.selectList(wrapper);
        List<RpReport> filtered = all.stream().filter(r -> {
            SysUser u = sysUserMapper.selectById(r.getUserId());
            return u != null && postType.equals(u.getPostType());
        }).toList();
        result.put("total", filtered.size());
        result.put("postType", postType);
        return result;
    }

    // ========== Overdue ==========

    @Override
    public List<Map<String, Object>> getOverdueList() {
        LambdaQueryWrapper<RpOverdue> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) wrapper.eq(RpOverdue::getTenantId, UserContext.getTenantId());
        wrapper.orderByDesc(RpOverdue::getCreateTime);
        List<RpOverdue> overdues = rpOverdueMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RpOverdue o : overdues) {
            Map<String, Object> item = new HashMap<>();
            SysUser u = sysUserMapper.selectById(o.getUserId());
            item.put("userId", o.getUserId());
            item.put("userName", u != null ? u.getRealName() : "unknown");
            item.put("reportType", o.getReportType());
            item.put("reportPeriod", o.getReportPeriod());
            item.put("deadline", o.getDeadline() != null ? o.getDeadline().toString() : "");
            item.put("isReminded", o.getIsReminded());
            result.add(item);
        }
        return result;
    }

    @Override
    public void exportOverdueExcel(HttpServletResponse response) {
        List<Map<String, Object>> overdues = getOverdueList();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Overdue");
            Row header = sheet.createRow(0);
            String[] titles = {"UserID", "Name", "Type", "Period", "Deadline", "Reminded"};
            for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
            int rowIdx = 1;
            for (Map<String, Object> o : overdues) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(String.valueOf(o.get("userId")));
                row.createCell(1).setCellValue(String.valueOf(o.get("userName")));
                row.createCell(2).setCellValue(String.valueOf(o.get("reportType")));
                row.createCell(3).setCellValue(String.valueOf(o.get("reportPeriod")));
                row.createCell(4).setCellValue(String.valueOf(o.get("deadline")));
                row.createCell(5).setCellValue(("1".equals(String.valueOf(o.get("isReminded")))) ? "Yes" : "No");
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("Overdue.xlsx", "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private void saveRevision(Long reportId, String revisionType, String contentSnapshot) {
        try {
            RpReportRevision revision = new RpReportRevision();
            revision.setReportId(reportId);
            revision.setRevisionType(revisionType);
            revision.setContentSnapshot(contentSnapshot);
            revision.setOperatorId(UserContext.getUserId());
            rpReportRevisionMapper.insert(revision);
            log.info("修改记录已保存: reportId={}, type={}", reportId, revisionType);
        } catch (Exception e) {
            log.error("保存修改记录失败: reportId={}, type={}", reportId, revisionType, e);
        }
    }
}

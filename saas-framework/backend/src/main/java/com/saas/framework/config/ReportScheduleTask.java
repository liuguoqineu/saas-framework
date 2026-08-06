package com.saas.framework.config;

import com.saas.framework.entity.report.RpApproval;
import com.saas.framework.entity.report.RpConfig;
import com.saas.framework.entity.report.RpOverdue;
import com.saas.framework.entity.report.RpReport;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
public class ReportScheduleTask {

    @Resource
    private RpReportMapper rpReportMapper;
    @Resource
    private RpOverdueMapper rpOverdueMapper;
    @Resource
    private RpConfigMapper rpConfigMapper;
    @Resource
    private RpApprovalMapper rpApprovalMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 逾期检测任务 - 每小时执行一次
     * 检测未填报/未审批的报表，标记为逾期并记录到rp_overdue表
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Async
    public void overdueCheckTask() {
        log.info("========== 开始执行逾期检测任务 ==========");
        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            String dailyPeriod = today.format(DATE_FMT);
            String weeklyPeriod = getWeeklyPeriod(today);
            String monthlyPeriod = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            checkOverdueForType("DAILY", dailyPeriod, getDailyDeadline());
            checkOverdueForType("WEEKLY", weeklyPeriod, getWeeklyDeadline(today));
            checkOverdueForType("MONTHLY", monthlyPeriod, getMonthlyDeadline());

            sendOverdueReminders();
            log.info("========== 逾期检测任务执行完成 ==========");
        } catch (Exception e) {
            log.error("========== 逾期检测任务执行失败 ==========", e);
        }
    }

    private void checkOverdueForType(String reportType, String period, LocalTime deadline) {
        if (deadline == null) return;

        List<SysUser> allUsers = sysUserMapper.selectList(null);
        for (SysUser user : allUsers) {
            if (user.getPostType() == null) continue;

            LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RpReport::getUserId, user.getId())
                   .eq(RpReport::getReportType, reportType)
                   .eq(RpReport::getReportPeriod, period)
                   .ne(RpReport::getStatus, "DRAFT");
            RpReport existing = rpReportMapper.selectOne(wrapper);

            if (existing == null || "DRAFT".equals(existing.getStatus())) {
                LocalDateTime deadLine = LocalDateTime.of(LocalDate.now(), deadline);
                if (LocalDateTime.now().isAfter(deadLine)) {
                    LambdaQueryWrapper<RpOverdue> ovWrapper = new LambdaQueryWrapper<>();
                    ovWrapper.eq(RpOverdue::getUserId, user.getId())
                            .eq(RpOverdue::getReportType, reportType)
                            .eq(RpOverdue::getReportPeriod, period);
                    RpOverdue existingOverdue = rpOverdueMapper.selectOne(ovWrapper);

                    if (existingOverdue == null) {
                        RpOverdue overdue = new RpOverdue();
                        overdue.setUserId(user.getId());
                        overdue.setReportType(reportType);
                        overdue.setReportPeriod(period);
                        overdue.setDeadline(deadLine);
                        overdue.setIsReminded(0);
                        overdue.setTenantId(user.getTenantId() != null ? user.getTenantId() : 0L);
                        rpOverdueMapper.insert(overdue);
                        log.info("新增逾期记录: userId={}, type={}, period={}", user.getId(), reportType, period);
                    }
                }
            }
        }
    }

    /**
     * 填报提醒任务 - 每日17:00执行（截止前1小时提醒）
     */
    @Scheduled(cron = "0 0 17 * * ?")
    @Async
    public void fillRemindTask() {
        log.info("========== 开始执行填报提醒任务 ==========");
        try {
            LocalDate today = LocalDate.now();
            remindUsersForType("DAILY", today.format(DATE_FMT), "日报");
            remindUsersForType("WEEKLY", getWeeklyPeriod(today), "周报");

            if (today.getDayOfMonth() == 3) {
                remindUsersForType("MONTHLY", today.format(DateTimeFormatter.ofPattern("yyyy-MM")), "月报");
            }

            log.info("========== 填报提醒任务执行完成 ==========");
        } catch (Exception e) {
            log.error("========== 填报提醒任务执行失败 ==========", e);
        }
    }

    private void remindUsersForType(String reportType, String period, String typeName) {
        LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpReport::getReportType, reportType)
               .eq(RpReport::getReportPeriod, period)
               .in(RpReport::getStatus, "DRAFT", "SUBMITTED", "APPROVED");
        List<RpReport> submittedReports = rpReportMapper.selectList(wrapper);
        List<Long> submittedUserIds = submittedReports.stream()
                .map(RpReport::getUserId)
                .collect(Collectors.toList());

        List<SysUser> allUsers = sysUserMapper.selectList(null);
        for (SysUser user : allUsers) {
            if (user.getPostType() == null || submittedUserIds.contains(user.getId())) continue;

            log.info("填报提醒: userId={}, type={}, period={}, 尚未提交", user.getId(), typeName, period);
        }
    }

    /**
     * 审批超时提醒 - 每日10:00执行
     * 提醒有超时待审批的领导
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @Async
    public void approvalTimeoutRemindTask() {
        log.info("========== 开始执行审批超时提醒任务 ==========");
        try {
            int timeoutHours = getApprovalTimeoutHours();
            LocalDateTime threshold = LocalDateTime.now().minusHours(timeoutHours);

            LambdaQueryWrapper<RpApproval> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RpApproval::getStatus, "PENDING")
                   .lt(RpApproval::getCreateTime, threshold);
            List<RpApproval> pendingApprovals = rpApprovalMapper.selectList(wrapper);

            for (RpApproval approval : pendingApprovals) {
                long hours = java.time.Duration.between(
                    approval.getCreateTime(), LocalDateTime.now()
                ).toHours();

                log.info("审批超时提醒: approverId={}, 超时{}小时, 待审批{}条",
                    approval.getApproverId(), hours, getCountByApprover(approval.getApproverId()));
            }

            log.info("========== 审批超时提醒任务执行完成，共提醒{}人 ==========",
                pendingApprovals.stream().map(RpApproval::getApproverId).distinct().count());
        } catch (Exception e) {
            log.error("========== 审批超时提醒任务执行失败 ==========", e);
        }
    }

    /**
     * 数据归档任务 - 每月1日凌晨3:00执行
     * 将36个月前的数据标记为已归档（可后续迁移至冷存储）
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    @Async
    public void archiveDataTask() {
        log.info("========== 开始执行数据归档任务 ==========");
        try {
            int retentionMonths = getRetentionMonths();
            LocalDate archiveThreshold = LocalDate.now().minusMonths(retentionMonths);
            String archivePeriod = archiveThreshold.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
            wrapper.le(RpReport::getReportPeriod, archivePeriod)
                   .eq(RpReport::getStatus, "APPROVED");
            long count = rpReportMapper.selectCount(wrapper);

            log.info("数据归档扫描完成：{}个月前的已通过报表共{}条，可考虑迁移至冷存储",
                retentionMonths, count);

            RpConfig config = new RpConfig();
            config.setConfigKey("last_archive_time");
            config.setConfigValue(LocalDateTime.now().toString());
            config.setDescription("上次归档执行时间");
            rpConfigMapper.updateById(config);

            log.info("========== 数据归档任务执行完成 ==========");
        } catch (Exception e) {
            log.error("========== 数据归档任务执行失败 ==========", e);
        }
    }

    private void sendOverdueReminders() {
        LambdaQueryWrapper<RpOverdue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpOverdue::getIsReminded, 0);
        List<RpOverdue> overdues = rpOverdueMapper.selectList(wrapper);

        int maxRemindCount = getMaxRemindCount();
        for (RpOverdue overdue : overdues) {
            int remindCount = getRemindCount(overdue.getUserId(), overdue.getReportType(), overdue.getReportPeriod());
            if (remindCount < maxRemindCount) {
                SysUser user = sysUserMapper.selectById(overdue.getUserId());
                if (user != null) {
                    log.info("逾期提醒: userId={}, type={}, period={}, 已逾期",
                        user.getId(), getTypeName(overdue.getReportType()), overdue.getReportPeriod());

                    overdue.setIsReminded(1);
                    rpOverdueMapper.updateById(overdue);
                }
            }
        }
    }

    // ========== 配置读取工具方法 ==========

    private LocalTime getDailyDeadline() {
        return getConfigValue("daily_deadline", "18:00")
            .map(LocalTime::parse).orElse(null);
    }

    private LocalTime getWeeklyDeadline(LocalDate date) {
        return getConfigValue("weekly_deadline_time", "18:00")
            .map(LocalTime::parse).orElse(null);
    }

    private LocalTime getMonthlyDeadline() {
        return getConfigValue("monthly_deadline_time", "18:00")
            .map(LocalTime::parse).orElse(null);
    }

    private int getApprovalTimeoutHours() {
        return getConfigValue("approval_timeout_hours", "24")
            .map(Integer::parseInt).orElse(24);
    }

    private int getRetentionMonths() {
        return getConfigValue("archive_retention_months", "36")
            .map(Integer::parseInt).orElse(36);
    }

    private int getMaxRemindCount() {
        return getConfigValue("max_remind_count", "3")
            .map(Integer::parseInt).orElse(3);
    }

    private java.util.Optional<String> getConfigValue(String key, String defaultValue) {
        try {
            RpConfig config = rpConfigMapper.selectById(key);
            return java.util.Optional.ofNullable(config != null ? config.getConfigValue() : defaultValue);
        } catch (Exception e) {
            return java.util.Optional.ofNullable(defaultValue);
        }
    }

    private String getWeeklyPeriod(LocalDate date) {
        int startDay = getConfigValue("weekly_start_day", "1")
            .map(Integer::parseInt).orElse(1);
        DayOfWeek startDayOfWeek = DayOfWeek.of(startDay);
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(startDayOfWeek));
        return String.format("%d-W%02d", monday.getYear(), 
            (monday.getDayOfYear() - 1) / 7 + 1);
    }

    private long getCountByApprover(Long approverId) {
        LambdaQueryWrapper<RpApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpApproval::getApproverId, approverId)
               .eq(RpApproval::getStatus, "PENDING");
        return rpApprovalMapper.selectCount(wrapper);
    }

    private int getRemindCount(Long userId, String reportType, String period) {
        LambdaQueryWrapper<RpOverdue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpOverdue::getUserId, userId)
               .eq(RpOverdue::getReportType, reportType)
               .eq(RpOverdue::getReportPeriod, period)
               .eq(RpOverdue::getIsReminded, 1);
        return Math.toIntExact(rpOverdueMapper.selectCount(wrapper));
    }

    private String getTypeName(String reportType) {
        switch (reportType.toUpperCase()) {
            case "DAILY": return "日报";
            case "WEEKLY": return "周报";
            case "MONTHLY": return "月报";
            default: return "报表";
        }
    }
}

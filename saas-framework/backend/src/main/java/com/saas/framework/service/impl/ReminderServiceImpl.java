package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.LoginReminderVO;
import com.saas.framework.entity.*;
import com.saas.framework.entity.report.RpReport;
import com.saas.framework.entity.report.RpTemplate;
import com.saas.framework.mapper.BizContractMapper;
import com.saas.framework.mapper.BizContractReminderMapper;
import com.saas.framework.mapper.BizCustomerMapper;
import com.saas.framework.mapper.BizFollowUpRecordMapper;
import com.saas.framework.mapper.BizRepairOrderMapper;
import com.saas.framework.mapper.RpReportMapper;
import com.saas.framework.mapper.RpTemplateMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ReminderServiceImpl implements ReminderService {

    @Resource
    private BizContractReminderMapper bizContractReminderMapper;

    @Resource
    private BizContractMapper bizContractMapper;

    @Resource
    private BizFollowUpRecordMapper bizFollowUpRecordMapper;

    @Resource
    private BizRepairOrderMapper bizRepairOrderMapper;

    @Resource
    private BizCustomerMapper bizCustomerMapper;

    @Resource
    private RpReportMapper rpReportMapper;

    @Resource
    private RpTemplateMapper rpTemplateMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public LoginReminderVO getLoginReminders() {
        log.info("获取登录提醒 - 当前用户: {}, 租户ID: {}, 是否超管: {}",
                UserContext.getUsername(), UserContext.getTenantId(), UserContext.isSuperAdmin());

        LoginReminderVO vo = new LoginReminderVO();

        List<LoginReminderVO.ReminderItem> contractReminders = null;
        List<LoginReminderVO.ReminderItem> followUpReminders = null;
        List<LoginReminderVO.ReminderItem> repairReminders = null;
        List<LoginReminderVO.ReminderItem> reportReminders = null;

        try {
            if (UserContext.isSuperAdmin() || hasPermission("contract:list")) {
                log.info("用户有合同查看权限，查询合同到期提醒");
                contractReminders = getContractReminders();
            } else {
                log.info("用户无合同查看权限，跳过合同到期提醒");
            }
        } catch (Exception e) {
            log.error("查询合同到期提醒失败", e);
        }

        try {
            if (UserContext.isSuperAdmin() || hasPermission("customer:list")) {
                log.info("用户有客户查看权限，查询客户跟进提醒");
                followUpReminders = getFollowUpReminders();
            } else {
                log.info("用户无客户查看权限，跳过客户跟进提醒");
            }
        } catch (Exception e) {
            log.error("查询客户跟进提醒失败", e);
        }

        try {
            if (UserContext.isSuperAdmin() || hasPermission("repair:list")) {
                log.info("用户有报修查看权限，查询报修处理提醒");
                repairReminders = getRepairReminders();
            } else {
                log.info("用户无报修查看权限，跳过报修处理提醒");
            }
        } catch (Exception e) {
            log.error("查询报修处理提醒失败", e);
        }

        try {
            log.info("查询报表提醒（所有员工均可接收）");
            reportReminders = getReportReminders();
        } catch (Exception e) {
            log.error("查询报表提醒失败", e);
        }

        vo.setContractReminders(contractReminders);
        vo.setFollowUpReminders(followUpReminders);
        vo.setRepairReminders(repairReminders);
        vo.setReportReminders(reportReminders);
        int total = (contractReminders != null ? contractReminders.size() : 0) +
                (followUpReminders != null ? followUpReminders.size() : 0) +
                (repairReminders != null ? repairReminders.size() : 0) +
                (reportReminders != null ? reportReminders.size() : 0);
        vo.setTotalCount(total);

        log.info("登录提醒统计 - 合同到期: {}, 客户跟进: {}, 报修处理: {}, 报表提醒: {}, 总计: {}",
                contractReminders != null ? contractReminders.size() : 0,
                followUpReminders != null ? followUpReminders.size() : 0,
                repairReminders != null ? repairReminders.size() : 0,
                reportReminders != null ? reportReminders.size() : 0,
                vo.getTotalCount());

        return vo;
    }

    private boolean hasPermission(String permission) {
        List<String> permissions = UserContext.getPermissions();
        return permissions != null && permissions.contains(permission);
    }

    private List<LoginReminderVO.ReminderItem> getContractReminders() {
        List<BizContract> contracts;
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizContract::getTenantId, UserContext.getTenantId());
        }
        wrapper.eq(BizContract::getContractStatus, "已生效");
        wrapper.ge(BizContract::getExpireDate, LocalDate.now());
        wrapper.le(BizContract::getExpireDate, LocalDate.now().plusDays(30));
        wrapper.orderByAsc(BizContract::getExpireDate);
        contracts = bizContractMapper.selectList(wrapper);

        List<LoginReminderVO.ReminderItem> items = new ArrayList<>();
        for (BizContract c : contracts) {
            LoginReminderVO.ReminderItem item = new LoginReminderVO.ReminderItem();
            item.setId(c.getId());
            item.setType("CONTRACT");
            item.setTitle("合同到期提醒");
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), c.getExpireDate());
            item.setContent("合同[" + c.getContractNo() + "]客户[" + c.getCustomerName() + "]将于" + c.getExpireDate() + "到期（剩余" + daysBetween + "天）");
            item.setTime(c.getExpireDate().atTime(LocalTime.MIDNIGHT));
            item.setPerson(c.getPersonInCharge());
            item.setIsRead(0);
            item.setRelatedId(c.getId());

            boolean isMine = UserContext.getUsername().equals(c.getPersonInCharge()) ||
                    (UserContext.getUserId() != null && UserContext.getUserId().equals(c.getPersonInChargeId()));
            item.setIsMine(isMine ? 1 : 0);
            items.add(item);
        }
        return items;
    }

    private List<LoginReminderVO.ReminderItem> getFollowUpReminders() {
        LambdaQueryWrapper<BizFollowUpRecord> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizFollowUpRecord::getTenantId, UserContext.getTenantId());
        }
        wrapper.orderByDesc(BizFollowUpRecord::getFollowUpTime);
        List<BizFollowUpRecord> allRecords = bizFollowUpRecordMapper.selectList(wrapper);

        java.util.Map<Long, BizFollowUpRecord> latestByCustomer = new java.util.LinkedHashMap<>();
        for (BizFollowUpRecord r : allRecords) {
            if (r.getCustomerId() == null) continue;
            if (!latestByCustomer.containsKey(r.getCustomerId())) {
                latestByCustomer.put(r.getCustomerId(), r);
            }
        }

        List<LoginReminderVO.ReminderItem> items = new ArrayList<>();
        for (java.util.Map.Entry<Long, BizFollowUpRecord> entry : latestByCustomer.entrySet()) {
            BizFollowUpRecord r = entry.getValue();

            if (r.getFollowUpStatus() != null && r.getFollowUpStatus() != 1) {
                log.debug("客户[{}]最新跟进状态为{}，跳过提醒", r.getCustomerId(), r.getFollowUpStatus());
                continue;
            }

            BizCustomer customer = bizCustomerMapper.selectById(r.getCustomerId());
            if (customer == null || customer.getName() == null) {
                log.debug("跳过客户已被删除的跟进记录[{}]，customerId={}", r.getId(), r.getCustomerId());
                continue;
            }
            if (customer.getDeleted() != null && customer.getDeleted() == 1) {
                log.debug("跳过客户已逻辑删除的跟进记录[{}]，customerId={}", r.getId(), r.getCustomerId());
                continue;
            }
            if ("无效客户".equals(customer.getCooperationStatus())) {
                log.debug("跳过无效客户的跟进记录[{}]，customerId={}", r.getId(), r.getCustomerId());
                continue;
            }
            String customerName = customer.getName();
            LoginReminderVO.ReminderItem item = new LoginReminderVO.ReminderItem();
            item.setId(r.getId());
            item.setType("FOLLOW_UP");
            item.setTitle("跟进提醒");
            item.setContent("客户[" + customerName + "]有待跟进事项：" + (r.getFollowUpContent() != null ? r.getFollowUpContent() : "待跟进"));
            item.setTime(r.getCreateTime());
            item.setPerson(r.getFollowUpPerson());
            item.setIsRead(0);
            item.setRelatedId(r.getCustomerId());

            boolean isMine = UserContext.getUsername().equals(r.getFollowUpPerson()) ||
                    (UserContext.getUserId() != null && UserContext.getUserId().equals(r.getFollowUpPersonId()));
            item.setIsMine(isMine ? 1 : 0);
            items.add(item);
        }
        return items;
    }

    private List<LoginReminderVO.ReminderItem> getRepairReminders() {
        List<BizRepairOrder> orders;
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizRepairOrder::getTenantId, UserContext.getTenantId());
        }
        
        if (UserContext.isSuperAdmin()) {
            wrapper.in(BizRepairOrder::getStatus, Arrays.asList("未处理", "处理中"));
        } else if (hasPermission("repair:process")) {
            wrapper.and(w -> w
                    .and(inner -> inner.eq(BizRepairOrder::getStatus, "未处理"))
                    .or(inner -> inner.eq(BizRepairOrder::getStatus, "处理中")
                            .eq(BizRepairOrder::getAssigneeId, UserContext.getUserId()))
            );
        } else {
            wrapper.eq(BizRepairOrder::getStatus, "未处理");
        }
        
        wrapper.orderByAsc(BizRepairOrder::getCreateTime);
        orders = bizRepairOrderMapper.selectList(wrapper);

        List<LoginReminderVO.ReminderItem> items = new ArrayList<>();
        for (BizRepairOrder o : orders) {
            LoginReminderVO.ReminderItem item = new LoginReminderVO.ReminderItem();
            item.setId(o.getId());
            item.setType("REPAIR");
            item.setTitle("报修处理提醒");
            item.setContent("报修单[" + o.getRepairNo() + "]客户[" + o.getCustomerName() + "]待处理");
            item.setTime(o.getAssignTime() != null ? o.getAssignTime() : o.getCreateTime());
            item.setPerson(o.getAssigneeName());
            item.setIsRead(0);
            item.setRelatedId(o.getId());

            boolean isMine = UserContext.getUsername().equals(o.getAssigneeName()) ||
                    (UserContext.getUserId() != null && UserContext.getUserId().equals(o.getAssigneeId()));
            item.setIsMine(isMine ? 1 : 0);
            items.add(item);
        }
        return items;
    }

    private List<LoginReminderVO.ReminderItem> getReportReminders() {
        List<LoginReminderVO.ReminderItem> items = new ArrayList<>();

        log.info("开始查询报表提醒 - userId: {}", UserContext.getUserId());

        SysUser currentUser = sysUserMapper.selectById(UserContext.getUserId());
        log.info("查询到用户信息 - username: {}, postType: {}, realName: {}",
                currentUser != null ? currentUser.getUsername() : "null",
                currentUser != null ? currentUser.getPostType() : "null",
                currentUser != null ? currentUser.getRealName() : "null");

        if (currentUser == null || currentUser.getPostType() == null) {
            log.info("用户无岗位信息，跳过报表提醒");
            return items;
        }

        LambdaQueryWrapper<RpTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(RpTemplate::getPostType, currentUser.getPostType())
                       .eq(RpTemplate::getIsEnabled, 1);
        List<RpTemplate> templates = rpTemplateMapper.selectList(templateWrapper);

        log.info("查询到报表模板数量: {}, 岗位类型: {}", templates.size(), currentUser.getPostType());

        if (templates.isEmpty()) {
            log.info("用户岗位[{}]无关联报表模板，跳过报表提醒", currentUser.getPostType());
            return items;
        }

        for (RpTemplate template : templates) {
            log.info("处理模板 - code: {}, name: {}, type: {}",
                    template.getTemplateCode(), template.getTemplateName(), template.getReportType());
        }

        LocalDate today = LocalDate.now();
        log.info("当前日期: {}", today);

        for (RpTemplate template : templates) {
            String reportType = template.getReportType();
            String period = calculateCurrentPeriod(today, reportType);

            log.info("检查报表 - type: {}, period: {}", reportType, period);

            boolean submitted = isReportSubmitted(UserContext.getUserId(), reportType, period);
            log.info("报表提交状态 - type: {}, period: {}, 已提交: {}", reportType, period, submitted);

            if (!submitted) {
                LoginReminderVO.ReminderItem item = new LoginReminderVO.ReminderItem();
                item.setId(0L);
                item.setType("REPORT");
                item.setTitle("报表提交提醒");
                String typeName = getReportTypeName(reportType);
                item.setContent(typeName + "[" + period + "]待填写");
                item.setTime(LocalDateTime.now());
                item.setPerson(currentUser.getRealName());
                item.setIsRead(0);
                item.setRelatedId(null);
                item.setIsMine(1);
                items.add(item);

                log.info("✓ 发现未填报报表: userId={}, type={}, period={}",
                        UserContext.getUserId(), reportType, period);
            }
        }

        log.info("报表提醒查询完成 - 共发现 {} 条未填报记录", items.size());

        return items;
    }

    private String calculateCurrentPeriod(LocalDate date, String reportType) {
        switch (reportType.toUpperCase()) {
            case "DAILY":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "WEEKLY":
                return getWeeklyPeriod(date);
            case "MONTHLY":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default:
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    private String getWeeklyPeriod(LocalDate date) {
        DayOfWeek startDayOfWeek = DayOfWeek.MONDAY;
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(startDayOfWeek));
        return String.format("%d-W%02d", monday.getYear(),
                (monday.getDayOfYear() - 1) / 7 + 1);
    }

    private boolean isReportSubmitted(Long userId, String reportType, String period) {
        LambdaQueryWrapper<RpReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpReport::getUserId, userId)
               .eq(RpReport::getReportType, reportType)
               .eq(RpReport::getReportPeriod, period)
               .ne(RpReport::getStatus, "DRAFT");
        Long count = rpReportMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private String getReportTypeName(String reportType) {
        if (reportType == null) return "报表";
        switch (reportType.toUpperCase()) {
            case "DAILY": return "日报";
            case "WEEKLY": return "周报";
            case "MONTHLY": return "月报";
            default: return reportType;
        }
    }
}

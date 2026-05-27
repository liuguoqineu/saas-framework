package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.LoginReminderVO;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.BizContractMapper;
import com.saas.framework.mapper.BizContractReminderMapper;
import com.saas.framework.mapper.BizCustomerMapper;
import com.saas.framework.mapper.BizFollowUpRecordMapper;
import com.saas.framework.mapper.BizRepairOrderMapper;
import com.saas.framework.service.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public LoginReminderVO getLoginReminders() {
        log.info("获取登录提醒 - 当前用户: {}, 租户ID: {}, 是否超管: {}",
                UserContext.getUsername(), UserContext.getTenantId(), UserContext.isSuperAdmin());

        LoginReminderVO vo = new LoginReminderVO();

        List<LoginReminderVO.ReminderItem> contractReminders = null;
        List<LoginReminderVO.ReminderItem> followUpReminders = null;
        List<LoginReminderVO.ReminderItem> repairReminders = null;

        if (UserContext.isSuperAdmin() || hasPermission("contract:list")) {
            log.info("用户有合同查看权限，查询合同到期提醒");
            contractReminders = getContractReminders();
        } else {
            log.info("用户无合同查看权限，跳过合同到期提醒");
        }

        if (UserContext.isSuperAdmin() || hasPermission("customer:list")) {
            log.info("用户有客户查看权限，查询客户跟进提醒");
            followUpReminders = getFollowUpReminders();
        } else {
            log.info("用户无客户查看权限，跳过客户跟进提醒");
        }

        if (UserContext.isSuperAdmin() || hasPermission("repair:list")) {
            log.info("用户有报修查看权限，查询报修处理提醒");
            repairReminders = getRepairReminders();
        } else {
            log.info("用户无报修查看权限，跳过报修处理提醒");
        }

        vo.setContractReminders(contractReminders);
        vo.setFollowUpReminders(followUpReminders);
        vo.setRepairReminders(repairReminders);
        int total = (contractReminders != null ? contractReminders.size() : 0) +
                (followUpReminders != null ? followUpReminders.size() : 0) +
                (repairReminders != null ? repairReminders.size() : 0);
        vo.setTotalCount(total);

        log.info("登录提醒统计 - 合同到期: {}, 客户跟进: {}, 报修处理: {}, 总计: {}",
                contractReminders != null ? contractReminders.size() : 0,
                followUpReminders != null ? followUpReminders.size() : 0,
                repairReminders != null ? repairReminders.size() : 0,
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
        List<BizFollowUpRecord> records;
        LambdaQueryWrapper<BizFollowUpRecord> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizFollowUpRecord::getTenantId, UserContext.getTenantId());
        }
        wrapper.eq(BizFollowUpRecord::getFollowUpStatus, 1);
        wrapper.orderByAsc(BizFollowUpRecord::getCreateTime);
        records = bizFollowUpRecordMapper.selectList(wrapper);

        List<LoginReminderVO.ReminderItem> items = new ArrayList<>();
        for (BizFollowUpRecord r : records) {
            if (r.getCustomerId() == null) {
                log.debug("跳过无客户关联的跟进记录[{}]", r.getId());
                continue;
            }
            BizCustomer customer = bizCustomerMapper.selectById(r.getCustomerId());
            if (customer == null || customer.getName() == null) {
                log.debug("跳过客户已被删除的跟进记录[{}]，customerId={}", r.getId(), r.getCustomerId());
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
            wrapper.eq(BizRepairOrder::getStatus, "未处理");
        } else if (hasPermission("repair:process")) {
            wrapper.eq(BizRepairOrder::getStatus, "处理中");
            wrapper.eq(BizRepairOrder::getAssigneeId, UserContext.getUserId());
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
}

package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.CustomerStatusChangeRequest;
import com.saas.framework.common.dto.FollowUpRecordRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.*;
import com.saas.framework.service.FollowUpService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class FollowUpServiceImpl implements FollowUpService {

    @Resource
    private BizFollowUpRecordMapper followUpRecordMapper;

    @Resource
    private BizCustomerStatusLogMapper customerStatusLogMapper;

    @Resource
    private BizCustomerMapper customerMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public IPage<BizFollowUpRecord> pageRecords(int page, int size, Long customerId, String customerName,
                                                 Long followUpPersonId, String followUpPerson,
                                                 Integer followUpStatus, Integer followUpMethod,
                                                 String startTime, String endTime) {
        LambdaQueryWrapper<BizFollowUpRecord> wrapper = new LambdaQueryWrapper<>();
        if (customerId != null) {
            wrapper.eq(BizFollowUpRecord::getCustomerId, customerId);
        }
        if (followUpPersonId != null) {
            wrapper.eq(BizFollowUpRecord::getFollowUpPersonId, followUpPersonId);
        }
        if (StringUtils.hasText(followUpPerson)) {
            wrapper.like(BizFollowUpRecord::getFollowUpPerson, followUpPerson);
        }
        if (followUpStatus != null) {
            wrapper.eq(BizFollowUpRecord::getFollowUpStatus, followUpStatus);
        }
        if (followUpMethod != null) {
            wrapper.eq(BizFollowUpRecord::getFollowUpMethod, followUpMethod);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(BizFollowUpRecord::getFollowUpTime, LocalDateTime.parse(startTime + " 00:00:00", DATETIME_FORMATTER));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(BizFollowUpRecord::getFollowUpTime, LocalDateTime.parse(endTime + " 23:59:59", DATETIME_FORMATTER));
        }
        if (StringUtils.hasText(customerName)) {
            List<Long> customerIds = getCustomerIdsByName(customerName);
            if (customerIds.isEmpty()) {
                return new Page<>(page, size);
            }
            wrapper.in(BizFollowUpRecord::getCustomerId, customerIds);
        }
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizFollowUpRecord::getTenantId, UserContext.getTenantId());
        }
        wrapper.orderByDesc(BizFollowUpRecord::getFollowUpTime);
        IPage<BizFollowUpRecord> resultPage = followUpRecordMapper.selectPage(new Page<>(page, size), wrapper);
        
        for (BizFollowUpRecord record : resultPage.getRecords()) {
            record.setCustomerName(getCustomerName(record.getCustomerId()));
        }
        
        return resultPage;
    }

    @Override
    public BizFollowUpRecord createRecord(FollowUpRecordRequest request) {
        BizCustomer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        BizFollowUpRecord record = new BizFollowUpRecord();
        record.setCustomerId(request.getCustomerId());
        record.setFollowUpTime(request.getFollowUpTime() != null ? request.getFollowUpTime() : LocalDateTime.now());
        record.setFollowUpPersonId(UserContext.getUserId());
        record.setFollowUpPerson(UserContext.getUsername());
        record.setFollowUpMethod(request.getFollowUpMethod());
        record.setFollowUpContent(request.getFollowUpContent());
        record.setNextPlan(request.getNextPlan());
        record.setFollowUpStatus(request.getFollowUpStatus());

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        record.setTenantId(tenantId != null ? tenantId : 0L);
        record.setCreateBy(UserContext.getUserId());

        followUpRecordMapper.insert(record);

        customer.setFollowUpPersonId(UserContext.getUserId());
        customer.setFollowUpPerson(UserContext.getUsername());
        customerMapper.updateById(customer);

        log.info("新增跟进记录请求参数: customerId={}, newCooperationStatus={}, changeReason={}",
                request.getCustomerId(),
                request.getNewCooperationStatus(),
                request.getChangeReason());

        boolean shouldChange = shouldChangeCooperationStatus(request);
        log.info("是否需要变更合作状态: {}", shouldChange);

        if (shouldChange) {
            changeCooperationStatusForRecord(customer, record.getId(), request);
        }

        log.info("新增跟进记录: id={}, customerId={}", record.getId(), request.getCustomerId());
        return record;
    }

    private boolean shouldChangeCooperationStatus(FollowUpRecordRequest request) {
        return StringUtils.hasText(request.getNewCooperationStatus()) && StringUtils.hasText(request.getChangeReason());
    }

    private void changeCooperationStatusForRecord(BizCustomer customer, Long followUpRecordId, FollowUpRecordRequest request) {
        BizCustomerStatusLog statusLog = new BizCustomerStatusLog();
        statusLog.setCustomerId(customer.getId());
        statusLog.setOldCooperationStatus(customer.getCooperationStatus());
        statusLog.setNewCooperationStatus(request.getNewCooperationStatus());
        statusLog.setChangeReason(request.getChangeReason());
        statusLog.setFollowUpRecordId(followUpRecordId);
        statusLog.setChangePersonId(UserContext.getUserId());
        statusLog.setChangePerson(UserContext.getUsername());
        statusLog.setChangeTime(LocalDateTime.now());

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        statusLog.setTenantId(tenantId != null ? tenantId : 0L);

        customerStatusLogMapper.insert(statusLog);

        customer.setCooperationStatus(request.getNewCooperationStatus());
        customerMapper.updateById(customer);

        log.info("跟进时自动变更客户状态: customerId={}, followUpRecordId={}, {} -> {}",
                customer.getId(), followUpRecordId,
                statusLog.getOldCooperationStatus(),
                request.getNewCooperationStatus());
    }

    @Override
    public BizFollowUpRecord updateRecord(Long id, FollowUpRecordRequest request) {
        BizFollowUpRecord record = followUpRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "跟进记录不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(record.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的跟进记录");
        }

        record.setCustomerId(request.getCustomerId());
        record.setFollowUpTime(request.getFollowUpTime());
        record.setFollowUpMethod(request.getFollowUpMethod());
        record.setFollowUpContent(request.getFollowUpContent());
        record.setNextPlan(request.getNextPlan());
        record.setFollowUpStatus(request.getFollowUpStatus());

        followUpRecordMapper.updateById(record);
        log.info("修改跟进记录: id={}", id);
        return record;
    }

    @Override
    public void deleteRecord(Long id) {
        BizFollowUpRecord record = followUpRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "跟进记录不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(record.getTenantId())) {
            throw new BusinessException(403, "无权删除其他租户的跟进记录");
        }
        followUpRecordMapper.deleteById(id);
        log.info("删除跟进记录: id={}", id);
    }

    @Override
    public BizFollowUpRecord getRecordDetail(Long id) {
        BizFollowUpRecord record = followUpRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "跟进记录不存在");
        }
        return record;
    }

    @Override
    public void exportRecords(HttpServletResponse response, Long customerId, String customerName,
                               Long followUpPersonId, String followUpPerson,
                               Integer followUpStatus, Integer followUpMethod,
                               String startTime, String endTime) {
        IPage<BizFollowUpRecord> page = pageRecords(1, 10000, customerId, customerName,
                followUpPersonId, followUpPerson, followUpStatus, followUpMethod, startTime, endTime);
        List<BizFollowUpRecord> records = page.getRecords();

        String[] methodDesc = {"", "电话", "微信", "邮件", "上门拜访", "其他"};
        String[] statusDesc = {"", "待跟进", "已跟进", "已达成意向"};

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("跟进记录");
            String[] headers = {"客户名称", "跟进时间", "跟进人", "跟进方式", "跟进内容", "下一步计划", "跟进状态", "创建时间"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowIndex = 1;
            for (BizFollowUpRecord r : records) {
                Row row = sheet.createRow(rowIndex++);
                String cName = getCustomerName(r.getCustomerId());
                row.createCell(0).setCellValue(cName);
                row.createCell(1).setCellValue(r.getFollowUpTime() != null ? r.getFollowUpTime().format(DATETIME_FORMATTER) : "");
                row.createCell(2).setCellValue(r.getFollowUpPerson() != null ? r.getFollowUpPerson() : "");
                row.createCell(3).setCellValue(r.getFollowUpMethod() != null && r.getFollowUpMethod() < methodDesc.length ? methodDesc[r.getFollowUpMethod()] : "");
                row.createCell(4).setCellValue(r.getFollowUpContent() != null ? r.getFollowUpContent() : "");
                row.createCell(5).setCellValue(r.getNextPlan() != null ? r.getNextPlan() : "");
                row.createCell(6).setCellValue(r.getFollowUpStatus() != null && r.getFollowUpStatus() < statusDesc.length ? statusDesc[r.getFollowUpStatus()] : "");
                row.createCell(7).setCellValue(r.getCreateTime() != null ? r.getCreateTime().format(DATETIME_FORMATTER) : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("跟进记录.xlsx", "UTF-8"));
            workbook.write(response.getOutputStream());
            log.info("导出跟进记录: 共{}条", records.size());
        } catch (IOException e) {
            log.error("跟进记录导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public List<BizFollowUpRecord> listRecordsByCustomerId(Long customerId) {
        BizCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        LambdaQueryWrapper<BizFollowUpRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizFollowUpRecord::getCustomerId, customerId);
        wrapper.orderByDesc(BizFollowUpRecord::getFollowUpTime);
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizFollowUpRecord::getTenantId, UserContext.getTenantId());
        }
        return followUpRecordMapper.selectList(wrapper);
    }

    @Override
    public void changeCustomerStatus(Long customerId, CustomerStatusChangeRequest request) {
        BizCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(customer.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的客户状态");
        }

        BizCustomerStatusLog statusLog = new BizCustomerStatusLog();
        statusLog.setCustomerId(customerId);
        statusLog.setOldCooperationStatus(customer.getCooperationStatus());
        statusLog.setNewCooperationStatus(request.getNewCooperationStatus());
        statusLog.setChangeReason(request.getChangeReason());
        statusLog.setFollowUpRecordId(request.getFollowUpRecordId());
        statusLog.setChangePersonId(UserContext.getUserId());
        statusLog.setChangePerson(UserContext.getUsername());
        statusLog.setChangeTime(LocalDateTime.now());

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        statusLog.setTenantId(tenantId != null ? tenantId : 0L);

        customerStatusLogMapper.insert(statusLog);

        customer.setCooperationStatus(request.getNewCooperationStatus());
        customerMapper.updateById(customer);

        log.info("变更客户状态: customerId={}, {} -> {}",
                customerId,
                statusLog.getOldCooperationStatus(),
                request.getNewCooperationStatus());
    }

    @Override
    public List<BizCustomerStatusLog> listStatusLogs(Long customerId) {
        BizCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        LambdaQueryWrapper<BizCustomerStatusLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizCustomerStatusLog::getCustomerId, customerId);
        wrapper.orderByDesc(BizCustomerStatusLog::getChangeTime);
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizCustomerStatusLog::getTenantId, UserContext.getTenantId());
        }
        return customerStatusLogMapper.selectList(wrapper);
    }

    private List<Long> getCustomerIdsByName(String customerName) {
        LambdaQueryWrapper<BizCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(BizCustomer::getName, customerName);
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizCustomer::getTenantId, UserContext.getTenantId());
        }
        List<BizCustomer> customers = customerMapper.selectList(wrapper);
        return customers.stream().map(BizCustomer::getId).collect(java.util.stream.Collectors.toList());
    }

    private String getCustomerName(Long customerId) {
        if (customerId == null) return "";
        BizCustomer customer = customerMapper.selectById(customerId);
        return customer != null ? customer.getName() : "";
    }
}

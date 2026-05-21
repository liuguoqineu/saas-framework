package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.ContractRequest;
import com.saas.framework.common.dto.ContractStatusChangeRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.*;
import com.saas.framework.service.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ContractServiceImpl implements ContractService {

    @Resource
    private BizContractMapper bizContractMapper;

    @Resource
    private BizContractAttachmentMapper bizContractAttachmentMapper;

    @Resource
    private BizContractModifyLogMapper bizContractModifyLogMapper;

    @Resource
    private BizContractReminderMapper bizContractReminderMapper;

    @Resource
    private BizCustomerMapper bizCustomerMapper;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String STATUS_ACTIVE = "已生效";
    private static final String STATUS_TERMINATED = "已终止";

    @Override
    public IPage<BizContract> page(int page, int size, String contractNo, String customerName,
                                   String signDateStart, String signDateEnd, String expireDateStart,
                                   String expireDateEnd, String contractStatus) {
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(contractNo)) {
            wrapper.like(BizContract::getContractNo, contractNo);
        }
        if (StringUtils.hasText(customerName)) {
            wrapper.like(BizContract::getCustomerName, customerName);
        }
        if (StringUtils.hasText(signDateStart)) {
            wrapper.ge(BizContract::getSignDate, LocalDate.parse(signDateStart, DATE_FORMATTER));
        }
        if (StringUtils.hasText(signDateEnd)) {
            wrapper.le(BizContract::getSignDate, LocalDate.parse(signDateEnd, DATE_FORMATTER));
        }
        if (StringUtils.hasText(expireDateStart)) {
            wrapper.ge(BizContract::getExpireDate, LocalDate.parse(expireDateStart, DATE_FORMATTER));
        }
        if (StringUtils.hasText(expireDateEnd)) {
            wrapper.le(BizContract::getExpireDate, LocalDate.parse(expireDateEnd, DATE_FORMATTER));
        }
        if (StringUtils.hasText(contractStatus)) {
            wrapper.eq(BizContract::getContractStatus, contractStatus);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizContract::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(BizContract::getCreateTime);

        return bizContractMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public BizContract create(ContractRequest request) {
        if (!StringUtils.hasText(request.getContractNo())) {
            throw new BusinessException("合同编号不能为空");
        }
        if (request.getCustomerId() == null) {
            throw new BusinessException("请选择关联客户");
        }

        BizCustomer customer = bizCustomerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "关联客户不存在");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        Long finalTenantId = tenantId != null ? tenantId : 0L;

        LambdaQueryWrapper<BizContract> noWrapper = new LambdaQueryWrapper<>();
        noWrapper.eq(BizContract::getContractNo, request.getContractNo());
        noWrapper.eq(BizContract::getTenantId, finalTenantId);
        if (bizContractMapper.selectCount(noWrapper) > 0) {
            throw new BusinessException("合同编号已存在");
        }

        BizContract contract = new BizContract();
        copyRequestToEntity(request, contract);
        contract.setCustomerId(request.getCustomerId());
        contract.setCustomerName(customer.getName());
        contract.setContractStatus(STATUS_ACTIVE);
        contract.setTenantId(finalTenantId);

        bizContractMapper.insert(contract);

        if (contract.getExpireDate() != null) {
            generateReminders(contract.getId());
        }

        log.info("新增合同: id={}, contractNo={}, customerName={}", contract.getId(), contract.getContractNo(), contract.getCustomerName());
        return contract;
    }

    @Override
    @Transactional
    public void update(Long id, ContractRequest request) {
        BizContract contract = bizContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(contract.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的合同数据");
        }

        if (STATUS_TERMINATED.equals(contract.getContractStatus())) {
            throw new BusinessException("已终止的合同不可修改");
        }

        if (StringUtils.hasText(request.getContractNo()) && !request.getContractNo().equals(contract.getContractNo())) {
            Long tenantId = contract.getTenantId();
            LambdaQueryWrapper<BizContract> noWrapper = new LambdaQueryWrapper<>();
            noWrapper.eq(BizContract::getContractNo, request.getContractNo());
            noWrapper.eq(BizContract::getTenantId, tenantId);
            if (bizContractMapper.selectCount(noWrapper) > 0) {
                throw new BusinessException("合同编号已存在");
            }
        }

        recordModifyLogs(id, contract, request);

        String oldStatus = contract.getContractStatus();
        copyRequestToEntity(request, contract);
        String newStatus = contract.getContractStatus();

        if (request.getCustomerId() != null) {
            contract.setCustomerId(request.getCustomerId());
            BizCustomer customer = bizCustomerMapper.selectById(request.getCustomerId());
            if (customer != null) {
                contract.setCustomerName(customer.getName());
            }
        }

        bizContractMapper.updateById(contract);

        if (STATUS_TERMINATED.equals(newStatus) && !STATUS_TERMINATED.equals(oldStatus)) {
            syncCustomerStatusOnTerminate(contract);
        }

        if (contract.getExpireDate() != null) {
            LambdaQueryWrapper<BizContractReminder> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(BizContractReminder::getContractId, id);
            bizContractReminderMapper.delete(delWrapper);
            generateReminders(id);
        }

        log.info("修改合同: id={}, contractNo={}", id, contract.getContractNo());
    }

    @Override
    public BizContract detail(Long id) {
        BizContract contract = bizContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(contract.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的合同数据");
        }

        return contract;
    }

    @Override
    public void delete(Long id) {
        BizContract contract = bizContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(contract.getTenantId())) {
            throw new BusinessException(403, "无权删除其他租户的合同数据");
        }

        bizContractMapper.physicalDeleteById(id);
        log.info("彻底删除合同（物理删除）: id={}, contractNo={}", id, contract.getContractNo());
    }

    @Override
    @Transactional
    public void changeStatus(Long id, ContractStatusChangeRequest request) {
        BizContract contract = bizContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(contract.getTenantId())) {
            throw new BusinessException(403, "无权操作其他租户的合同数据");
        }

        String oldStatus = contract.getContractStatus();
        String newStatus = request.getNewStatus();

        validateStatusTransition(oldStatus, newStatus);

        BizContractModifyLog modifyLog = new BizContractModifyLog();
        modifyLog.setContractId(id);
        modifyLog.setFieldName("contractStatus");
        modifyLog.setOldValue(oldStatus);
        modifyLog.setNewValue(newStatus);
        modifyLog.setModifyUserId(UserContext.getUserId());
        modifyLog.setModifyUser(UserContext.getUsername());
        modifyLog.setModifyTime(LocalDateTime.now());
        modifyLog.setTenantId(contract.getTenantId());
        if (StringUtils.hasText(request.getChangeReason())) {
            modifyLog.setNewValue(newStatus + "（原因: " + request.getChangeReason() + "）");
        }
        bizContractModifyLogMapper.insert(modifyLog);

        contract.setContractStatus(newStatus);
        bizContractMapper.updateById(contract);

        if (STATUS_TERMINATED.equals(newStatus)) {
            syncCustomerStatusOnTerminate(contract);
        }
        if (STATUS_ACTIVE.equals(newStatus)) {
            BizCustomer customer = bizCustomerMapper.selectById(contract.getCustomerId());
            if (customer != null) {
                boolean needUpdate = false;
                if (!"已合作".equals(customer.getCooperationCategory())) {
                    customer.setCooperationCategory("已合作");
                    customer.setCooperationStatus("正常履约");
                    needUpdate = true;
                } else if ("终止合作".equals(customer.getCooperationStatus())) {
                    customer.setCooperationStatus("正常履约");
                    needUpdate = true;
                }
                if (needUpdate) {
                    bizCustomerMapper.updateById(customer);
                    log.info("合同生效，同步客户合作状态: customerId={}, cooperationStatus={}", customer.getId(), customer.getCooperationStatus());
                }
            }
        }

        if (STATUS_TERMINATED.equals(newStatus)) {
            LambdaQueryWrapper<BizContractReminder> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(BizContractReminder::getContractId, id);
            bizContractReminderMapper.delete(delWrapper);
        }

        log.info("合同状态变更: id={}, {} -> {}", id, oldStatus, newStatus);
    }

    private void validateStatusTransition(String oldStatus, String newStatus) {
        if (!StringUtils.hasText(newStatus)) {
            throw new BusinessException("新合同状态不能为空");
        }

        Set<String> validStatuses = new HashSet<>(Arrays.asList(STATUS_ACTIVE, STATUS_TERMINATED));
        if (!validStatuses.contains(newStatus)) {
            throw new BusinessException("无效的合同状态: " + newStatus);
        }

        if (STATUS_ACTIVE.equals(oldStatus) && !STATUS_TERMINATED.equals(newStatus)) {
            throw new BusinessException("已生效合同只能变更为已终止");
        }
        if (STATUS_TERMINATED.equals(oldStatus)) {
            throw new BusinessException("已终止合同不可再变更状态");
        }
    }

    private void syncCustomerStatusOnTerminate(BizContract contract) {
        BizCustomer customer = bizCustomerMapper.selectById(contract.getCustomerId());
        if (customer == null) return;

        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizContract::getCustomerId, customer.getId());
        wrapper.eq(BizContract::getContractStatus, STATUS_ACTIVE);
        wrapper.eq(BizContract::getTenantId, contract.getTenantId());

        long activeCount = bizContractMapper.selectCount(wrapper);
        if (activeCount == 0) {
            customer.setCooperationCategory("已合作");
            customer.setCooperationStatus("终止合作");
            bizCustomerMapper.updateById(customer);
            log.info("合同终止且无其他有效合同，同步客户状态为终止合作: customerId={}", customer.getId());
        }
    }

    @Override
    public List<BizContractAttachment> listAttachments(Long contractId) {
        BizContract contract = bizContractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        LambdaQueryWrapper<BizContractAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizContractAttachment::getContractId, contractId);
        wrapper.orderByDesc(BizContractAttachment::getCreateTime);

        return bizContractAttachmentMapper.selectList(wrapper);
    }

    @Override
    public void uploadAttachment(Long contractId, MultipartFile file, String fileType) {
        BizContract contract = bizContractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File dest = new File(uploadDir, storedFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        BizContractAttachment attachment = new BizContractAttachment();
        attachment.setContractId(contractId);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(storedFileName);
        attachment.setFileType(StringUtils.hasText(fileType) ? fileType : "其他");
        attachment.setFileSize(file.getSize());

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        attachment.setTenantId(tenantId != null ? tenantId : 0L);

        bizContractAttachmentMapper.insert(attachment);
        log.info("上传合同附件: contractId={}, fileName={}", contractId, originalFilename);
    }

    @Override
    public void deleteAttachment(Long attachmentId) {
        BizContractAttachment attachment = bizContractAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }

        File file = new File(uploadPath, attachment.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        bizContractAttachmentMapper.deleteById(attachmentId);
        log.info("删除合同附件: attachmentId={}", attachmentId);
    }

    @Override
    public void downloadAttachment(Long attachmentId, HttpServletResponse response) {
        BizContractAttachment attachment = bizContractAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }

        File file = new File(uploadPath, attachment.getFilePath());
        if (!file.exists()) {
            throw new BusinessException(404, "文件不存在");
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            String encodedFileName = URLEncoder.encode(attachment.getFileName(), "UTF-8").replaceAll("\\+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            response.setContentLengthLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            log.info("下载合同附件: attachmentId={}, fileName={}", attachmentId, attachment.getFileName());
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public List<BizContractModifyLog> listModifyLogs(Long contractId) {
        BizContract contract = bizContractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        LambdaQueryWrapper<BizContractModifyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizContractModifyLog::getContractId, contractId);
        wrapper.orderByDesc(BizContractModifyLog::getModifyTime);

        return bizContractModifyLogMapper.selectList(wrapper);
    }

    @Override
    public List<BizContractReminder> listReminders(Long contractId) {
        BizContract contract = bizContractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }

        LambdaQueryWrapper<BizContractReminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizContractReminder::getContractId, contractId);
        wrapper.orderByAsc(BizContractReminder::getRemindDate);

        return bizContractReminderMapper.selectList(wrapper);
    }

    @Override
    public List<BizContractReminder> getPendingReminders() {
        Long tenantId = UserContext.getTenantId();
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<BizContractReminder> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizContractReminder::getTenantId, tenantId);
        }
        wrapper.le(BizContractReminder::getRemindDate, today);
        wrapper.eq(BizContractReminder::getIsHandled, 0);
        wrapper.orderByAsc(BizContractReminder::getRemindDate);

        return bizContractReminderMapper.selectList(wrapper);
    }

    @Override
    public void markReminderRead(Long reminderId) {
        BizContractReminder reminder = bizContractReminderMapper.selectById(reminderId);
        if (reminder == null) {
            throw new BusinessException(404, "提醒不存在");
        }
        reminder.setIsRead(1);
        bizContractReminderMapper.updateById(reminder);
    }

    @Override
    public void markReminderHandled(Long reminderId) {
        BizContractReminder reminder = bizContractReminderMapper.selectById(reminderId);
        if (reminder == null) {
            throw new BusinessException(404, "提醒不存在");
        }
        reminder.setIsHandled(1);
        bizContractReminderMapper.updateById(reminder);
    }

    @Override
    public void generateReminders(Long contractId) {
        BizContract contract = bizContractMapper.selectById(contractId);
        if (contract == null || contract.getExpireDate() == null) return;

        int[] remindDaysArr = {30, 15, 7};

        for (int days : remindDaysArr) {
            LocalDate remindDate = contract.getExpireDate().minusDays(days);
            if (remindDate.isBefore(LocalDate.now())) continue;

            BizContractReminder reminder = new BizContractReminder();
            reminder.setContractId(contractId);
            reminder.setContractNo(contract.getContractNo());
            reminder.setCustomerName(contract.getCustomerName());
            reminder.setRemindDays(days);
            reminder.setRemindDate(remindDate);
            reminder.setPersonInChargeId(contract.getPersonInChargeId());
            reminder.setPersonInCharge(contract.getPersonInCharge());
            reminder.setIsRead(0);
            reminder.setIsHandled(0);
            reminder.setTenantId(contract.getTenantId());

            bizContractReminderMapper.insert(reminder);
        }

        log.info("生成合同到期提醒: contractId={}, expireDate={}", contractId, contract.getExpireDate());
    }

    @Override
    public void exportContracts(HttpServletResponse response, String contractNo, String customerName,
                                String signDateStart, String signDateEnd, String expireDateStart,
                                String expireDateEnd, String contractStatus) {
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(contractNo)) {
            wrapper.like(BizContract::getContractNo, contractNo);
        }
        if (StringUtils.hasText(customerName)) {
            wrapper.like(BizContract::getCustomerName, customerName);
        }
        if (StringUtils.hasText(signDateStart)) {
            wrapper.ge(BizContract::getSignDate, LocalDate.parse(signDateStart, DATE_FORMATTER));
        }
        if (StringUtils.hasText(signDateEnd)) {
            wrapper.le(BizContract::getSignDate, LocalDate.parse(signDateEnd, DATE_FORMATTER));
        }
        if (StringUtils.hasText(expireDateStart)) {
            wrapper.ge(BizContract::getExpireDate, LocalDate.parse(expireDateStart, DATE_FORMATTER));
        }
        if (StringUtils.hasText(expireDateEnd)) {
            wrapper.le(BizContract::getExpireDate, LocalDate.parse(expireDateEnd, DATE_FORMATTER));
        }
        if (StringUtils.hasText(contractStatus)) {
            wrapper.eq(BizContract::getContractStatus, contractStatus);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizContract::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(BizContract::getCreateTime);

        List<BizContract> contracts = bizContractMapper.selectList(wrapper);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("合同列表");

            String[] headers = {"合同编号", "客户名称", "签订日期", "到期日期",
                    "合同金额", "服务内容", "付款方式", "负责人", "合同状态", "备注", "创建时间"};
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
                sheet.setColumnWidth(i, 4000);
            }

            int rowIndex = 1;
            for (BizContract c : contracts) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(c.getContractNo() != null ? c.getContractNo() : "");
                row.createCell(1).setCellValue(c.getCustomerName() != null ? c.getCustomerName() : "");
                row.createCell(2).setCellValue(c.getSignDate() != null ? c.getSignDate().toString() : "");
                row.createCell(3).setCellValue(c.getExpireDate() != null ? c.getExpireDate().toString() : "");
                row.createCell(4).setCellValue(c.getContractAmount() != null ? c.getContractAmount().toString() : "");
                row.createCell(5).setCellValue(c.getServiceContent() != null ? c.getServiceContent() : "");
                row.createCell(6).setCellValue(c.getPaymentMethod() != null ? c.getPaymentMethod() : "");
                row.createCell(7).setCellValue(c.getPersonInCharge() != null ? c.getPersonInCharge() : "");
                row.createCell(8).setCellValue(c.getContractStatus() != null ? c.getContractStatus() : "");
                row.createCell(9).setCellValue(c.getRemark() != null ? c.getRemark() : "");
                row.createCell(10).setCellValue(c.getCreateTime() != null ? c.getCreateTime().toString() : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("合同列表.xlsx", "UTF-8"));

            workbook.write(response.getOutputStream());
            log.info("导出合同列表: 共{}条", contracts.size());

        } catch (IOException e) {
            log.error("合同导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private void copyRequestToEntity(ContractRequest request, BizContract contract) {
        if (StringUtils.hasText(request.getContractNo())) {
            contract.setContractNo(request.getContractNo());
        }
        if (StringUtils.hasText(request.getSignDate())) {
            contract.setSignDate(LocalDate.parse(request.getSignDate(), DATE_FORMATTER));
        }
        if (StringUtils.hasText(request.getExpireDate())) {
            contract.setExpireDate(LocalDate.parse(request.getExpireDate(), DATE_FORMATTER));
        }
        if (request.getContractAmount() != null) {
            contract.setContractAmount(request.getContractAmount());
        }
        if (StringUtils.hasText(request.getServiceContent())) {
            contract.setServiceContent(request.getServiceContent());
        }
        if (StringUtils.hasText(request.getPaymentMethod())) {
            contract.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getPersonInChargeId() != null) {
            contract.setPersonInChargeId(request.getPersonInChargeId());
        }
        if (StringUtils.hasText(request.getPersonInCharge())) {
            contract.setPersonInCharge(request.getPersonInCharge());
        }
        if (StringUtils.hasText(request.getRemark())) {
            contract.setRemark(request.getRemark());
        }
        if (StringUtils.hasText(request.getContractStatus())) {
            contract.setContractStatus(request.getContractStatus());
        }
    }

    private void recordModifyLogs(Long contractId, BizContract oldContract, ContractRequest newRequest) {
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();
        Long tenantId = oldContract.getTenantId();

        Map<String, String[]> fieldMap = new LinkedHashMap<>();
        fieldMap.put("contractNo", new String[]{"合同编号", oldContract.getContractNo(), newRequest.getContractNo()});
        fieldMap.put("contractStatus", new String[]{"合同状态", oldContract.getContractStatus(), newRequest.getContractStatus()});
        fieldMap.put("signDate", new String[]{"签订日期",
                oldContract.getSignDate() != null ? oldContract.getSignDate().toString() : "",
                newRequest.getSignDate()});
        fieldMap.put("expireDate", new String[]{"到期日期",
                oldContract.getExpireDate() != null ? oldContract.getExpireDate().toString() : "",
                newRequest.getExpireDate()});
        fieldMap.put("contractAmount", new String[]{"合同金额",
                oldContract.getContractAmount() != null ? oldContract.getContractAmount().toString() : "",
                newRequest.getContractAmount() != null ? newRequest.getContractAmount().toString() : ""});
        fieldMap.put("serviceContent", new String[]{"服务内容", oldContract.getServiceContent(), newRequest.getServiceContent()});
        fieldMap.put("paymentMethod", new String[]{"付款方式", oldContract.getPaymentMethod(), newRequest.getPaymentMethod()});
        fieldMap.put("personInCharge", new String[]{"负责人", oldContract.getPersonInCharge(), newRequest.getPersonInCharge()});
        fieldMap.put("remark", new String[]{"备注", oldContract.getRemark(), newRequest.getRemark()});

        for (Map.Entry<String, String[]> entry : fieldMap.entrySet()) {
            String fieldName = entry.getKey();
            String[] values = entry.getValue();
            String oldVal = values[1] != null ? values[1] : "";
            String newVal = values[2] != null ? values[2] : "";

            if (!oldVal.equals(newVal)) {
                BizContractModifyLog modifyLog = new BizContractModifyLog();
                modifyLog.setContractId(contractId);
                modifyLog.setFieldName(fieldName);
                modifyLog.setOldValue(oldVal);
                modifyLog.setNewValue(newVal);
                modifyLog.setModifyUserId(userId);
                modifyLog.setModifyUser(username);
                modifyLog.setModifyTime(LocalDateTime.now());
                modifyLog.setTenantId(tenantId);
                bizContractModifyLogMapper.insert(modifyLog);
            }
        }
    }
}

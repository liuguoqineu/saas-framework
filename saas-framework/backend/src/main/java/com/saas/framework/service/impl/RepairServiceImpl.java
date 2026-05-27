package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.*;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.*;
import com.saas.framework.service.RepairService;
import com.saas.framework.config.FilePathConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class RepairServiceImpl implements RepairService {

    @Resource
    private BizRepairOrderMapper repairOrderMapper;

    @Resource
    private BizRepairAttachmentMapper repairAttachmentMapper;

    @Resource
    private BizRepairProcessLogMapper repairProcessLogMapper;

    @Resource
    private BizCustomerMapper customerMapper;

    @Resource
    private FilePathConfig filePathConfig;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String STATUS_PENDING = "未处理";
    private static final String STATUS_PROCESSING = "处理中";
    private static final String STATUS_RESOLVED = "已解决";
    private static final String STATUS_UNRESOLVED = "无法解决";

    @Override
    public IPage<BizRepairOrder> page(int page, int size, String customerName, String repairTimeStart,
                                       String repairTimeEnd, String status, String assigneeName,
                                       String urgency, String repairType) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(customerName)) {
            wrapper.like(BizRepairOrder::getCustomerName, customerName);
        }
        if (StringUtils.hasText(repairTimeStart)) {
            wrapper.ge(BizRepairOrder::getRepairTime, LocalDateTime.parse(repairTimeStart + " 00:00:00", DATETIME_FORMATTER));
        }
        if (StringUtils.hasText(repairTimeEnd)) {
            wrapper.le(BizRepairOrder::getRepairTime, LocalDateTime.parse(repairTimeEnd + " 23:59:59", DATETIME_FORMATTER));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizRepairOrder::getStatus, status);
        }
        if (StringUtils.hasText(assigneeName)) {
            wrapper.like(BizRepairOrder::getAssigneeName, assigneeName);
        }
        if (StringUtils.hasText(urgency)) {
            wrapper.eq(BizRepairOrder::getUrgency, urgency);
        }
        if (StringUtils.hasText(repairType)) {
            wrapper.eq(BizRepairOrder::getRepairType, repairType);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizRepairOrder::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(BizRepairOrder::getCreateTime);

        return repairOrderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizRepairOrder create(RepairOrderRequest request) {
        if (!StringUtils.hasText(request.getRepairContent())) {
            throw new BusinessException("报修内容不能为空");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        Long finalTenantId = tenantId != null ? tenantId : 0L;

        if (request.getCustomerId() != null) {
            BizCustomer customer = customerMapper.selectById(request.getCustomerId());
            if (customer == null) {
                throw new BusinessException(404, "关联客户不存在");
            }
            if (!StringUtils.hasText(request.getCustomerName())) {
                request.setCustomerName(customer.getName());
            }
            if (!StringUtils.hasText(request.getContactPerson())) {
                request.setContactPerson(customer.getContactPerson());
            }
            if (!StringUtils.hasText(request.getContactPhone())) {
                request.setContactPhone(customer.getContactPhone());
            }
        }

        BizRepairOrder order = new BizRepairOrder();
        order.setRepairNo(generateRepairNo());
        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(request.getCustomerName());
        order.setContactPerson(request.getContactPerson());
        order.setContactPhone(request.getContactPhone());
        order.setRepairContent(request.getRepairContent());
        order.setRepairType(request.getRepairType());
        order.setRepairTime(request.getRepairTime() != null ? request.getRepairTime() : LocalDateTime.now());
        order.setRepairAddress(request.getRepairAddress());
        order.setUrgency(StringUtils.hasText(request.getUrgency()) ? request.getUrgency() : "普通");
        order.setStatus(STATUS_PENDING);
        order.setFaultDescription(request.getFaultDescription());
        order.setConfirmStatus(0);
        order.setIsException(0);
        order.setCreatorId(UserContext.getUserId());
        order.setCreatorName(UserContext.getUsername());
        order.setTenantId(finalTenantId);

        repairOrderMapper.insert(order);

        addProcessLog(order.getId(), "录入", null, STATUS_PENDING, "录入报修单", order.getTenantId());

        log.info("新增报修单: id={}, repairNo={}, customerName={}", order.getId(), order.getRepairNo(), order.getCustomerName());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RepairOrderRequest request) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的报修数据");
        }

        if (StringUtils.hasText(request.getCustomerName())) {
            order.setCustomerName(request.getCustomerName());
        }
        if (StringUtils.hasText(request.getContactPerson())) {
            order.setContactPerson(request.getContactPerson());
        }
        if (StringUtils.hasText(request.getContactPhone())) {
            order.setContactPhone(request.getContactPhone());
        }
        if (StringUtils.hasText(request.getRepairContent())) {
            order.setRepairContent(request.getRepairContent());
        }
        if (StringUtils.hasText(request.getRepairType())) {
            order.setRepairType(request.getRepairType());
        }
        if (request.getRepairTime() != null) {
            order.setRepairTime(request.getRepairTime());
        }
        if (StringUtils.hasText(request.getRepairAddress())) {
            order.setRepairAddress(request.getRepairAddress());
        }
        if (StringUtils.hasText(request.getUrgency())) {
            order.setUrgency(request.getUrgency());
        }
        if (StringUtils.hasText(request.getFaultDescription())) {
            order.setFaultDescription(request.getFaultDescription());
        }
        if (request.getCustomerId() != null) {
            order.setCustomerId(request.getCustomerId());
        }

        repairOrderMapper.updateById(order);

        addProcessLog(order.getId(), "补充", order.getStatus(), order.getStatus(), "补充报修信息", order.getTenantId());

        log.info("修改报修单: id={}, repairNo={}", id, order.getRepairNo());
    }

    @Override
    public BizRepairOrder detail(Long id) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的报修数据");
        }

        return order;
    }

    @Override
    public void delete(Long id) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权删除其他租户的报修数据");
        }

        repairOrderMapper.deleteById(id);
        log.info("删除报修单: id={}, repairNo={}", id, order.getRepairNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, RepairAssignRequest request) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_UNRESOLVED.equals(order.getStatus())) {
            throw new BusinessException("当前状态的报修单不可分配");
        }

        String oldStatus = order.getStatus();
        order.setAssigneeId(request.getAssigneeId());
        order.setAssigneeName(request.getAssigneeName());
        order.setAssignerId(UserContext.getUserId());
        order.setAssignerName(UserContext.getUsername());
        order.setAssignTime(LocalDateTime.now());
        order.setStatus(STATUS_PROCESSING);

        repairOrderMapper.updateById(order);

        addProcessLog(order.getId(), "分配", oldStatus, STATUS_PROCESSING,
                "分配给运维人员: " + request.getAssigneeName(), order.getTenantId());

        log.info("分配报修单: id={}, assignee={}", id, request.getAssigneeName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(Long id, RepairProcessRequest request) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        if (!STATUS_PROCESSING.equals(order.getStatus())) {
            throw new BusinessException("只有处理中的报修单可以更新进度");
        }

        String oldStatus = order.getStatus();
        String newStatus = request.getStatus();
        if (!StringUtils.hasText(newStatus)) {
            throw new BusinessException("请指定报修状态");
        }

        if (!STATUS_PROCESSING.equals(newStatus) && !STATUS_RESOLVED.equals(newStatus) && !STATUS_UNRESOLVED.equals(newStatus)) {
            throw new BusinessException("无效的报修状态: " + newStatus);
        }

        order.setStatus(newStatus);
        order.setProcessTime(LocalDateTime.now());
        if (StringUtils.hasText(request.getProcessMethod())) {
            order.setProcessMethod(request.getProcessMethod());
        }
        if (StringUtils.hasText(request.getReplacedParts())) {
            order.setReplacedParts(request.getReplacedParts());
        }
        if (StringUtils.hasText(request.getFaultReason())) {
            order.setFaultReason(request.getFaultReason());
        }

        if (STATUS_RESOLVED.equals(newStatus)) {
            order.setConfirmStatus(0);
        }

        if (STATUS_UNRESOLVED.equals(newStatus)) {
            order.setIsException(1);
        }

        repairOrderMapper.updateById(order);

        String content = "更新报修进度为: " + newStatus;
        if (StringUtils.hasText(request.getProcessMethod())) {
            content += "，处理方式: " + request.getProcessMethod();
        }
        if (StringUtils.hasText(request.getFaultReason())) {
            content += "，故障原因: " + request.getFaultReason();
        }

        addProcessLog(order.getId(), "进度更新", oldStatus, newStatus, content, order.getTenantId());

        log.info("更新报修进度: id={}, status={}", id, newStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        if (!STATUS_RESOLVED.equals(order.getStatus())) {
            throw new BusinessException("只有已解决的报修单可以确认");
        }

        if (order.getConfirmStatus() == 1) {
            throw new BusinessException("报修单已确认，无需重复确认");
        }

        order.setConfirmStatus(1);
        order.setConfirmTime(LocalDateTime.now());
        order.setConfirmPerson(UserContext.getUsername());

        repairOrderMapper.updateById(order);

        addProcessLog(order.getId(), "确认", order.getStatus(), order.getStatus(),
                "报修确认闭环，确认人: " + UserContext.getUsername(), order.getTenantId());

        log.info("确认报修单: id={}, repairNo={}", id, order.getRepairNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markException(Long id, RepairExceptionRequest request) {
        BizRepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        order.setIsException(1);
        if (StringUtils.hasText(request.getExceptionReason())) {
            order.setExceptionReason(request.getExceptionReason());
        }
        if (StringUtils.hasText(request.getSecondPlan())) {
            order.setSecondPlan(request.getSecondPlan());
        }
        if (request.getSecondRemindTime() != null) {
            order.setSecondRemindTime(request.getSecondRemindTime());
        }

        repairOrderMapper.updateById(order);

        String content = "标记异常，原因: " + request.getExceptionReason();
        if (StringUtils.hasText(request.getSecondPlan())) {
            content += "，二次处理计划: " + request.getSecondPlan();
        }

        addProcessLog(order.getId(), "异常标记", order.getStatus(), order.getStatus(), content, order.getTenantId());

        log.info("标记报修单异常: id={}, repairNo={}", id, order.getRepairNo());
    }

    @Override
    public List<BizRepairAttachment> listAttachments(Long repairId) {
        BizRepairOrder order = repairOrderMapper.selectById(repairId);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        LambdaQueryWrapper<BizRepairAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizRepairAttachment::getRepairId, repairId);
        wrapper.orderByDesc(BizRepairAttachment::getCreateTime);

        return repairAttachmentMapper.selectList(wrapper);
    }

    @Override
    public void uploadAttachment(Long repairId, MultipartFile file, String fileType) {
        BizRepairOrder order = repairOrderMapper.selectById(repairId);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
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

        File uploadDir = new File(filePathConfig.getUploadPath());
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

        BizRepairAttachment attachment = new BizRepairAttachment();
        attachment.setRepairId(repairId);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(storedFileName);
        attachment.setFileType(StringUtils.hasText(fileType) ? fileType : "其他");
        attachment.setFileSize(file.getSize());

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        attachment.setTenantId(tenantId != null ? tenantId : 0L);

        repairAttachmentMapper.insert(attachment);
        log.info("上传报修附件: repairId={}, fileName={}", repairId, originalFilename);
    }

    @Override
    public void deleteAttachment(Long attachmentId) {
        BizRepairAttachment attachment = repairAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }

        File file = new File(filePathConfig.getUploadPath(), attachment.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        repairAttachmentMapper.deleteById(attachmentId);
        log.info("删除报修附件: attachmentId={}", attachmentId);
    }

    @Override
    public void downloadAttachment(Long attachmentId, HttpServletResponse response) {
        BizRepairAttachment attachment = repairAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }

        File file = new File(filePathConfig.getUploadPath(), attachment.getFilePath());
        if (!file.exists()) {
            throw new BusinessException(404, "文件不存在");
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            String fileName = attachment.getFileName();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setContentLengthLong(file.length());

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            log.info("下载报修附件: attachmentId={}, fileName={}", attachmentId, fileName);
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public List<BizRepairProcessLog> listProcessLogs(Long repairId) {
        BizRepairOrder order = repairOrderMapper.selectById(repairId);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }

        LambdaQueryWrapper<BizRepairProcessLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizRepairProcessLog::getRepairId, repairId);
        wrapper.orderByAsc(BizRepairProcessLog::getOperateTime);

        return repairProcessLogMapper.selectList(wrapper);
    }

    @Override
    public RepairStatsVO stats() {
        Long tenantId = UserContext.getTenantId();

        LambdaQueryWrapper<BizRepairOrder> baseWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            baseWrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }

        long totalCount = repairOrderMapper.selectCount(baseWrapper);

        LambdaQueryWrapper<BizRepairOrder> resolvedWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            resolvedWrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }
        resolvedWrapper.eq(BizRepairOrder::getStatus, STATUS_RESOLVED);
        long resolvedCount = repairOrderMapper.selectCount(resolvedWrapper);

        LambdaQueryWrapper<BizRepairOrder> pendingWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            pendingWrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }
        pendingWrapper.eq(BizRepairOrder::getStatus, STATUS_PENDING);
        long unresolvedCount = repairOrderMapper.selectCount(pendingWrapper);

        LambdaQueryWrapper<BizRepairOrder> processingWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            processingWrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }
        processingWrapper.eq(BizRepairOrder::getStatus, STATUS_PROCESSING);
        long processingCount = repairOrderMapper.selectCount(processingWrapper);

        LambdaQueryWrapper<BizRepairOrder> exceptionWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            exceptionWrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }
        exceptionWrapper.eq(BizRepairOrder::getIsException, 1);
        long exceptionCount = repairOrderMapper.selectCount(exceptionWrapper);

        List<Map<String, Object>> highFreqCustomers = new ArrayList<>();
        LambdaQueryWrapper<BizRepairOrder> customerWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            customerWrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }
        customerWrapper.isNotNull(BizRepairOrder::getCustomerName);
        List<BizRepairOrder> allOrders = repairOrderMapper.selectList(customerWrapper);
        Map<String, Long> customerCountMap = new LinkedHashMap<>();
        for (BizRepairOrder o : allOrders) {
            String name = o.getCustomerName();
            if (name != null) {
                customerCountMap.merge(name, 1L, Long::sum);
            }
        }
        customerCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    highFreqCustomers.add(item);
                });

        List<Map<String, Object>> highFreqFaultTypes = new ArrayList<>();
        Map<String, Long> faultTypeCountMap = new LinkedHashMap<>();
        for (BizRepairOrder o : allOrders) {
            String type = o.getRepairType();
            if (type != null) {
                faultTypeCountMap.merge(type, 1L, Long::sum);
            }
        }
        faultTypeCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    highFreqFaultTypes.add(item);
                });

        RepairStatsVO vo = new RepairStatsVO();
        vo.setTotalCount(totalCount);
        vo.setResolvedCount(resolvedCount);
        vo.setUnresolvedCount(unresolvedCount);
        vo.setProcessingCount(processingCount);
        vo.setExceptionCount(exceptionCount);
        vo.setHighFrequencyCustomers(highFreqCustomers);
        vo.setHighFrequencyFaultTypes(highFreqFaultTypes);

        return vo;
    }

    @Override
    public void exportRepairOrders(HttpServletResponse response, String customerName, String repairTimeStart,
                                    String repairTimeEnd, String status, String assigneeName,
                                    String urgency, String repairType) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(customerName)) {
            wrapper.like(BizRepairOrder::getCustomerName, customerName);
        }
        if (StringUtils.hasText(repairTimeStart)) {
            wrapper.ge(BizRepairOrder::getRepairTime, LocalDateTime.parse(repairTimeStart + " 00:00:00", DATETIME_FORMATTER));
        }
        if (StringUtils.hasText(repairTimeEnd)) {
            wrapper.le(BizRepairOrder::getRepairTime, LocalDateTime.parse(repairTimeEnd + " 23:59:59", DATETIME_FORMATTER));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizRepairOrder::getStatus, status);
        }
        if (StringUtils.hasText(assigneeName)) {
            wrapper.like(BizRepairOrder::getAssigneeName, assigneeName);
        }
        if (StringUtils.hasText(urgency)) {
            wrapper.eq(BizRepairOrder::getUrgency, urgency);
        }
        if (StringUtils.hasText(repairType)) {
            wrapper.eq(BizRepairOrder::getRepairType, repairType);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizRepairOrder::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(BizRepairOrder::getCreateTime);

        List<BizRepairOrder> orders = repairOrderMapper.selectList(wrapper);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("报修列表");

            String[] headers = {"报修单号", "客户名称", "联系人", "联系电话", "报修类型", "报修内容",
                    "报修时间", "报修地点", "紧急程度", "报修状态", "运维人员", "处理方式",
                    "故障原因", "确认状态", "是否异常", "录入人", "创建时间"};
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
            for (BizRepairOrder o : orders) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(o.getRepairNo() != null ? o.getRepairNo() : "");
                row.createCell(1).setCellValue(o.getCustomerName() != null ? o.getCustomerName() : "");
                row.createCell(2).setCellValue(o.getContactPerson() != null ? o.getContactPerson() : "");
                row.createCell(3).setCellValue(o.getContactPhone() != null ? o.getContactPhone() : "");
                row.createCell(4).setCellValue(o.getRepairType() != null ? o.getRepairType() : "");
                row.createCell(5).setCellValue(o.getRepairContent() != null ? o.getRepairContent() : "");
                row.createCell(6).setCellValue(o.getRepairTime() != null ? o.getRepairTime().format(DATETIME_FORMATTER) : "");
                row.createCell(7).setCellValue(o.getRepairAddress() != null ? o.getRepairAddress() : "");
                row.createCell(8).setCellValue(o.getUrgency() != null ? o.getUrgency() : "");
                row.createCell(9).setCellValue(o.getStatus() != null ? o.getStatus() : "");
                row.createCell(10).setCellValue(o.getAssigneeName() != null ? o.getAssigneeName() : "");
                row.createCell(11).setCellValue(o.getProcessMethod() != null ? o.getProcessMethod() : "");
                row.createCell(12).setCellValue(o.getFaultReason() != null ? o.getFaultReason() : "");
                row.createCell(13).setCellValue(o.getConfirmStatus() != null && o.getConfirmStatus() == 1 ? "已确认" : "未确认");
                row.createCell(14).setCellValue(o.getIsException() != null && o.getIsException() == 1 ? "异常" : "正常");
                row.createCell(15).setCellValue(o.getCreatorName() != null ? o.getCreatorName() : "");
                row.createCell(16).setCellValue(o.getCreateTime() != null ? o.getCreateTime().format(DATETIME_FORMATTER) : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("报修列表.xlsx", "UTF-8"));

            workbook.write(response.getOutputStream());
            log.info("导出报修列表: 共{}条", orders.size());

        } catch (IOException e) {
            log.error("报修导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public List<BizRepairOrder> getUnconfirmedReminders() {
        Long tenantId = UserContext.getTenantId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(24);

        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizRepairOrder::getTenantId, tenantId);
        }
        wrapper.eq(BizRepairOrder::getStatus, STATUS_RESOLVED);
        wrapper.eq(BizRepairOrder::getConfirmStatus, 0);
        wrapper.le(BizRepairOrder::getProcessTime, threshold);
        wrapper.orderByAsc(BizRepairOrder::getProcessTime);

        return repairOrderMapper.selectList(wrapper);
    }

    private String generateRepairNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", new Random().nextInt(1000000));
        String repairNo = "WX" + datePart + randomPart;

        int maxRetry = 3;
        int retryCount = 0;

        while (retryCount < maxRetry) {
            LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BizRepairOrder::getRepairNo, repairNo);
            Long count = repairOrderMapper.selectCount(wrapper);

            if (count == 0) {
                return repairNo;
            }

            retryCount++;
            log.warn("报修单号 {} 已存在，第{}次重新生成", repairNo, retryCount);
            randomPart = String.format("%06d", new Random().nextInt(1000000));
            repairNo = "WX" + datePart + randomPart;
        }

        throw new BusinessException("生成报修单号失败，请稍后重试");
    }

    private void addProcessLog(Long repairId, String action, String oldStatus, String newStatus,
                                String content, Long tenantId) {
        BizRepairProcessLog processLog = new BizRepairProcessLog();
        processLog.setRepairId(repairId);
        processLog.setAction(action);
        processLog.setOldStatus(oldStatus);
        processLog.setNewStatus(newStatus);
        processLog.setContent(content);
        processLog.setOperatorId(UserContext.getUserId());
        processLog.setOperatorName(UserContext.getUsername());
        processLog.setOperateTime(LocalDateTime.now());
        processLog.setTenantId(tenantId != null ? tenantId : 0L);

        repairProcessLogMapper.insert(processLog);
    }
}

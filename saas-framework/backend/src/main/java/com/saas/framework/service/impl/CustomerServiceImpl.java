package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.CustomerRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.BizCustomer;
import com.saas.framework.entity.BizCustomerAttachment;
import com.saas.framework.entity.BizCustomerModifyLog;
import com.saas.framework.entity.BizContract;
import com.saas.framework.mapper.BizCustomerAttachmentMapper;
import com.saas.framework.mapper.BizCustomerMapper;
import com.saas.framework.mapper.BizCustomerModifyLogMapper;
import com.saas.framework.mapper.BizContractMapper;
import com.saas.framework.service.CustomerService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 客户服务实现
 * 包含客户CRUD、附件管理、修改日志、Excel导入导出
 * 多租户隔离：新增时自动填充 tenant_id，查询时手动控制租户过滤
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private BizCustomerMapper bizCustomerMapper;

    @Resource
    private BizCustomerAttachmentMapper bizCustomerAttachmentMapper;

    @Resource
    private BizCustomerModifyLogMapper bizCustomerModifyLogMapper;

    @Resource
    private BizContractMapper bizContractMapper;

    @Resource
    private FilePathConfig filePathConfig;

    @Override
    public IPage<BizCustomer> page(int page, int size, String name, String businessCategory,
                                    String businessType, String cooperationCategory,
                                    String cooperationStatus, String region, String contactPerson,
                                    String maintenanceCategory) {
        LambdaQueryWrapper<BizCustomer> wrapper = new LambdaQueryWrapper<>();

        // 无效客户不出现在分页查询中（包含 isInvalid=0 或 IS NULL 的情况）
        wrapper.and(w -> w.eq(BizCustomer::getIsInvalid, 0).or().isNull(BizCustomer::getIsInvalid));

        if (StringUtils.hasText(name)) {
            wrapper.like(BizCustomer::getName, name);
        }
        if (StringUtils.hasText(businessCategory)) {
            wrapper.eq(BizCustomer::getBusinessCategory, businessCategory);
        }
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(BizCustomer::getBusinessType, businessType);
        }
        if (StringUtils.hasText(cooperationCategory)) {
            wrapper.eq(BizCustomer::getCooperationCategory, cooperationCategory);
        }
        if (StringUtils.hasText(cooperationStatus)) {
            wrapper.eq(BizCustomer::getCooperationStatus, cooperationStatus);
        }
        if (StringUtils.hasText(region)) {
            wrapper.eq(BizCustomer::getRegion, region);
        }
        if (StringUtils.hasText(contactPerson)) {
            wrapper.like(BizCustomer::getContactPerson, contactPerson);
        }
        if (StringUtils.hasText(maintenanceCategory)) {
            wrapper.eq(BizCustomer::getMaintenanceCategory, maintenanceCategory);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizCustomer::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(BizCustomer::getCreateTime);

        IPage<BizCustomer> resultPage = bizCustomerMapper.selectPage(new Page<>(page, size), wrapper);

        for (BizCustomer customer : resultPage.getRecords()) {
            customer.setContractExpireDate(getContractExpireDate(customer.getId()));
        }

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CustomerRequest request) {
        BizCustomer customer = new BizCustomer();
        copyRequestToEntity(request, customer);

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        customer.setTenantId(tenantId != null ? tenantId : 0L);
        customer.setIsInvalid(0);
        if (!StringUtils.hasText(request.getCooperationCategory())) {
            customer.setCooperationCategory("潜在");
        }
        if (!StringUtils.hasText(request.getCooperationStatus())) {
            customer.setCooperationStatus("中潜力");
        }

        bizCustomerMapper.insert(customer);
        log.info("新增客户: id={}, name={}, tenantId={}", customer.getId(), customer.getName(), customer.getTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CustomerRequest request) {
        BizCustomer customer = bizCustomerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(customer.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的客户数据");
        }

        recordModifyLogs(id, customer, request);

        copyRequestToEntity(request, customer);
        bizCustomerMapper.updateById(customer);

        log.info("修改客户: id={}, name={}", id, request.getName());
    }

    @Override
    public BizCustomer detail(Long id) {
        BizCustomer customer = bizCustomerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(customer.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的客户数据");
        }

        customer.setContractExpireDate(getContractExpireDate(id));
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markInvalid(Long id) {
        BizCustomer customer = bizCustomerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(customer.getTenantId())) {
            throw new BusinessException(403, "无权操作其他租户的客户数据");
        }

        if (customer.getIsInvalid() == 1) {
            throw new BusinessException("该客户已被标记为无效");
        }

        customer.setIsInvalid(1);
        bizCustomerMapper.updateById(customer);

        BizCustomerModifyLog modifyLog = new BizCustomerModifyLog();
        modifyLog.setCustomerId(id);
        modifyLog.setFieldName("isInvalid");
        modifyLog.setOldValue("0");
        modifyLog.setNewValue("1");
        modifyLog.setModifyUserId(UserContext.getUserId());
        modifyLog.setModifyUser(UserContext.getUsername());
        modifyLog.setModifyTime(LocalDateTime.now());
        modifyLog.setTenantId(customer.getTenantId());
        bizCustomerModifyLogMapper.insert(modifyLog);

        log.info("标记客户无效: id={}, name={}", id, customer.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BizCustomer customer = bizCustomerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(customer.getTenantId())) {
            throw new BusinessException(403, "无权删除其他租户的客户数据");
        }

        if (customer.getIsInvalid() != null && customer.getIsInvalid() == 1) {
            throw new BusinessException("该客户已被标记为无效");
        }

        customer.setIsInvalid(1);
        bizCustomerMapper.updateById(customer);

        BizCustomerModifyLog modifyLog = new BizCustomerModifyLog();
        modifyLog.setCustomerId(id);
        modifyLog.setFieldName("isInvalid");
        modifyLog.setOldValue("0");
        modifyLog.setNewValue("1");
        modifyLog.setModifyUserId(UserContext.getUserId());
        modifyLog.setModifyUser(UserContext.getUsername());
        modifyLog.setModifyTime(LocalDateTime.now());
        modifyLog.setTenantId(customer.getTenantId());
        bizCustomerModifyLogMapper.insert(modifyLog);

        log.info("软删除客户（标记为无效）: id={}, name={}", id, customer.getName());
    }

    @Override
    public List<BizCustomerAttachment> listAttachments(Long customerId) {
        BizCustomer customer = bizCustomerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        LambdaQueryWrapper<BizCustomerAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizCustomerAttachment::getCustomerId, customerId);
        wrapper.orderByDesc(BizCustomerAttachment::getCreateTime);

        return bizCustomerAttachmentMapper.selectList(wrapper);
    }

    @Override
    public void uploadAttachment(Long customerId, MultipartFile file, String fileType) {
        BizCustomer customer = bizCustomerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
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

        BizCustomerAttachment attachment = new BizCustomerAttachment();
        attachment.setCustomerId(customerId);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(storedFileName);
        attachment.setFileType(StringUtils.hasText(fileType) ? fileType : "其他");
        attachment.setFileSize(file.getSize());

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        attachment.setTenantId(tenantId != null ? tenantId : 0L);

        bizCustomerAttachmentMapper.insert(attachment);
        log.info("上传客户附件: customerId={}, fileName={}", customerId, originalFilename);
    }

    @Override
    public void deleteAttachment(Long attachmentId) {
        BizCustomerAttachment attachment = bizCustomerAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }

        File file = new File(filePathConfig.getUploadPath(), attachment.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        bizCustomerAttachmentMapper.deleteById(attachmentId);
        log.info("删除客户附件: attachmentId={}", attachmentId);
    }

    @Override
    public void downloadAttachment(Long attachmentId, HttpServletResponse response) {
        BizCustomerAttachment attachment = bizCustomerAttachmentMapper.selectById(attachmentId);
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

            log.info("下载客户附件: attachmentId={}, fileName={}", attachmentId, fileName);
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public List<BizCustomerModifyLog> listModifyLogs(Long customerId) {
        BizCustomer customer = bizCustomerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        LambdaQueryWrapper<BizCustomerModifyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizCustomerModifyLog::getCustomerId, customerId);
        wrapper.orderByDesc(BizCustomerModifyLog::getModifyTime);

        return bizCustomerModifyLogMapper.selectList(wrapper);
    }

    @Override
    public void importCustomers(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("导入文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            throw new BusinessException("仅支持 Excel 文件（.xlsx 或 .xls）");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        Long finalTenantId = tenantId != null ? tenantId : 0L;

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int successCount = 0;
            int errorCount = 0;
            StringBuilder errorMsg = new StringBuilder();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String name = getCellStringValue(row.getCell(0));
                    if (!StringUtils.hasText(name)) {
                        errorCount++;
                        errorMsg.append("第").append(i + 1).append("行: 客户名称不能为空; ");
                        continue;
                    }

                    BizCustomer customer = new BizCustomer();
                    customer.setName(name);
                    customer.setBusinessCategory(getCellStringValue(row.getCell(1)));
                    customer.setBusinessType(getCellStringValue(row.getCell(2)));
                    customer.setCooperationCategory(getCellStringValue(row.getCell(3)));
                    customer.setCooperationStatus(getCellStringValue(row.getCell(4)));
                    customer.setAddress(getCellStringValue(row.getCell(5)));
                    customer.setRegion(getCellStringValue(row.getCell(6)));
                    customer.setContactPerson(getCellStringValue(row.getCell(7)));
                    customer.setContactPhone(getCellStringValue(row.getCell(8)));
                    customer.setGasScale(getCellStringValue(row.getCell(9)));
                    customer.setSmartGasSystem(getCellStringValue(row.getCell(10)));
                    customer.setContractInfo(getCellStringValue(row.getCell(11)));
                    customer.setIsInvalid(0);
                    customer.setTenantId(finalTenantId);

                    if (!StringUtils.hasText(customer.getCooperationCategory())) {
                        customer.setCooperationCategory("潜在");
                    }
                    if (!StringUtils.hasText(customer.getCooperationStatus())) {
                        customer.setCooperationStatus("中潜力");
                    }

                    bizCustomerMapper.insert(customer);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    errorMsg.append("第").append(i + 1).append("行: ").append(e.getMessage()).append("; ");
                }
            }

            log.info("客户导入完成: 成功{}条, 失败{}条", successCount, errorCount);

            if (errorCount > 0) {
                throw new BusinessException("导入完成: 成功" + successCount + "条, 失败" + errorCount + "条。错误详情: " + errorMsg);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("客户导入失败", e);
            throw new BusinessException("导入失败: " + e.getMessage());
        }
    }

    @Override
    public void exportCustomers(HttpServletResponse response, String name, String businessCategory,
                                 String businessType, String cooperationCategory,
                                 String cooperationStatus, String region) {
        LambdaQueryWrapper<BizCustomer> wrapper = new LambdaQueryWrapper<>();

        // 导出也过滤掉无效客户
        wrapper.eq(BizCustomer::getIsInvalid, 0);

        if (StringUtils.hasText(name)) {
            wrapper.like(BizCustomer::getName, name);
        }
        if (StringUtils.hasText(businessCategory)) {
            wrapper.eq(BizCustomer::getBusinessCategory, businessCategory);
        }
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(BizCustomer::getBusinessType, businessType);
        }
        if (StringUtils.hasText(cooperationCategory)) {
            wrapper.eq(BizCustomer::getCooperationCategory, cooperationCategory);
        }
        if (StringUtils.hasText(cooperationStatus)) {
            wrapper.eq(BizCustomer::getCooperationStatus, cooperationStatus);
        }
        if (StringUtils.hasText(region)) {
            wrapper.eq(BizCustomer::getRegion, region);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizCustomer::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(BizCustomer::getCreateTime);

        List<BizCustomer> customers = bizCustomerMapper.selectList(wrapper);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户列表");

            String[] headers = {"客户名称", "业务一级分类", "业务二级分类", "合作一级分类", "合作二级分类",
                    "地址", "区域", "联系人", "联系电话", "用气规模", "智慧燃气系统", "合同信息", "创建时间"};
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
            for (BizCustomer c : customers) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(c.getName() != null ? c.getName() : "");
                row.createCell(1).setCellValue(c.getBusinessCategory() != null ? c.getBusinessCategory() : "");
                row.createCell(2).setCellValue(c.getBusinessType() != null ? c.getBusinessType() : "");
                row.createCell(3).setCellValue(c.getCooperationCategory() != null ? c.getCooperationCategory() : "");
                row.createCell(4).setCellValue(c.getCooperationStatus() != null ? c.getCooperationStatus() : "");
                row.createCell(5).setCellValue(c.getAddress() != null ? c.getAddress() : "");
                row.createCell(6).setCellValue(c.getRegion() != null ? c.getRegion() : "");
                row.createCell(7).setCellValue(c.getContactPerson() != null ? c.getContactPerson() : "");
                row.createCell(8).setCellValue(c.getContactPhone() != null ? c.getContactPhone() : "");
                row.createCell(9).setCellValue(c.getGasScale() != null ? c.getGasScale() : "");
                row.createCell(10).setCellValue(c.getSmartGasSystem() != null ? c.getSmartGasSystem() : "");
                row.createCell(11).setCellValue(c.getContractInfo() != null ? c.getContractInfo() : "");
                row.createCell(12).setCellValue(c.getCreateTime() != null ? c.getCreateTime().toString() : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("客户列表.xlsx", "UTF-8"));

            workbook.write(response.getOutputStream());
            log.info("导出客户列表: 共{}条", customers.size());

        } catch (IOException e) {
            log.error("客户导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private void copyRequestToEntity(CustomerRequest request, BizCustomer customer) {
        if (request.getName() != null) customer.setName(request.getName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getDetailAddress() != null) customer.setDetailAddress(request.getDetailAddress());
        if (request.getRegion() != null) customer.setRegion(request.getRegion());
        if (request.getContactPerson() != null) customer.setContactPerson(request.getContactPerson());
        if (request.getContactPhone() != null) customer.setContactPhone(request.getContactPhone());
        if (request.getBusinessCategory() != null) customer.setBusinessCategory(request.getBusinessCategory());
        if (request.getBusinessType() != null) customer.setBusinessType(request.getBusinessType());
        if (request.getCooperationCategory() != null) customer.setCooperationCategory(request.getCooperationCategory());
        if (request.getCooperationStatus() != null) customer.setCooperationStatus(request.getCooperationStatus());
        if (request.getGasScale() != null) customer.setGasScale(request.getGasScale());
        if (request.getSmartGasSystem() != null) customer.setSmartGasSystem(request.getSmartGasSystem());
        if (request.getContractInfo() != null) customer.setContractInfo(request.getContractInfo());
        if (request.getFollowUpPersonId() != null) customer.setFollowUpPersonId(request.getFollowUpPersonId());
        if (request.getFollowUpPerson() != null) customer.setFollowUpPerson(request.getFollowUpPerson());
        if (request.getMaintenanceCategory() != null) customer.setMaintenanceCategory(request.getMaintenanceCategory());
    }

    private void recordModifyLogs(Long customerId, BizCustomer oldCustomer, CustomerRequest newRequest) {
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();
        Long tenantId = oldCustomer.getTenantId();

        Map<String, String[]> fieldMap = new LinkedHashMap<>();
        fieldMap.put("name", new String[]{"客户名称", oldCustomer.getName(), newRequest.getName()});
        fieldMap.put("address", new String[]{"地址", oldCustomer.getAddress(), newRequest.getAddress()});
        fieldMap.put("region", new String[]{"区域", oldCustomer.getRegion(), newRequest.getRegion()});
        fieldMap.put("contactPerson", new String[]{"联系人", oldCustomer.getContactPerson(), newRequest.getContactPerson()});
        fieldMap.put("contactPhone", new String[]{"联系电话", oldCustomer.getContactPhone(), newRequest.getContactPhone()});
        fieldMap.put("businessCategory", new String[]{"业务一级分类", oldCustomer.getBusinessCategory(), newRequest.getBusinessCategory()});
        fieldMap.put("businessType", new String[]{"业务二级分类", oldCustomer.getBusinessType(), newRequest.getBusinessType()});
        fieldMap.put("cooperationCategory", new String[]{"合作一级分类", oldCustomer.getCooperationCategory(), newRequest.getCooperationCategory()});
        fieldMap.put("cooperationStatus", new String[]{"合作二级分类", oldCustomer.getCooperationStatus(), newRequest.getCooperationStatus()});
        fieldMap.put("gasScale", new String[]{"用气规模", oldCustomer.getGasScale(), newRequest.getGasScale()});
        fieldMap.put("smartGasSystem", new String[]{"智慧燃气系统", oldCustomer.getSmartGasSystem(), newRequest.getSmartGasSystem()});
        fieldMap.put("contractInfo", new String[]{"合同信息", oldCustomer.getContractInfo(), newRequest.getContractInfo()});
        fieldMap.put("followUpPerson", new String[]{"跟进人", oldCustomer.getFollowUpPerson(), newRequest.getFollowUpPerson()});

        for (Map.Entry<String, String[]> entry : fieldMap.entrySet()) {
            String fieldName = entry.getKey();
            String[] values = entry.getValue();
            String oldVal = values[1] != null ? values[1] : "";
            String newVal = values[2] != null ? values[2] : "";

            if (!oldVal.equals(newVal)) {
                BizCustomerModifyLog modifyLog = new BizCustomerModifyLog();
                modifyLog.setCustomerId(customerId);
                modifyLog.setFieldName(fieldName);
                modifyLog.setOldValue(oldVal);
                modifyLog.setNewValue(newVal);
                modifyLog.setModifyUserId(userId);
                modifyLog.setModifyUser(username);
                modifyLog.setModifyTime(LocalDateTime.now());
                modifyLog.setTenantId(tenantId);
                bizCustomerModifyLogMapper.insert(modifyLog);
            }
        }
    }

    private LocalDate getContractExpireDate(Long customerId) {
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizContract::getCustomerId, customerId);
        wrapper.eq(BizContract::getContractStatus, "已生效");
        wrapper.isNotNull(BizContract::getExpireDate);
        wrapper.orderByAsc(BizContract::getExpireDate);
        wrapper.last("LIMIT 1");
        
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizContract::getTenantId, UserContext.getTenantId());
        }
        
        BizContract contract = bizContractMapper.selectOne(wrapper);
        return contract != null ? contract.getExpireDate() : null;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}

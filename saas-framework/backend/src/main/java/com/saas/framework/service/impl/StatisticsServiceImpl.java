package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.*;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.*;
import com.saas.framework.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private BizCustomerMapper customerMapper;

    @Resource
    private BizRepairOrderMapper repairOrderMapper;

    @Resource
    private BizFollowUpRecordMapper followUpRecordMapper;

    @Resource
    private BizContractMapper contractMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public CustomerStatsVO customerStats(String startDate, String endDate) {
        LambdaQueryWrapper<BizCustomer> wrapper = buildCustomerWrapper(startDate, endDate);
        List<BizCustomer> allCustomers = customerMapper.selectList(wrapper);

        List<BizCustomer> validCustomers = allCustomers.stream()
                .filter(c -> c.getIsInvalid() == null || c.getIsInvalid() != 1)
                .collect(Collectors.toList());
        List<BizCustomer> invalidCustomers = allCustomers.stream()
                .filter(c -> c.getIsInvalid() != null && c.getIsInvalid() == 1)
                .collect(Collectors.toList());

        CustomerStatsVO vo = new CustomerStatsVO();
        vo.setTotalCount(allCustomers.size());
        vo.setValidCount(validCustomers.size());
        vo.setInvalidCount(invalidCustomers.size());

        vo.setValidCustomers(buildSubStats(validCustomers));
        vo.setInvalidCustomers(buildSubStats(invalidCustomers));

        return vo;
    }

    private CustomerStatsVO.CustomerSubStats buildSubStats(List<BizCustomer> customers) {
        CustomerStatsVO.CustomerSubStats sub = new CustomerStatsVO.CustomerSubStats();

        Map<String, Long> categoryCount = customers.stream()
                .filter(c -> c.getBusinessCategory() != null)
                .collect(Collectors.groupingBy(BizCustomer::getBusinessCategory, Collectors.counting()));
        sub.setByBusinessCategory(toMapList(categoryCount));

        Map<String, Long> statusCount = customers.stream()
                .filter(c -> c.getCooperationStatus() != null)
                .collect(Collectors.groupingBy(BizCustomer::getCooperationStatus, Collectors.counting()));
        sub.setByCooperationStatus(toMapList(statusCount));

        Map<String, Long> regionCount = customers.stream()
                .filter(c -> c.getAddress() != null && !c.getAddress().isEmpty())
                .collect(Collectors.groupingBy(c -> {
                    String address = c.getAddress();
                    if (address.contains("/")) {
                        String[] parts = address.split("/");
                        if (parts.length >= 2) {
                            return parts[1];
                        }
                        return parts[0];
                    }
                    return address;
                }, Collectors.counting()));
        sub.setByRegion(toMapList(regionCount));

        Map<String, Long> typeCount = customers.stream()
                .filter(c -> c.getBusinessType() != null)
                .collect(Collectors.groupingBy(BizCustomer::getBusinessType, Collectors.counting()));
        sub.setByBusinessType(toMapList(typeCount));

        return sub;
    }

    @Override
    public RepairTrendStatsVO repairStats(String startDate, String endDate, String period) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = buildRepairWrapper(startDate, endDate);
        List<BizRepairOrder> orders = repairOrderMapper.selectList(wrapper);

        RepairTrendStatsVO vo = new RepairTrendStatsVO();
        vo.setTotalCount(orders.size());

        vo.setResolvedCount(orders.stream().filter(o -> "已解决".equals(o.getStatus())).count());
        vo.setUnresolvedCount(orders.stream().filter(o -> "未处理".equals(o.getStatus())).count());
        vo.setProcessingCount(orders.stream().filter(o -> "处理中".equals(o.getStatus())).count());
        vo.setExceptionCount(orders.stream().filter(o -> o.getIsException() != null && o.getIsException() == 1).count());

        Map<String, Long> monthCount = orders.stream()
                .filter(o -> o.getRepairTime() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getRepairTime().format(MONTH_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.counting()));
        vo.setByMonth(toMapList(monthCount));

        Map<String, Long> faultTypeCount = orders.stream()
                .filter(o -> o.getRepairType() != null)
                .collect(Collectors.groupingBy(BizRepairOrder::getRepairType, Collectors.counting()));
        List<Map<String, Object>> faultTypes = toMapListSorted(faultTypeCount);
        vo.setByFaultType(faultTypes);
        vo.setHighFrequencyFaultTypes(faultTypes.stream().limit(10).collect(Collectors.toList()));

        Map<String, Long> customerCount = orders.stream()
                .filter(o -> o.getCustomerName() != null)
                .collect(Collectors.groupingBy(BizRepairOrder::getCustomerName, Collectors.counting()));
        List<Map<String, Object>> highFreqCustomers = toMapListSorted(customerCount);
        vo.setHighFrequencyCustomers(highFreqCustomers.stream().limit(10).collect(Collectors.toList()));

        Map<String, Long> customerTypeCount = new LinkedHashMap<>();
        Set<Long> customerIds = orders.stream()
                .map(BizRepairOrder::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!customerIds.isEmpty()) {
            Map<Long, BizCustomer> customerMap = customerMapper.selectBatchIds(customerIds).stream()
                    .collect(Collectors.toMap(BizCustomer::getId, c -> c, (a, b) -> a));
            for (BizRepairOrder order : orders) {
                if (order.getCustomerId() != null) {
                    BizCustomer customer = customerMap.get(order.getCustomerId());
                    if (customer != null && customer.getBusinessCategory() != null) {
                        customerTypeCount.merge(customer.getBusinessCategory(), 1L, Long::sum);
                    }
                }
            }
        }
        vo.setByCustomerType(toMapList(customerTypeCount));

        return vo;
    }

    @Override
    public VisitStatsVO visitStats(String startDate, String endDate) {
        LambdaQueryWrapper<BizFollowUpRecord> wrapper = buildFollowUpWrapper(startDate, endDate);
        List<BizFollowUpRecord> records = followUpRecordMapper.selectList(wrapper);

        LambdaQueryWrapper<BizCustomer> customerWrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            customerWrapper.eq(BizCustomer::getTenantId, UserContext.getTenantId());
        }
        long totalCustomers = customerMapper.selectCount(customerWrapper);

        VisitStatsVO vo = new VisitStatsVO();
        vo.setTotalVisits(records.size());
        vo.setCompletedVisits(records.stream().filter(r -> r.getFollowUpStatus() != null && r.getFollowUpStatus() == 2).count());
        vo.setCompletionRate(records.isEmpty() ? 0 : Math.round(vo.getCompletedVisits() * 10000.0 / records.size()) / 100.0);

        Set<Long> visitedCustomerIds = records.stream()
                .map(BizFollowUpRecord::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        vo.setCoveredCustomers(visitedCustomerIds.size());
        vo.setTotalCustomers(totalCustomers);
        vo.setCoverageRate(totalCustomers == 0 ? 0 : Math.round(visitedCustomerIds.size() * 10000.0 / totalCustomers) / 100.0);

        Map<String, Long> personCount = records.stream()
                .filter(r -> r.getFollowUpPerson() != null)
                .collect(Collectors.groupingBy(BizFollowUpRecord::getFollowUpPerson, Collectors.counting()));
        vo.setByPerson(toMapList(personCount));

        Map<String, Long> methodCount = records.stream()
                .filter(r -> r.getFollowUpMethod() != null)
                .collect(Collectors.groupingBy(r -> {
                    switch (r.getFollowUpMethod()) {
                        case 1: return "电话";
                        case 2: return "微信";
                        case 3: return "邮件";
                        case 4: return "上门拜访";
                        default: return "其他";
                    }
                }, Collectors.counting()));
        vo.setByMethod(toMapList(methodCount));

        Map<String, Long> monthCount = records.stream()
                .filter(r -> r.getFollowUpTime() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getFollowUpTime().format(MONTH_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.counting()));
        vo.setByMonth(toMapList(monthCount));

        Map<String, Long> customerTypeCount = new LinkedHashMap<>();
        Set<Long> visitedCustomerIdsForType = records.stream()
                .map(BizFollowUpRecord::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!visitedCustomerIdsForType.isEmpty()) {
            Map<Long, BizCustomer> customerMap = customerMapper.selectBatchIds(visitedCustomerIdsForType).stream()
                    .collect(Collectors.toMap(BizCustomer::getId, c -> c, (a, b) -> a));
            for (BizFollowUpRecord record : records) {
                if (record.getCustomerId() != null) {
                    BizCustomer customer = customerMap.get(record.getCustomerId());
                    if (customer != null && customer.getBusinessCategory() != null) {
                        customerTypeCount.merge(customer.getBusinessCategory(), 1L, Long::sum);
                    }
                }
            }
        }
        vo.setByCustomerType(toMapList(customerTypeCount));

        return vo;
    }

    @Override
    public ContractStatsVO contractStats(String startDate, String endDate) {
        LambdaQueryWrapper<BizContract> wrapper = buildContractWrapper(startDate, endDate);
        List<BizContract> contracts = contractMapper.selectList(wrapper);

        ContractStatsVO vo = new ContractStatsVO();
        vo.setTotalCount(contracts.size());
        vo.setActiveCount(contracts.stream().filter(c -> "已生效".equals(c.getContractStatus())).count());
        vo.setTerminatedCount(contracts.stream().filter(c -> "已终止".equals(c.getContractStatus())).count());

        Map<String, Long> statusCount = contracts.stream()
                .filter(c -> c.getContractStatus() != null)
                .collect(Collectors.groupingBy(BizContract::getContractStatus, Collectors.counting()));
        vo.setByStatus(toMapList(statusCount));

        Map<String, Long> monthCount = contracts.stream()
                .filter(c -> c.getSignDate() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getSignDate().format(MONTH_FORMATTER),
                        LinkedHashMap::new,
                        Collectors.counting()));
        vo.setByMonth(toMapList(monthCount));

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (BizContract c : contracts) {
            if (c.getSignDate() != null && c.getContractAmount() != null) {
                String month = c.getSignDate().format(MONTH_FORMATTER);
                revenueByMonth.merge(month, c.getContractAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> revenueList = new ArrayList<>();
        revenueByMonth.forEach((month, amount) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", month);
            item.put("value", amount);
            revenueList.add(item);
        });
        vo.setRevenueByMonth(revenueList);

        return vo;
    }

    @Override
    public void exportCustomerStats(HttpServletResponse response, String startDate, String endDate) {
        CustomerStatsVO stats = customerStats(startDate, endDate);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户统计");

            String[] headers = {"统计维度", "分类", "数量"};
            createHeaderRow(sheet, headers);

            int rowIndex = 1;
            Row summaryRow1 = sheet.createRow(rowIndex++);
            summaryRow1.createCell(0).setCellValue("客户总数");
            summaryRow1.createCell(1).setCellValue(stats.getTotalCount() + "");
            Row summaryRow2 = sheet.createRow(rowIndex++);
            summaryRow2.createCell(0).setCellValue("有效客户数");
            summaryRow2.createCell(1).setCellValue(stats.getValidCount() + "");
            Row summaryRow3 = sheet.createRow(rowIndex++);
            summaryRow3.createCell(0).setCellValue("无效客户数");
            summaryRow3.createCell(1).setCellValue(stats.getInvalidCount() + "");

            if (stats.getValidCustomers() != null) {
                rowIndex++;
                rowIndex = fillStatsRows(sheet, rowIndex, "有效-业务类型", stats.getValidCustomers().getByBusinessCategory());
                rowIndex = fillStatsRows(sheet, rowIndex, "有效-合作状态", stats.getValidCustomers().getByCooperationStatus());
                rowIndex = fillStatsRows(sheet, rowIndex, "有效-区域分布", stats.getValidCustomers().getByRegion());
                rowIndex = fillStatsRows(sheet, rowIndex, "有效-业务细分类", stats.getValidCustomers().getByBusinessType());
            }
            if (stats.getInvalidCustomers() != null) {
                rowIndex++;
                rowIndex = fillStatsRows(sheet, rowIndex, "无效-业务类型", stats.getInvalidCustomers().getByBusinessCategory());
                rowIndex = fillStatsRows(sheet, rowIndex, "无效-合作状态", stats.getInvalidCustomers().getByCooperationStatus());
                rowIndex = fillStatsRows(sheet, rowIndex, "无效-区域分布", stats.getInvalidCustomers().getByRegion());
                fillStatsRows(sheet, rowIndex, "无效-业务细分类", stats.getInvalidCustomers().getByBusinessType());
            }

            writeExcelResponse(response, workbook, "客户统计");
        } catch (IOException e) {
            log.error("客户统计导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportRepairStats(HttpServletResponse response, String startDate, String endDate, String period) {
        RepairTrendStatsVO stats = repairStats(startDate, endDate, period);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("报修统计");

            String[] headers = {"统计维度", "分类", "数量"};
            createHeaderRow(sheet, headers);

            int rowIndex = 1;
            rowIndex = fillStatsRows(sheet, rowIndex, "月度趋势", stats.getByMonth());
            rowIndex = fillStatsRows(sheet, rowIndex, "客户类型", stats.getByCustomerType());
            rowIndex = fillStatsRows(sheet, rowIndex, "故障类型", stats.getByFaultType());
            fillStatsRows(sheet, rowIndex, "高频客户", stats.getHighFrequencyCustomers());

            writeExcelResponse(response, workbook, "报修统计");
        } catch (IOException e) {
            log.error("报修统计导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportVisitStats(HttpServletResponse response, String startDate, String endDate) {
        VisitStatsVO stats = visitStats(startDate, endDate);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("拜访统计");

            String[] headers = {"统计维度", "分类", "数量"};
            createHeaderRow(sheet, headers);

            int rowIndex = 1;
            rowIndex = fillStatsRows(sheet, rowIndex, "跟进人", stats.getByPerson());
            rowIndex = fillStatsRows(sheet, rowIndex, "跟进方式", stats.getByMethod());
            rowIndex = fillStatsRows(sheet, rowIndex, "月度趋势", stats.getByMonth());
            fillStatsRows(sheet, rowIndex, "客户类型", stats.getByCustomerType());

            writeExcelResponse(response, workbook, "拜访统计");
        } catch (IOException e) {
            log.error("拜访统计导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportContractStats(HttpServletResponse response, String startDate, String endDate) {
        ContractStatsVO stats = contractStats(startDate, endDate);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("合同统计");

            String[] headers = {"统计维度", "分类", "数量/金额"};
            createHeaderRow(sheet, headers);

            int rowIndex = 1;
            rowIndex = fillStatsRows(sheet, rowIndex, "合同类型", stats.getByType());
            rowIndex = fillStatsRows(sheet, rowIndex, "合同状态", stats.getByStatus());
            rowIndex = fillStatsRows(sheet, rowIndex, "月度趋势", stats.getByMonth());

            for (Map<String, Object> item : stats.getRevenueByMonth()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("营收月度");
                row.createCell(1).setCellValue(item.get("name") != null ? item.get("name").toString() : "");
                row.createCell(2).setCellValue(item.get("value") != null ? item.get("value").toString() : "0");
            }

            writeExcelResponse(response, workbook, "合同统计");
        } catch (IOException e) {
            log.error("合同统计导出失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private LambdaQueryWrapper<BizCustomer> buildCustomerWrapper(String startDate, String endDate) {
        LambdaQueryWrapper<BizCustomer> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizCustomer::getTenantId, UserContext.getTenantId());
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(BizCustomer::getCreateTime, LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(BizCustomer::getCreateTime, LocalDate.parse(endDate, DATE_FORMATTER).atTime(23, 59, 59));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<BizRepairOrder> buildRepairWrapper(String startDate, String endDate) {
        LambdaQueryWrapper<BizRepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizRepairOrder::getTenantId, UserContext.getTenantId());
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(BizRepairOrder::getRepairTime, LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(BizRepairOrder::getRepairTime, LocalDate.parse(endDate, DATE_FORMATTER).atTime(23, 59, 59));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<BizFollowUpRecord> buildFollowUpWrapper(String startDate, String endDate) {
        LambdaQueryWrapper<BizFollowUpRecord> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizFollowUpRecord::getTenantId, UserContext.getTenantId());
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(BizFollowUpRecord::getFollowUpTime, LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(BizFollowUpRecord::getFollowUpTime, LocalDate.parse(endDate, DATE_FORMATTER).atTime(23, 59, 59));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<BizContract> buildContractWrapper(String startDate, String endDate) {
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizContract::getTenantId, UserContext.getTenantId());
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(BizContract::getSignDate, LocalDate.parse(startDate, DATE_FORMATTER));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(BizContract::getSignDate, LocalDate.parse(endDate, DATE_FORMATTER));
        }
        return wrapper;
    }

    private List<Map<String, Object>> toMapList(Map<String, Long> countMap) {
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((key, value) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", key);
            item.put("value", value);
            result.add(item);
        });
        return result;
    }

    private List<Map<String, Object>> toMapListSorted(Map<String, Long> countMap) {
        return countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private void createHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
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
    }

    private int fillStatsRows(Sheet sheet, int rowIndex, String dimension, List<Map<String, Object>> data) {
        for (Map<String, Object> item : data) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(dimension);
            row.createCell(1).setCellValue(item.get("name") != null ? item.get("name").toString() : "");
            row.createCell(2).setCellValue(item.get("value") != null ? item.get("value").toString() : "0");
        }
        return rowIndex;
    }

    private void writeExcelResponse(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName + ".xlsx", "UTF-8"));
        workbook.write(response.getOutputStream());
        log.info("导出{}统计完成", fileName);
    }
}

package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.IndependentStockInRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.StockInCheckRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.Device;
import com.saas.framework.entity.DeviceInventory;
import com.saas.framework.entity.DeviceStockInOrder;
import com.saas.framework.mapper.DeviceInventoryMapper;
import com.saas.framework.mapper.DeviceMapper;
import com.saas.framework.mapper.DeviceStockInOrderMapper;
import com.saas.framework.service.DeviceTimelineService;
import com.saas.framework.service.StockInService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
@Service
public class StockInServiceImpl implements StockInService {

    @Resource
    private DeviceStockInOrderMapper stockInOrderMapper;

    @Resource
    private DeviceInventoryMapper inventoryMapper;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DeviceTimelineService deviceTimelineService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public PageResult<DeviceStockInOrder> page(int page, int size, String orderNo, String itemName,
                                                 String warehouseName, String stockInDateStart,
                                                 String stockInDateEnd, Integer checkStatus) {
        LambdaQueryWrapper<DeviceStockInOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(orderNo)) {
            wrapper.like(DeviceStockInOrder::getOrderNo, orderNo);
        }
        if (StringUtils.hasText(itemName)) {
            wrapper.like(DeviceStockInOrder::getItemName, itemName);
        }
        if (StringUtils.hasText(warehouseName)) {
            wrapper.like(DeviceStockInOrder::getWarehouseName, warehouseName);
        }
        if (StringUtils.hasText(stockInDateStart)) {
            wrapper.ge(DeviceStockInOrder::getCreateTime, LocalDateTime.parse(stockInDateStart + "T00:00:00"));
        }
        if (StringUtils.hasText(stockInDateEnd)) {
            wrapper.le(DeviceStockInOrder::getCreateTime, LocalDateTime.parse(stockInDateEnd + "T23:59:59"));
        }
        if (checkStatus != null) {
            wrapper.eq(DeviceStockInOrder::getCheckStatus, checkStatus);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(DeviceStockInOrder::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(DeviceStockInOrder::getCreateTime);

        IPage<DeviceStockInOrder> iPage = stockInOrderMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(iPage);
    }

    @Override
    public DeviceStockInOrder detail(Long id) {
        DeviceStockInOrder order = stockInOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "入库单不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的入库数据");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void independentStockIn(IndependentStockInRequest request) {
        if (request.getItemType() == null) {
            throw new BusinessException("物料类型不能为空");
        }
        if (!StringUtils.hasText(request.getItemName())) {
            throw new BusinessException("物料名称不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("入库数量必须大于0");
        }
        if (!StringUtils.hasText(request.getWarehouseName())) {
            throw new BusinessException("入库仓库不能为空");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        Long finalTenantId = tenantId != null ? tenantId : 0L;

        String handler = StringUtils.hasText(request.getHandler()) ? request.getHandler() : UserContext.getUsername();

        // 生成入库单
        DeviceStockInOrder stockInOrder = new DeviceStockInOrder();
        stockInOrder.setOrderNo(generateStockInOrderNo());
        stockInOrder.setItemType(request.getItemType());
        stockInOrder.setItemName(request.getItemName());
        stockInOrder.setBrand(request.getBrand());
        stockInOrder.setModel(request.getModel());
        stockInOrder.setSpec(request.getSpec());
        stockInOrder.setQuantity(request.getQuantity());
        stockInOrder.setUnit(request.getUnit());
        stockInOrder.setWarehouseName(request.getWarehouseName());
        stockInOrder.setLocation(request.getLocation());
        stockInOrder.setCheckStatus(0); // 待验收
        stockInOrder.setHandler(handler);
        stockInOrder.setRemark(request.getRemark());
        stockInOrder.setTenantId(finalTenantId);

        // 设备入库：查找或创建 Device 记录
        if (request.getItemType() == 1) {
            stockInOrderMapper.insert(stockInOrder);

            for (int i = 0; i < request.getQuantity(); i++) {
                Device device = new Device();
                device.setDeviceCode(generateDeviceCode(request.getItemName()));
                device.setDeviceName(request.getItemName());
                device.setBrand(request.getBrand());
                device.setModel(request.getModel());
                device.setSpec(request.getSpec());
                device.setStockInOrderId(stockInOrder.getId());
                device.setStatus(1); // 待安装
                device.setWarehouseName(request.getWarehouseName());
                device.setTenantId(finalTenantId);
                deviceMapper.insert(device);

                // 记录时间线
                deviceTimelineService.recordEvent(device.getId(), 2, "独立入库",
                        "入库至仓库: " + request.getWarehouseName(),
                        stockInOrder.getId(), stockInOrder.getOrderNo(), handler, finalTenantId);
            }
        } else {
            // 配件入库：只插入入库单
            stockInOrderMapper.insert(stockInOrder);
        }

        // 更新库存台账
        updateInventory(request, finalTenantId);

        log.info("独立入库完成: orderNo={}, itemName={}, quantity={}, warehouseName={}",
                stockInOrder.getOrderNo(), request.getItemName(), request.getQuantity(), request.getWarehouseName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void check(Long id, StockInCheckRequest request) {
        if (request.getCheckStatus() == null) {
            throw new BusinessException("验收状态不能为空");
        }

        DeviceStockInOrder order = stockInOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "入库单不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权操作其他租户的入库数据");
        }

        order.setCheckStatus(request.getCheckStatus());
        order.setCheckPhoto(request.getCheckPhoto());
        stockInOrderMapper.updateById(order);

        log.info("入库验收完成: id={}, checkStatus={}", id, request.getCheckStatus());
    }

    /**
     * 更新库存台账
     */
    private void updateInventory(IndependentStockInRequest request, Long tenantId) {
        LambdaQueryWrapper<DeviceInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceInventory::getItemType, request.getItemType());
        wrapper.eq(DeviceInventory::getItemName, request.getItemName());
        wrapper.eq(DeviceInventory::getModel, request.getModel() != null ? request.getModel() : "");
        wrapper.eq(DeviceInventory::getSpec, request.getSpec() != null ? request.getSpec() : "");
        wrapper.eq(DeviceInventory::getWarehouseName, request.getWarehouseName());
        wrapper.eq(DeviceInventory::getTenantId, tenantId);

        DeviceInventory inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            inventory = new DeviceInventory();
            inventory.setItemType(request.getItemType());
            inventory.setItemName(request.getItemName());
            inventory.setBrand(request.getBrand());
            inventory.setModel(request.getModel());
            inventory.setSpec(request.getSpec());
            inventory.setUnit(request.getUnit());
            inventory.setWarehouseName(request.getWarehouseName());
            inventory.setTotalQty(request.getQuantity());
            inventory.setStockedInQty(request.getQuantity());
            inventory.setStockedOutQty(0);
            inventory.setTenantId(tenantId);
            inventoryMapper.insert(inventory);
        } else {
            inventory.setTotalQty(inventory.getTotalQty() + request.getQuantity());
            inventory.setStockedInQty(inventory.getStockedInQty() + request.getQuantity());
            inventoryMapper.updateById(inventory);
        }
    }

    /**
     * 生成入库单号：RK + yyyyMMddHHmmss + 3位随机数
     */
    private String generateStockInOrderNo() {
        String datePart = LocalDateTime.now().format(DATETIME_FORMATTER);
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return "RK" + datePart + randomPart;
    }

    /**
     * 生成设备档案编码：DEV + 分类码(2位) + yyyyMM + 4位流水号
     */
    private String generateDeviceCode(String deviceName) {
        String categoryCode = "01";
        String monthPart = LocalDate.now().format(MONTH_FORMATTER);
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "DEV" + categoryCode + monthPart + randomPart;
    }
}

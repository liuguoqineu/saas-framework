package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.IndependentStockOutRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.Device;
import com.saas.framework.entity.DeviceInventory;
import com.saas.framework.entity.DeviceStockOutOrder;
import com.saas.framework.mapper.DeviceInventoryMapper;
import com.saas.framework.mapper.DeviceMapper;
import com.saas.framework.mapper.DeviceStockOutOrderMapper;
import com.saas.framework.service.DeviceTimelineService;
import com.saas.framework.service.StockOutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class StockOutServiceImpl implements StockOutService {

    @Resource
    private DeviceStockOutOrderMapper stockOutOrderMapper;

    @Resource
    private DeviceInventoryMapper inventoryMapper;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DeviceTimelineService deviceTimelineService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PageResult<DeviceStockOutOrder> page(int page, int size, String orderNo, String itemName,
                                                  Integer usageType, String receiver,
                                                  String stockOutDateStart, String stockOutDateEnd) {
        LambdaQueryWrapper<DeviceStockOutOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(orderNo)) {
            wrapper.like(DeviceStockOutOrder::getOrderNo, orderNo);
        }
        if (StringUtils.hasText(itemName)) {
            wrapper.like(DeviceStockOutOrder::getItemName, itemName);
        }
        if (usageType != null) {
            wrapper.eq(DeviceStockOutOrder::getUsageType, usageType);
        }
        if (StringUtils.hasText(receiver)) {
            wrapper.like(DeviceStockOutOrder::getReceiver, receiver);
        }
        if (StringUtils.hasText(stockOutDateStart)) {
            wrapper.ge(DeviceStockOutOrder::getCreateTime, LocalDateTime.parse(stockOutDateStart + "T00:00:00"));
        }
        if (StringUtils.hasText(stockOutDateEnd)) {
            wrapper.le(DeviceStockOutOrder::getCreateTime, LocalDateTime.parse(stockOutDateEnd + "T23:59:59"));
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(DeviceStockOutOrder::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(DeviceStockOutOrder::getCreateTime);

        IPage<DeviceStockOutOrder> iPage = stockOutOrderMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(iPage);
    }

    @Override
    public DeviceStockOutOrder detail(Long id) {
        DeviceStockOutOrder order = stockOutOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "出库单不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的出库数据");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockOut(IndependentStockOutRequest request) {
        // 1. 参数校验
        if (request.getInventoryId() == null) {
            throw new BusinessException("库存ID不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("出库数量必须大于0");
        }
        if (request.getUsageType() == null) {
            throw new BusinessException("领用用途不能为空");
        }
        if (!StringUtils.hasText(request.getReceiver())) {
            throw new BusinessException("领用人不能为空");
        }

        // 2. 获取库存记录，校验库存是否充足
        DeviceInventory inventory = inventoryMapper.selectById(request.getInventoryId());
        if (inventory == null) {
            throw new BusinessException(404, "库存记录不存在");
        }
        if (request.getQuantity() > inventory.getTotalQty()) {
            throw new BusinessException("出库数量(" + request.getQuantity() + ")超过当前库存(" + inventory.getTotalQty() + ")");
        }

        Long tenantId = inventory.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : UserContext.getTenantId();
        }

        // 3. 生成出库单号
        String orderNo = generateStockOutOrderNo();

        // 4. 创建出库单
        DeviceStockOutOrder stockOutOrder = new DeviceStockOutOrder();
        stockOutOrder.setOrderNo(orderNo);
        stockOutOrder.setItemType(inventory.getItemType());
        stockOutOrder.setItemName(inventory.getItemName());
        stockOutOrder.setBrand(inventory.getBrand());
        stockOutOrder.setModel(inventory.getModel());
        stockOutOrder.setSpec(inventory.getSpec());
        stockOutOrder.setQuantity(request.getQuantity());
        stockOutOrder.setUnit(inventory.getUnit());
        stockOutOrder.setUsageType(request.getUsageType());
        stockOutOrder.setRepairOrderId(request.getRepairOrderId());
        stockOutOrder.setDeptName(request.getDeptName());
        stockOutOrder.setReceiver(request.getReceiver());
        stockOutOrder.setReceiverPhone(request.getReceiverPhone());
        stockOutOrder.setReviewer(request.getReviewer());
        stockOutOrder.setOutPhoto(request.getOutPhoto());
        stockOutOrder.setRemark(request.getRemark());
        stockOutOrder.setTenantId(tenantId);

        // 用途类型标签
        String usageTypeLabel = getUsageTypeLabel(request.getUsageType());

        // 5. 设备出库：查找该仓库中待安装的设备
        if (inventory.getItemType() != null && inventory.getItemType() == 1) {
            LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
            deviceWrapper.eq(Device::getDeviceName, inventory.getItemName());
            deviceWrapper.eq(Device::getWarehouseName, inventory.getWarehouseName());
            deviceWrapper.eq(Device::getStatus, 1); // 待安装
            deviceWrapper.orderByAsc(Device::getId);
            deviceWrapper.last("LIMIT " + request.getQuantity());
            List<Device> devices = deviceMapper.selectList(deviceWrapper);

            if (devices.size() < request.getQuantity()) {
                throw new BusinessException("仓库中待安装的设备数量不足，无法完成出库");
            }

            stockOutOrderMapper.insert(stockOutOrder);

            // 更新每台设备的状态和出库信息
            for (Device device : devices) {
                device.setStatus(2); // 在用
                device.setWarehouseName(null); // 出库后清空仓库
                device.setStockOutOrderId(stockOutOrder.getId());
                deviceMapper.updateById(device);
            }

            // 设置出库单的设备ID
            stockOutOrder.setOutDeviceId(devices.get(0).getId());
            stockOutOrderMapper.updateById(stockOutOrder);

            // 8. 为每台出库设备记录时间线事件
            for (Device device : devices) {
                deviceTimelineService.recordEvent(device.getId(), 3, "设备出库",
                        "出库用途: " + usageTypeLabel + ", 领用人: " + request.getReceiver(),
                        stockOutOrder.getId(), stockOutOrder.getOrderNo(), UserContext.getUsername(), tenantId);
            }
        } else {
            // 6. 配件出库：只插入出库单
            stockOutOrderMapper.insert(stockOutOrder);
        }

        // 7. 更新库存：减少当前库存，增加累计出库
        inventory.setTotalQty(inventory.getTotalQty() - request.getQuantity());
        inventory.setStockedOutQty(inventory.getStockedOutQty() + request.getQuantity());
        inventoryMapper.updateById(inventory);

        log.info("独立出库完成: inventoryId={}, quantity={}, usageType={}, orderNo={}",
                request.getInventoryId(), request.getQuantity(), request.getUsageType(), stockOutOrder.getOrderNo());
    }

    private String generateStockOutOrderNo() {
        String datePart = LocalDateTime.now().format(DATETIME_FORMATTER);
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return "CK" + datePart + randomPart;
    }

    private String getUsageTypeLabel(Integer usageType) {
        if (usageType == null) {
            return "未知";
        }
        switch (usageType) {
            case 1:
                return "新装设备";
            case 2:
                return "维修更换";
            case 3:
                return "抢修备用";
            default:
                return "未知";
        }
    }
}

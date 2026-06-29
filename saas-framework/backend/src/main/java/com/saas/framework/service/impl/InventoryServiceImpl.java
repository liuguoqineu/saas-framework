package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.StockOutRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.Device;
import com.saas.framework.entity.DeviceInventory;
import com.saas.framework.entity.DeviceStockOutOrder;
import com.saas.framework.mapper.DeviceInventoryMapper;
import com.saas.framework.mapper.DeviceMapper;
import com.saas.framework.mapper.DeviceStockOutOrderMapper;
import com.saas.framework.service.InventoryService;
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
public class InventoryServiceImpl implements InventoryService {

    @Resource
    private DeviceInventoryMapper inventoryMapper;

    @Resource
    private DeviceStockOutOrderMapper stockOutOrderMapper;

    @Resource
    private DeviceMapper deviceMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PageResult<DeviceInventory> page(int page, int size, String itemName, String warehouseName,
                                              Integer itemType, Boolean lowStock) {
        LambdaQueryWrapper<DeviceInventory> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(itemName)) {
            wrapper.like(DeviceInventory::getItemName, itemName);
        }
        if (StringUtils.hasText(warehouseName)) {
            wrapper.like(DeviceInventory::getWarehouseName, warehouseName);
        }
        if (itemType != null) {
            wrapper.eq(DeviceInventory::getItemType, itemType);
        }
        if (lowStock != null && lowStock) {
            wrapper.apply("total_qty <= min_stock_qty");
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(DeviceInventory::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(DeviceInventory::getUpdateTime);

        IPage<DeviceInventory> iPage = inventoryMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(iPage);
    }

    @Override
    public DeviceInventory detail(Long id) {
        DeviceInventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new BusinessException(404, "库存记录不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(inventory.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的库存数据");
        }
        return inventory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockOut(StockOutRequest request) {
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

        DeviceInventory inventory = inventoryMapper.selectById(request.getInventoryId());
        if (inventory == null) {
            throw new BusinessException(404, "库存记录不存在");
        }

        if (request.getQuantity() > inventory.getTotalQty()) {
            throw new BusinessException("出库数量(" + request.getQuantity() + ")超过当前库存(" + inventory.getTotalQty() + ")");
        }

        Long tenantId = inventory.getTenantId();

        // 生成出库单
        DeviceStockOutOrder stockOutOrder = new DeviceStockOutOrder();
        stockOutOrder.setOrderNo(generateStockOutOrderNo());
        stockOutOrder.setItemType(inventory.getItemType());
        stockOutOrder.setItemName(inventory.getItemName());
        stockOutOrder.setBrand(inventory.getBrand());
        stockOutOrder.setModel(inventory.getModel());
        stockOutOrder.setSpec(inventory.getSpec());
        stockOutOrder.setQuantity(request.getQuantity());
        stockOutOrder.setUnit(inventory.getUnit());
        stockOutOrder.setUsageType(request.getUsageType());
        stockOutOrder.setDeptName(request.getDeptName());
        stockOutOrder.setReceiver(request.getReceiver());
        stockOutOrder.setReceiverPhone(request.getReceiverPhone());
        stockOutOrder.setReviewer(request.getReviewer());
        stockOutOrder.setRemark(request.getRemark());
        stockOutOrder.setTenantId(tenantId);

        // 设备出库：查找该仓库中待安装的设备
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

            stockOutOrder.setOutDeviceId(devices.get(0).getId());
            stockOutOrderMapper.updateById(stockOutOrder);
        } else {
            // 配件出库
            stockOutOrderMapper.insert(stockOutOrder);
        }

        // 更新库存：减少当前库存，增加累计出库
        inventory.setTotalQty(inventory.getTotalQty() - request.getQuantity());
        inventory.setStockedOutQty(inventory.getStockedOutQty() + request.getQuantity());
        inventoryMapper.updateById(inventory);

        log.info("库存出库完成: inventoryId={}, quantity={}, usageType={}", request.getInventoryId(), request.getQuantity(), request.getUsageType());
    }

    @Override
    public List<DeviceStockOutOrder> listStockOutOrders(Long inventoryId) {
        DeviceInventory inventory = inventoryMapper.selectById(inventoryId);
        if (inventory == null) {
            throw new BusinessException(404, "库存记录不存在");
        }

        LambdaQueryWrapper<DeviceStockOutOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceStockOutOrder::getItemName, inventory.getItemName());
        if (StringUtils.hasText(inventory.getModel())) {
            wrapper.eq(DeviceStockOutOrder::getModel, inventory.getModel());
        }
        if (StringUtils.hasText(inventory.getSpec())) {
            wrapper.eq(DeviceStockOutOrder::getSpec, inventory.getSpec());
        }
        wrapper.eq(DeviceStockOutOrder::getItemType, inventory.getItemType());
        wrapper.eq(DeviceStockOutOrder::getTenantId, inventory.getTenantId());
        wrapper.orderByDesc(DeviceStockOutOrder::getCreateTime);

        return stockOutOrderMapper.selectList(wrapper);
    }

    @Override
    public void updateMinStockQty(Long id, Integer minStockQty) {
        DeviceInventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new BusinessException(404, "库存记录不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(inventory.getTenantId())) {
            throw new BusinessException(403, "无权操作其他租户的库存数据");
        }
        inventory.setMinStockQty(minStockQty);
        inventoryMapper.updateById(inventory);
        log.info("更新预警阈值: id={}, minStockQty={}", id, minStockQty);
    }

    private String generateStockOutOrderNo() {
        String datePart = LocalDateTime.now().format(DATETIME_FORMATTER);
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return "CK" + datePart + randomPart;
    }
}

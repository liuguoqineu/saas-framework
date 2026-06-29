package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.RepairProcessWithReplacementRequest;
import com.saas.framework.common.dto.ReplacementItemDTO;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.BizRepairOrderMapper;
import com.saas.framework.mapper.DeviceInventoryMapper;
import com.saas.framework.mapper.DeviceMapper;
import com.saas.framework.mapper.DeviceReplacementItemMapper;
import com.saas.framework.mapper.DeviceReplacementMapper;
import com.saas.framework.mapper.DeviceStockOutOrderMapper;
import com.saas.framework.service.DeviceReplacementService;
import com.saas.framework.service.DeviceTimelineService;
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
public class DeviceReplacementServiceImpl implements DeviceReplacementService {

    @Resource
    private DeviceReplacementMapper replacementMapper;

    @Resource
    private DeviceReplacementItemMapper replacementItemMapper;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DeviceStockOutOrderMapper stockOutOrderMapper;

    @Resource
    private DeviceInventoryMapper inventoryMapper;

    @Resource
    private DeviceTimelineService deviceTimelineService;

    @Resource
    private BizRepairOrderMapper repairOrderMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PageResult<DeviceReplacement> page(int page, int size, String replacementNo, Integer replacementType, Long repairOrderId, String replaceTimeStart, String replaceTimeEnd) {
        LambdaQueryWrapper<DeviceReplacement> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(replacementNo)) {
            wrapper.like(DeviceReplacement::getReplacementNo, replacementNo);
        }
        if (replacementType != null) {
            wrapper.eq(DeviceReplacement::getReplacementType, replacementType);
        }
        if (repairOrderId != null) {
            wrapper.eq(DeviceReplacement::getRepairOrderId, repairOrderId);
        }
        if (StringUtils.hasText(replaceTimeStart)) {
            wrapper.ge(DeviceReplacement::getReplaceTime, LocalDateTime.parse(replaceTimeStart + "T00:00:00"));
        }
        if (StringUtils.hasText(replaceTimeEnd)) {
            wrapper.le(DeviceReplacement::getReplaceTime, LocalDateTime.parse(replaceTimeEnd + "T23:59:59"));
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(DeviceReplacement::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(DeviceReplacement::getCreateTime);

        IPage<DeviceReplacement> iPage = replacementMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(iPage);
    }

    @Override
    public DeviceReplacement detail(Long id) {
        DeviceReplacement replacement = replacementMapper.selectById(id);
        if (replacement == null) {
            throw new BusinessException(404, "更换记录不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(replacement.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的更换记录");
        }

        // 查询更换明细
        LambdaQueryWrapper<DeviceReplacementItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DeviceReplacementItem::getReplacementId, id);
        List<DeviceReplacementItem> items = replacementItemMapper.selectList(itemWrapper);

        // 为每条明细补充出库单号
        for (DeviceReplacementItem item : items) {
            if (item.getStockOutOrderId() != null) {
                DeviceStockOutOrder stockOutOrder = stockOutOrderMapper.selectById(item.getStockOutOrderId());
                if (stockOutOrder != null) {
                    item.setStockOutOrderNo(stockOutOrder.getOrderNo());
                }
            }
            if (item.getNewStockOutOrderId() != null) {
                DeviceStockOutOrder stockOutOrder = stockOutOrderMapper.selectById(item.getNewStockOutOrderId());
                if (stockOutOrder != null) {
                    item.setNewStockOutOrderNo(stockOutOrder.getOrderNo());
                }
            }
        }

        replacement.setItems(items);

        // 查询关联维修单号
        if (replacement.getRepairOrderId() != null) {
            BizRepairOrder repairOrder = repairOrderMapper.selectById(replacement.getRepairOrderId());
            if (repairOrder != null) {
                replacement.setRepairNo(repairOrder.getRepairNo());
            }
        }

        return replacement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceReplacement createReplacement(RepairProcessWithReplacementRequest request, Long repairOrderId, Long tenantId) {
        // 1. 生成更换单号
        String replacementNo = generateReplacementNo();

        // 2. 创建更换主记录
        DeviceReplacement replacement = new DeviceReplacement();
        replacement.setReplacementNo(replacementNo);
        replacement.setReplacementType(request.getReplacementType());
        replacement.setRepairOrderId(repairOrderId);
        replacement.setReplaceTime(LocalDateTime.now());
        replacement.setReplacePerson(request.getReplacePerson());
        replacement.setReplaceReason(request.getReplaceReason());
        replacement.setReplacePhoto(request.getReplacePhoto());
        replacement.setOperator(UserContext.getUsername());
        replacement.setTenantId(tenantId);
        replacementMapper.insert(replacement);

        // 3. 处理更换明细
        List<ReplacementItemDTO> items = request.getReplacementItems();
        if (items != null && !items.isEmpty()) {
            for (ReplacementItemDTO itemDTO : items) {
                DeviceReplacementItem item = new DeviceReplacementItem();
                item.setReplacementId(replacement.getId());
                item.setItemType(itemDTO.getItemType());
                item.setOldDeviceId(itemDTO.getOldDeviceId());
                item.setOldItemName(itemDTO.getOldItemName());
                item.setOldItemModel(itemDTO.getOldItemModel());
                item.setOldItemStatus(itemDTO.getOldItemStatus());
                item.setNewDeviceId(itemDTO.getNewDeviceId());
                item.setNewItemName(itemDTO.getNewItemName());
                item.setNewItemModel(itemDTO.getNewItemModel());
                item.setNewItemQty(itemDTO.getNewItemQty());
                item.setTenantId(tenantId);

                if (itemDTO.getItemType() != null && itemDTO.getItemType() == 2) {
                    // 设备更换
                    processDeviceReplacement(itemDTO, item, replacementNo, tenantId);
                } else if (itemDTO.getItemType() != null && itemDTO.getItemType() == 1) {
                    // 配件更换
                    processPartReplacement(itemDTO, item, repairOrderId, tenantId);
                }

                replacementItemMapper.insert(item);
            }
        }

        log.info("创建更换记录: id={}, replacementNo={}, repairOrderId={}", replacement.getId(), replacementNo, repairOrderId);
        return replacement;
    }

    /**
     * 处理设备更换：更新旧设备状态、新设备状态、创建出库单、记录时间线
     */
    private void processDeviceReplacement(ReplacementItemDTO itemDTO, DeviceReplacementItem item, String replacementNo, Long tenantId) {
        // 更新旧设备状态
        if (itemDTO.getOldDeviceId() != null) {
            Device oldDevice = deviceMapper.selectById(itemDTO.getOldDeviceId());
            if (oldDevice != null) {
                // oldItemStatus: 1-报废，2-返修，3-留用
                if (itemDTO.getOldItemStatus() != null) {
                    if (itemDTO.getOldItemStatus() == 1) {
                        oldDevice.setStatus(5); // 报废
                        // 记录报废事件
                        deviceTimelineService.recordEvent(oldDevice.getId(), 9, "设备报废",
                                "设备更换后报废，更换单号: " + replacementNo,
                                null, replacementNo, UserContext.getUsername(), tenantId);
                    } else if (itemDTO.getOldItemStatus() == 2) {
                        oldDevice.setStatus(4); // 停用（返修）
                    } else if (itemDTO.getOldItemStatus() == 3) {
                        oldDevice.setStatus(4); // 停用（留用）
                    }
                    deviceMapper.updateById(oldDevice);
                }

                // 记录旧设备时间线
                deviceTimelineService.recordEvent(oldDevice.getId(), 8, "设备更换-旧件",
                        "设备被更换，旧件处理方式: " + getOldItemStatusDesc(itemDTO.getOldItemStatus()),
                        null, null, UserContext.getUsername(), tenantId);
            }
        }

        // 更新新设备状态
        if (itemDTO.getNewDeviceId() != null) {
            Device newDevice = deviceMapper.selectById(itemDTO.getNewDeviceId());
            if (newDevice != null) {
                // 创建出库单
                DeviceStockOutOrder stockOutOrder = new DeviceStockOutOrder();
                stockOutOrder.setOrderNo(generateStockOutOrderNo());
                stockOutOrder.setItemType(1);
                stockOutOrder.setItemName(newDevice.getDeviceName());
                stockOutOrder.setBrand(newDevice.getBrand());
                stockOutOrder.setModel(newDevice.getModel());
                stockOutOrder.setSpec(newDevice.getSpec());
                stockOutOrder.setQuantity(1);
                stockOutOrder.setUsageType(2); // 维修更换
                stockOutOrder.setOutDeviceId(newDevice.getId());
                stockOutOrder.setReceiver(StringUtils.hasText(itemDTO.getOldItemName()) ? itemDTO.getOldItemName() : UserContext.getUsername());
                stockOutOrder.setTenantId(tenantId);
                stockOutOrderMapper.insert(stockOutOrder);

                // 更新新设备状态
                newDevice.setStatus(2); // 在用
                newDevice.setWarehouseName(null);
                newDevice.setStockOutOrderId(stockOutOrder.getId());
                deviceMapper.updateById(newDevice);

                // 记录新设备出库单ID到更换明细
                item.setNewStockOutOrderId(stockOutOrder.getId());

                // 记录新设备时间线
                deviceTimelineService.recordEvent(newDevice.getId(), 8, "设备更换-新件",
                        "设备作为替换件安装使用", stockOutOrder.getId(), stockOutOrder.getOrderNo(), UserContext.getUsername(), tenantId);
            }
        }
    }

    /**
     * 处理配件更换：创建出库单、扣减库存、记录时间线
     */
    private void processPartReplacement(ReplacementItemDTO itemDTO, DeviceReplacementItem item, Long repairOrderId, Long tenantId) {
        // 创建出库单
        DeviceStockOutOrder stockOutOrder = new DeviceStockOutOrder();
        stockOutOrder.setOrderNo(generateStockOutOrderNo());
        stockOutOrder.setItemType(2); // 配件
        stockOutOrder.setItemName(itemDTO.getNewItemName());
        stockOutOrder.setModel(itemDTO.getNewItemModel());
        stockOutOrder.setQuantity(itemDTO.getNewItemQty() != null ? itemDTO.getNewItemQty() : 1);
        stockOutOrder.setUsageType(2); // 维修更换
        stockOutOrder.setRepairOrderId(repairOrderId);
        stockOutOrder.setReceiver(UserContext.getUsername());
        stockOutOrder.setTenantId(tenantId);
        stockOutOrderMapper.insert(stockOutOrder);

        // 记录出库单ID到更换明细
        item.setStockOutOrderId(stockOutOrder.getId());

        // 扣减库存：按itemName+model+warehouseName+tenantId查找
        if (StringUtils.hasText(itemDTO.getNewItemName())) {
            LambdaQueryWrapper<DeviceInventory> invWrapper = new LambdaQueryWrapper<>();
            invWrapper.eq(DeviceInventory::getItemName, itemDTO.getNewItemName());
            if (StringUtils.hasText(itemDTO.getNewItemModel())) {
                invWrapper.eq(DeviceInventory::getModel, itemDTO.getNewItemModel());
            }
            invWrapper.eq(DeviceInventory::getTenantId, tenantId);
            List<DeviceInventory> inventories = inventoryMapper.selectList(invWrapper);

            if (!inventories.isEmpty()) {
                DeviceInventory inventory = inventories.get(0);
                int qty = itemDTO.getNewItemQty() != null ? itemDTO.getNewItemQty() : 1;
                inventory.setTotalQty(inventory.getTotalQty() - qty);
                inventory.setStockedOutQty(inventory.getStockedOutQty() + qty);
                inventoryMapper.updateById(inventory);
            }
        }

        // 记录配件更换时间线
        Long timelineDeviceId = itemDTO.getOldDeviceId();
        if (timelineDeviceId == null && repairOrderId != null) {
            BizRepairOrder repairOrder = repairOrderMapper.selectById(repairOrderId);
            if (repairOrder != null && repairOrder.getDeviceId() != null) {
                timelineDeviceId = repairOrder.getDeviceId();
            }
        }
        if (timelineDeviceId != null) {
            deviceTimelineService.recordEvent(timelineDeviceId, 7, "配件更换",
                    "更换配件: " + itemDTO.getNewItemName(), stockOutOrder.getId(), stockOutOrder.getOrderNo(), UserContext.getUsername(), tenantId);
        }
    }

    private String generateReplacementNo() {
        String datePart = LocalDateTime.now().format(DATETIME_FORMATTER);
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return "GH" + datePart + randomPart;
    }

    private String generateStockOutOrderNo() {
        String datePart = LocalDateTime.now().format(DATETIME_FORMATTER);
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return "CK" + datePart + randomPart;
    }

    private String getOldItemStatusDesc(Integer oldItemStatus) {
        if (oldItemStatus == null) return "未知";
        switch (oldItemStatus) {
            case 1: return "报废";
            case 2: return "返修";
            case 3: return "留用";
            default: return "未知";
        }
    }
}

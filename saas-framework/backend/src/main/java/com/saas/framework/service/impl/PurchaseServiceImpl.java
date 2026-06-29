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
import com.saas.framework.service.DeviceTimelineService;
import com.saas.framework.service.PurchaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PurchaseServiceImpl implements PurchaseService {

    @Resource
    private DevicePurchaseOrderMapper purchaseOrderMapper;

    @Resource
    private DevicePurchaseItemMapper purchaseItemMapper;

    @Resource
    private DeviceStockInOrderMapper stockInOrderMapper;

    @Resource
    private DeviceInventoryMapper inventoryMapper;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DeviceTimelineService deviceTimelineService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 采购单状态：待入库 */
    private static final int STATUS_PENDING = 0;
    /** 采购单状态：已入库 */
    private static final int STATUS_STOCKED = 1;

    @Override
    public PageResult<DevicePurchaseOrder> page(int page, int size, String orderNo, String supplierName,
                                                  String purchaseDateStart, String purchaseDateEnd, Integer status) {
        LambdaQueryWrapper<DevicePurchaseOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(orderNo)) {
            wrapper.like(DevicePurchaseOrder::getOrderNo, orderNo);
        }
        if (StringUtils.hasText(supplierName)) {
            wrapper.like(DevicePurchaseOrder::getSupplierName, supplierName);
        }
        if (StringUtils.hasText(purchaseDateStart)) {
            wrapper.ge(DevicePurchaseOrder::getPurchaseDate, LocalDate.parse(purchaseDateStart));
        }
        if (StringUtils.hasText(purchaseDateEnd)) {
            wrapper.le(DevicePurchaseOrder::getPurchaseDate, LocalDate.parse(purchaseDateEnd));
        }
        if (status != null) {
            wrapper.eq(DevicePurchaseOrder::getStatus, status);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(DevicePurchaseOrder::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(DevicePurchaseOrder::getCreateTime);

        IPage<DevicePurchaseOrder> iPage = purchaseOrderMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(iPage);
    }

    @Override
    public DevicePurchaseOrder detail(Long id) {
        DevicePurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "采购单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的采购数据");
        }

        // 查询采购明细
        LambdaQueryWrapper<DevicePurchaseItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DevicePurchaseItem::getOrderId, id);
        itemWrapper.orderByAsc(DevicePurchaseItem::getId);
        List<DevicePurchaseItem> items = purchaseItemMapper.selectList(itemWrapper);

        // 计算剩余可入库数量
        for (DevicePurchaseItem item : items) {
            int remainQty = item.getQuantity() - item.getStockedQty();
            item.setRemainQty(remainQty);
        }

        order.setItems(items);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DevicePurchaseOrder create(PurchaseOrderRequest request) {
        if (!StringUtils.hasText(request.getSupplierName())) {
            throw new BusinessException("供应商名称不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("采购明细不能为空");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = UserContext.getTenantId();
        }
        Long finalTenantId = tenantId != null ? tenantId : 0L;

        // 创建采购主单
        DevicePurchaseOrder order = new DevicePurchaseOrder();
        order.setOrderNo(generatePurchaseOrderNo());
        order.setPurchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDate.now());
        order.setSupplierName(request.getSupplierName());
        order.setSupplierContact(request.getSupplierContact());
        order.setSupplierPhone(request.getSupplierPhone());
        order.setSupplierAddress(request.getSupplierAddress());
        order.setSupplierUnifiedCode(request.getSupplierUnifiedCode());
        order.setTotalAmount(request.getTotalAmount());
        order.setPurchaser(request.getPurchaser());
        order.setPurchaserPhone(request.getPurchaserPhone());
        order.setStatus(STATUS_PENDING);
        order.setRemark(request.getRemark());
        order.setTenantId(finalTenantId);

        purchaseOrderMapper.insert(order);

        // 创建采购明细
        for (PurchaseOrderRequest.PurchaseItemDTO itemDTO : request.getItems()) {
            DevicePurchaseItem item = new DevicePurchaseItem();
            item.setOrderId(order.getId());
            item.setItemType(itemDTO.getItemType());
            item.setItemName(itemDTO.getItemName());
            item.setBrand(itemDTO.getBrand());
            item.setModel(itemDTO.getModel());
            item.setSpec(itemDTO.getSpec());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnit(itemDTO.getUnit());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setTotalPrice(itemDTO.getTotalPrice());
            item.setFactoryNo(itemDTO.getFactoryNo());
            item.setCertFile(itemDTO.getCertFile());
            item.setInspectFile(itemDTO.getInspectFile());
            item.setDeliveryFile(itemDTO.getDeliveryFile());
            item.setStockedQty(0);
            item.setTenantId(finalTenantId);

            purchaseItemMapper.insert(item);

            // 如果是设备（item_type=1），为每台设备创建一条 device 记录
            if (itemDTO.getItemType() != null && itemDTO.getItemType() == 1) {
                for (int i = 0; i < itemDTO.getQuantity(); i++) {
                    Device device = new Device();
                    device.setDeviceCode(generateDeviceCode(itemDTO.getItemName()));
                    device.setDeviceName(itemDTO.getItemName());
                    device.setBrand(itemDTO.getBrand());
                    device.setModel(itemDTO.getModel());
                    device.setSpec(itemDTO.getSpec());
                    device.setPurchaseOrderId(order.getId());
                    device.setPurchaseItemId(item.getId());
                    device.setStatus(0); // 待入库
                    device.setTenantId(finalTenantId);
                    deviceMapper.insert(device);
                    // 记录采购事件到设备履历
                    deviceTimelineService.recordEvent(device.getId(), 1, "采购入库",
                            "采购单号: " + order.getOrderNo() + ", 供应商: " + order.getSupplierName(),
                            order.getId(), order.getOrderNo(), UserContext.getUsername(), finalTenantId);
                }
            }
        }

        log.info("新增采购单: id={}, orderNo={}, supplierName={}", order.getId(), order.getOrderNo(), order.getSupplierName());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PurchaseOrderRequest request) {
        DevicePurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "采购单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权修改其他租户的采购数据");
        }

        // 已入库的采购单不允许编辑
        if (order.getStatus() == STATUS_STOCKED) {
            throw new BusinessException("已入库的采购单不允许编辑");
        }

        // 更新主单信息
        if (StringUtils.hasText(request.getSupplierName())) {
            order.setSupplierName(request.getSupplierName());
        }
        if (request.getPurchaseDate() != null) {
            order.setPurchaseDate(request.getPurchaseDate());
        }
        order.setSupplierContact(request.getSupplierContact());
        order.setSupplierPhone(request.getSupplierPhone());
        order.setSupplierAddress(request.getSupplierAddress());
        order.setSupplierUnifiedCode(request.getSupplierUnifiedCode());
        order.setTotalAmount(request.getTotalAmount());
        order.setPurchaser(request.getPurchaser());
        order.setPurchaserPhone(request.getPurchaserPhone());
        order.setRemark(request.getRemark());

        purchaseOrderMapper.updateById(order);

        // 更新采购明细
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (PurchaseOrderRequest.PurchaseItemDTO itemDTO : request.getItems()) {
                if (itemDTO.getId() != null) {
                    // 更新已有明细
                    DevicePurchaseItem existItem = purchaseItemMapper.selectById(itemDTO.getId());
                    if (existItem != null && existItem.getStockedQty() == 0) {
                        // 如果是设备类型且数量增加了，需要补充创建Device记录
                        if (itemDTO.getItemType() != null && itemDTO.getItemType() == 1) {
                            LambdaQueryWrapper<Device> deviceCountWrapper = new LambdaQueryWrapper<>();
                            deviceCountWrapper.eq(Device::getPurchaseItemId, existItem.getId());
                            long existDeviceCount = deviceMapper.selectCount(deviceCountWrapper);
                            int newQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : existItem.getQuantity();
                            if (existDeviceCount < newQty) {
                                for (int i = 0; i < newQty - existDeviceCount; i++) {
                                    Device device = new Device();
                                    device.setDeviceCode(generateDeviceCode(itemDTO.getItemName()));
                                    device.setDeviceName(itemDTO.getItemName());
                                    device.setBrand(itemDTO.getBrand());
                                    device.setModel(itemDTO.getModel());
                                    device.setSpec(itemDTO.getSpec());
                                    device.setPurchaseOrderId(id);
                                    device.setPurchaseItemId(existItem.getId());
                                    device.setStatus(0);
                                    device.setTenantId(order.getTenantId());
                                    deviceMapper.insert(device);
                                    // 记录采购事件到设备履历
                                    deviceTimelineService.recordEvent(device.getId(), 1, "采购入库",
                                            "采购单号: " + order.getOrderNo() + ", 供应商: " + order.getSupplierName(),
                                            order.getId(), order.getOrderNo(), UserContext.getUsername(), order.getTenantId());
                                }
                            }
                        }
                        existItem.setItemType(itemDTO.getItemType());
                        existItem.setItemName(itemDTO.getItemName());
                        existItem.setBrand(itemDTO.getBrand());
                        existItem.setModel(itemDTO.getModel());
                        existItem.setSpec(itemDTO.getSpec());
                        existItem.setQuantity(itemDTO.getQuantity());
                        existItem.setUnit(itemDTO.getUnit());
                        existItem.setUnitPrice(itemDTO.getUnitPrice());
                        existItem.setTotalPrice(itemDTO.getTotalPrice());
                        existItem.setFactoryNo(itemDTO.getFactoryNo());
                        existItem.setCertFile(itemDTO.getCertFile());
                        existItem.setInspectFile(itemDTO.getInspectFile());
                        existItem.setDeliveryFile(itemDTO.getDeliveryFile());
                        purchaseItemMapper.updateById(existItem);
                    }
                } else {
                    // 新增明细
                    DevicePurchaseItem item = new DevicePurchaseItem();
                    item.setOrderId(id);
                    item.setItemType(itemDTO.getItemType());
                    item.setItemName(itemDTO.getItemName());
                    item.setBrand(itemDTO.getBrand());
                    item.setModel(itemDTO.getModel());
                    item.setSpec(itemDTO.getSpec());
                    item.setQuantity(itemDTO.getQuantity());
                    item.setUnit(itemDTO.getUnit());
                    item.setUnitPrice(itemDTO.getUnitPrice());
                    item.setTotalPrice(itemDTO.getTotalPrice());
                    item.setFactoryNo(itemDTO.getFactoryNo());
                    item.setCertFile(itemDTO.getCertFile());
                    item.setInspectFile(itemDTO.getInspectFile());
                    item.setDeliveryFile(itemDTO.getDeliveryFile());
                    item.setStockedQty(0);
                    item.setTenantId(order.getTenantId());
                    purchaseItemMapper.insert(item);

                    // 设备类型，创建 device 记录
                    if (itemDTO.getItemType() != null && itemDTO.getItemType() == 1) {
                        for (int i = 0; i < itemDTO.getQuantity(); i++) {
                            Device device = new Device();
                            device.setDeviceCode(generateDeviceCode(itemDTO.getItemName()));
                            device.setDeviceName(itemDTO.getItemName());
                            device.setBrand(itemDTO.getBrand());
                            device.setModel(itemDTO.getModel());
                            device.setSpec(itemDTO.getSpec());
                            device.setPurchaseOrderId(id);
                            device.setPurchaseItemId(item.getId());
                            device.setStatus(0);
                            device.setTenantId(order.getTenantId());
                            deviceMapper.insert(device);
                            // 记录采购事件到设备履历
                            deviceTimelineService.recordEvent(device.getId(), 1, "采购入库",
                                    "采购单号: " + order.getOrderNo() + ", 供应商: " + order.getSupplierName(),
                                    order.getId(), order.getOrderNo(), UserContext.getUsername(), order.getTenantId());
                        }
                    }
                }
            }
        }

        log.info("修改采购单: id={}, orderNo={}", id, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DevicePurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "采购单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权删除其他租户的采购数据");
        }

        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException("只有待入库状态的采购单可以删除");
        }

        // 检查是否有已入库的明细
        LambdaQueryWrapper<DevicePurchaseItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DevicePurchaseItem::getOrderId, id);
        List<DevicePurchaseItem> items = purchaseItemMapper.selectList(itemWrapper);
        for (DevicePurchaseItem item : items) {
            if (item.getStockedQty() > 0) {
                throw new BusinessException("采购明细已有入库记录，无法删除");
            }
        }

        // 删除明细
        for (DevicePurchaseItem item : items) {
            purchaseItemMapper.deleteById(item.getId());
        }

        // 删除关联的设备档案
        LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
        deviceWrapper.eq(Device::getPurchaseOrderId, id);
        List<Device> devices = deviceMapper.selectList(deviceWrapper);
        for (Device device : devices) {
            deviceMapper.deleteById(device.getId());
        }

        // 删除主单
        purchaseOrderMapper.deleteById(id);

        log.info("删除采购单: id={}, orderNo={}", id, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockIn(StockInRequest request) {
        if (request.getPurchaseOrderId() == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        if (!StringUtils.hasText(request.getWarehouseName())) {
            throw new BusinessException("入库仓库不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("入库明细不能为空");
        }

        DevicePurchaseOrder order = purchaseOrderMapper.selectById(request.getPurchaseOrderId());
        if (order == null) {
            throw new BusinessException(404, "采购单不存在");
        }

        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(order.getTenantId())) {
            throw new BusinessException(403, "无权操作其他租户的采购数据");
        }

        Long tenantId = order.getTenantId();

        // 处理每条入库明细
        for (StockInRequest.StockInItemDTO itemDTO : request.getItems()) {
            if (itemDTO.getPurchaseItemId() == null) {
                throw new BusinessException("采购明细ID不能为空");
            }
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new BusinessException("入库数量必须大于0");
            }

            DevicePurchaseItem purchaseItem = purchaseItemMapper.selectById(itemDTO.getPurchaseItemId());
            if (purchaseItem == null) {
                throw new BusinessException(404, "采购明细不存在: " + itemDTO.getPurchaseItemId());
            }

            int remainQty = purchaseItem.getQuantity() - purchaseItem.getStockedQty();
            if (itemDTO.getQuantity() > remainQty) {
                throw new BusinessException("入库数量(" + itemDTO.getQuantity() + ")超过剩余可入库数量(" + remainQty + ")，物料：" + purchaseItem.getItemName());
            }

            // 生成入库单
            DeviceStockInOrder stockInOrder = new DeviceStockInOrder();
            stockInOrder.setOrderNo(generateStockInOrderNo());
            stockInOrder.setPurchaseOrderId(order.getId());
            stockInOrder.setPurchaseItemId(purchaseItem.getId());
            stockInOrder.setItemType(purchaseItem.getItemType());
            stockInOrder.setItemName(purchaseItem.getItemName());
            stockInOrder.setBrand(purchaseItem.getBrand());
            stockInOrder.setModel(purchaseItem.getModel());
            stockInOrder.setSpec(purchaseItem.getSpec());
            stockInOrder.setQuantity(itemDTO.getQuantity());
            stockInOrder.setUnit(purchaseItem.getUnit());
            stockInOrder.setWarehouseName(request.getWarehouseName());
            stockInOrder.setCheckStatus(0); // 待验收
            stockInOrder.setHandler(StringUtils.hasText(request.getHandler()) ? request.getHandler() : UserContext.getUsername());
            stockInOrder.setRemark(itemDTO.getRemark());
            stockInOrder.setTenantId(tenantId);

            // 设备入库：关联具体设备
            if (purchaseItem.getItemType() != null && purchaseItem.getItemType() == 1) {
                // 查找该采购明细下尚未入库的设备
                LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
                deviceWrapper.eq(Device::getPurchaseItemId, purchaseItem.getId());
                deviceWrapper.eq(Device::getStatus, 0); // 待入库
                deviceWrapper.orderByAsc(Device::getId);
                deviceWrapper.last("LIMIT " + itemDTO.getQuantity());
                List<Device> devices = deviceMapper.selectList(deviceWrapper);

                // 如果设备档案不足，自动补充创建
                if (devices.size() < itemDTO.getQuantity()) {
                    int needCreate = itemDTO.getQuantity() - devices.size();
                    log.warn("设备档案数量不足，采购明细ID={}，需要{}台，已有{}台，自动补充创建{}台",
                            purchaseItem.getId(), itemDTO.getQuantity(), devices.size(), needCreate);
                    for (int i = 0; i < needCreate; i++) {
                        Device device = new Device();
                        device.setDeviceCode(generateDeviceCode(purchaseItem.getItemName()));
                        device.setDeviceName(purchaseItem.getItemName());
                        device.setBrand(purchaseItem.getBrand());
                        device.setModel(purchaseItem.getModel());
                        device.setSpec(purchaseItem.getSpec());
                        device.setPurchaseOrderId(order.getId());
                        device.setPurchaseItemId(purchaseItem.getId());
                        device.setStatus(0); // 待入库
                        device.setTenantId(tenantId);
                        deviceMapper.insert(device);
                    }
                    // 重新查询
                    LambdaQueryWrapper<Device> reQueryWrapper = new LambdaQueryWrapper<>();
                    reQueryWrapper.eq(Device::getPurchaseItemId, purchaseItem.getId());
                    reQueryWrapper.eq(Device::getStatus, 0);
                    reQueryWrapper.orderByAsc(Device::getId);
                    reQueryWrapper.last("LIMIT " + itemDTO.getQuantity());
                    devices = deviceMapper.selectList(reQueryWrapper);
                }

                // 先插入入库单获取ID
                stockInOrderMapper.insert(stockInOrder);

                // 更新每台设备的状态和仓库信息
                for (Device device : devices) {
                    device.setStatus(1); // 待安装
                    device.setWarehouseName(request.getWarehouseName());
                    device.setStockInOrderId(stockInOrder.getId());
                    deviceMapper.updateById(device);
                    // 记录入库事件到设备履历
                    deviceTimelineService.recordEvent(device.getId(), 2, "采购入库",
                            "入库至仓库: " + request.getWarehouseName() + ", 采购单号: " + order.getOrderNo(),
                            stockInOrder.getId(), stockInOrder.getOrderNo(),
                            StringUtils.hasText(request.getHandler()) ? request.getHandler() : UserContext.getUsername(), tenantId);
                }
            } else {
                // 配件入库：只插入入库单
                stockInOrderMapper.insert(stockInOrder);
            }

            // 更新采购明细的已入库数量
            purchaseItem.setStockedQty(purchaseItem.getStockedQty() + itemDTO.getQuantity());
            purchaseItemMapper.updateById(purchaseItem);

            // 更新库存台账
            updateInventory(purchaseItem, itemDTO.getQuantity(), request.getWarehouseName(), tenantId);
        }

        // 更新采购单状态
        boolean allStocked = true;
        LambdaQueryWrapper<DevicePurchaseItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DevicePurchaseItem::getOrderId, order.getId());
        List<DevicePurchaseItem> allItems = purchaseItemMapper.selectList(itemWrapper);
        for (DevicePurchaseItem item : allItems) {
            if (item.getStockedQty() < item.getQuantity()) {
                allStocked = false;
                break;
            }
        }

        order.setStatus(allStocked ? STATUS_STOCKED : STATUS_PENDING);
        purchaseOrderMapper.updateById(order);

        log.info("采购单入库完成: purchaseOrderId={}, warehouseName={}", request.getPurchaseOrderId(), request.getWarehouseName());
    }

    @Override
    public List<DeviceStockInOrder> listStockInOrders(Long purchaseOrderId) {
        DevicePurchaseOrder order = purchaseOrderMapper.selectById(purchaseOrderId);
        if (order == null) {
            throw new BusinessException(404, "采购单不存在");
        }

        LambdaQueryWrapper<DeviceStockInOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceStockInOrder::getPurchaseOrderId, purchaseOrderId);
        wrapper.orderByDesc(DeviceStockInOrder::getCreateTime);

        List<DeviceStockInOrder> orders = stockInOrderMapper.selectList(wrapper);
        // 填充采购单号
        for (DeviceStockInOrder sio : orders) {
            sio.setPurchaseOrderNo(order.getOrderNo());
        }
        return orders;
    }

    /**
     * 更新库存台账
     */
    private void updateInventory(DevicePurchaseItem purchaseItem, int quantity, String warehouseName, Long tenantId) {
        LambdaQueryWrapper<DeviceInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceInventory::getItemType, purchaseItem.getItemType());
        wrapper.eq(DeviceInventory::getItemName, purchaseItem.getItemName());
        wrapper.eq(DeviceInventory::getModel, purchaseItem.getModel() != null ? purchaseItem.getModel() : "");
        wrapper.eq(DeviceInventory::getSpec, purchaseItem.getSpec() != null ? purchaseItem.getSpec() : "");
        wrapper.eq(DeviceInventory::getWarehouseName, warehouseName);
        wrapper.eq(DeviceInventory::getTenantId, tenantId);

        DeviceInventory inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            // 新增库存记录
            inventory = new DeviceInventory();
            inventory.setItemType(purchaseItem.getItemType());
            inventory.setItemName(purchaseItem.getItemName());
            inventory.setBrand(purchaseItem.getBrand());
            inventory.setModel(purchaseItem.getModel());
            inventory.setSpec(purchaseItem.getSpec());
            inventory.setUnit(purchaseItem.getUnit());
            inventory.setWarehouseName(warehouseName);
            inventory.setTotalQty(quantity);
            inventory.setStockedInQty(quantity);
            inventory.setStockedOutQty(0);
            inventory.setTenantId(tenantId);
            inventoryMapper.insert(inventory);
        } else {
            // 更新库存数量
            inventory.setTotalQty(inventory.getTotalQty() + quantity);
            inventory.setStockedInQty(inventory.getStockedInQty() + quantity);
            inventoryMapper.updateById(inventory);
        }
    }

    /**
     * 生成采购合同编号：CG + yyyyMMdd + 4位流水号
     */
    private String generatePurchaseOrderNo() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "CG" + datePart + randomPart;
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
        String categoryCode = "01"; // 默认分类码
        String monthPart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return "DEV" + categoryCode + monthPart + randomPart;
    }
}

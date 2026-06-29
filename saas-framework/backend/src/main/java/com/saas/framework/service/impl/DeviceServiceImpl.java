package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.DeviceInstallRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.*;
import com.saas.framework.mapper.*;
import com.saas.framework.service.DeviceService;
import com.saas.framework.service.DeviceTimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DeviceTimelineService deviceTimelineService;

    @Resource
    private DevicePurchaseOrderMapper purchaseOrderMapper;

    @Resource
    private DeviceStockInOrderMapper stockInOrderMapper;

    @Resource
    private DeviceStockOutOrderMapper stockOutOrderMapper;

    @Resource
    private BizRepairOrderMapper repairOrderMapper;

    @Override
    public PageResult<Device> page(int page, int size, String deviceCode, String deviceName,
                                    Integer status, String warehouseName, String installLocation) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(deviceCode)) {
            wrapper.like(Device::getDeviceCode, deviceCode);
        }
        if (StringUtils.hasText(deviceName)) {
            wrapper.like(Device::getDeviceName, deviceName);
        }
        if (status != null) {
            wrapper.eq(Device::getStatus, status);
        }
        if (StringUtils.hasText(warehouseName)) {
            wrapper.like(Device::getWarehouseName, warehouseName);
        }
        if (StringUtils.hasText(installLocation)) {
            wrapper.like(Device::getInstallLocation, installLocation);
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(Device::getTenantId, UserContext.getTenantId());
        }

        wrapper.orderByDesc(Device::getCreateTime);

        IPage<Device> iPage = deviceMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(iPage);
    }

    @Override
    public Device detail(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(404, "设备不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(device.getTenantId())) {
            throw new BusinessException(403, "无权查看其他租户的设备数据");
        }

        // 加载采购单号
        if (device.getPurchaseOrderId() != null) {
            DevicePurchaseOrder purchaseOrder = purchaseOrderMapper.selectById(device.getPurchaseOrderId());
            if (purchaseOrder != null) {
                device.setPurchaseOrderNo(purchaseOrder.getOrderNo());
            }
        }

        // 加载入库单号
        if (device.getStockInOrderId() != null) {
            DeviceStockInOrder stockInOrder = stockInOrderMapper.selectById(device.getStockInOrderId());
            if (stockInOrder != null) {
                device.setStockInOrderNo(stockInOrder.getOrderNo());
            }
        }

        // 加载出库单号
        if (device.getStockOutOrderId() != null) {
            DeviceStockOutOrder stockOutOrder = stockOutOrderMapper.selectById(device.getStockOutOrderId());
            if (stockOutOrder != null) {
                device.setStockOutOrderNo(stockOutOrder.getOrderNo());
            }
        }

        // 加载维修工单列表
        LambdaQueryWrapper<BizRepairOrder> repairWrapper = new LambdaQueryWrapper<>();
        repairWrapper.eq(BizRepairOrder::getDeviceId, device.getId());
        repairWrapper.orderByDesc(BizRepairOrder::getCreateTime);
        List<BizRepairOrder> repairOrders = repairOrderMapper.selectList(repairWrapper);
        device.setRepairOrders(repairOrders);

        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void install(Long id, DeviceInstallRequest request) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(404, "设备不存在");
        }
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(device.getTenantId())) {
            throw new BusinessException(403, "无权操作其他租户的设备数据");
        }
        if (device.getStatus() == null || device.getStatus() != 2) {
            throw new BusinessException("只有状态为在用的设备才能填写安装信息");
        }

        // 更新安装信息
        device.setInstallLocation(request.getInstallLocation());
        device.setInstallDate(request.getInstallDate());
        device.setInstallPerson(request.getInstallPerson());
        device.setUseDate(request.getUseDate());
        device.setAcceptRecord(request.getAcceptRecord());
        device.setInstallFile(request.getInstallFile());
        device.setAcceptPhoto(request.getAcceptPhoto());
        deviceMapper.updateById(device);

        // 记录时间线事件
        String eventDesc = "设备安装投用";
        if (StringUtils.hasText(request.getInstallLocation())) {
            eventDesc += "，安装位置：" + request.getInstallLocation();
        }
        if (request.getInstallDate() != null) {
            eventDesc += "，安装日期：" + request.getInstallDate();
        }
        deviceTimelineService.recordEvent(device.getId(), 4, "设备安装投用",
                eventDesc, null, null, UserContext.getUsername(), device.getTenantId());

        log.info("设备安装投用: deviceId={}, installLocation={}", id, request.getInstallLocation());
    }

    @Override
    public List<DeviceTimeline> getTimeline(Long deviceId) {
        return deviceTimelineService.getDeviceTimeline(deviceId);
    }

    @Override
    public List<Device> searchByCode(String keyword) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Device::getDeviceCode, keyword)
                    .or()
                    .like(Device::getDeviceName, keyword));
        }

        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(Device::getTenantId, UserContext.getTenantId());
        }

        wrapper.last("LIMIT 20");
        return deviceMapper.selectList(wrapper);
    }
}

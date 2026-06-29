package com.saas.framework.service;

import com.saas.framework.common.dto.DeviceInstallRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.Device;
import com.saas.framework.entity.DeviceTimeline;

import java.util.List;

public interface DeviceService {

    PageResult<Device> page(int page, int size, String deviceCode, String deviceName,
                            Integer status, String warehouseName, String installLocation);

    Device detail(Long id);

    void install(Long id, DeviceInstallRequest request);

    List<DeviceTimeline> getTimeline(Long deviceId);

    List<Device> searchByCode(String keyword);
}

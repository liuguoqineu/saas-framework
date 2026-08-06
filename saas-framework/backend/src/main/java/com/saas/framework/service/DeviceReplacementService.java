package com.saas.framework.service;

import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.RepairProcessWithReplacementRequest;
import com.saas.framework.entity.DeviceReplacement;

public interface DeviceReplacementService {

    PageResult<DeviceReplacement> page(int page, int size, String replacementNo, Integer replacementType, Long repairOrderId, String replaceTimeStart, String replaceTimeEnd);

    DeviceReplacement detail(Long id);

    DeviceReplacement createReplacement(RepairProcessWithReplacementRequest request, Long repairOrderId, Long tenantId);
}

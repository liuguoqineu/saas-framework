package com.saas.framework.service;

import com.saas.framework.entity.DeviceTimeline;
import java.util.List;

public interface DeviceTimelineService {
    /**
     * 记录设备履历事件
     * @param deviceId 设备ID
     * @param eventType 事件类型：1-采购，2-入库，3-出库，4-安装，5-报修，6-维修，7-配件更换，8-整机更换，9-报废
     * @param eventTitle 事件标题
     * @param eventDesc 事件描述
     * @param relatedId 关联业务ID
     * @param relatedOrderNo 关联单号
     * @param operator 操作人
     * @param tenantId 租户ID
     */
    void recordEvent(Long deviceId, Integer eventType, String eventTitle, String eventDesc,
                     Long relatedId, String relatedOrderNo, String operator, Long tenantId);

    /**
     * 查询设备履历时间线
     */
    List<DeviceTimeline> getDeviceTimeline(Long deviceId);
}

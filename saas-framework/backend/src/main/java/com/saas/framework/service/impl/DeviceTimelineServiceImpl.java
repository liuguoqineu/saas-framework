package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.entity.DeviceTimeline;
import com.saas.framework.mapper.DeviceTimelineMapper;
import com.saas.framework.service.DeviceTimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DeviceTimelineServiceImpl implements DeviceTimelineService {

    @Resource
    private DeviceTimelineMapper timelineMapper;

    @Override
    public void recordEvent(Long deviceId, Integer eventType, String eventTitle, String eventDesc,
                            Long relatedId, String relatedOrderNo, String operator, Long tenantId) {
        DeviceTimeline timeline = new DeviceTimeline();
        timeline.setDeviceId(deviceId);
        timeline.setEventType(eventType);
        timeline.setEventTime(LocalDateTime.now());
        timeline.setEventTitle(eventTitle);
        timeline.setEventDesc(eventDesc);
        timeline.setRelatedId(relatedId);
        timeline.setRelatedOrderNo(relatedOrderNo);
        timeline.setOperator(operator);
        timeline.setTenantId(tenantId);
        timelineMapper.insert(timeline);
        log.info("记录设备履历: deviceId={}, eventType={}, eventTitle={}", deviceId, eventType, eventTitle);
    }

    @Override
    public List<DeviceTimeline> getDeviceTimeline(Long deviceId) {
        LambdaQueryWrapper<DeviceTimeline> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceTimeline::getDeviceId, deviceId);
        wrapper.orderByDesc(DeviceTimeline::getEventTime);
        return timelineMapper.selectList(wrapper);
    }
}

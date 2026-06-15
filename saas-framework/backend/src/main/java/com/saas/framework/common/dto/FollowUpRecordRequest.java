package com.saas.framework.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class FollowUpRecordRequest {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotNull(message = "跟进时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followUpTime;

    private Long followUpPersonId;

    private String followUpPerson;

    @NotNull(message = "跟进方式不能为空")
    private Integer followUpMethod;

    @NotNull(message = "跟进内容不能为空")
    private String followUpContent;

    private String nextPlan;

    @NotNull(message = "跟进状态不能为空")
    private Integer followUpStatus;

    /**
     * 可选：新合作状态（正常履约/终止合作/高潜力/中潜力/低潜力/无效客户）
     * 如果需要同步变更客户合作状态，请同时提供 newCooperationStatus 和 changeReason
     */
    private String newCooperationStatus;

    /**
     * 可选：状态变更原因
     * 当提供了 newCooperationStatus 时，此字段必填
     */
    private String changeReason;
}

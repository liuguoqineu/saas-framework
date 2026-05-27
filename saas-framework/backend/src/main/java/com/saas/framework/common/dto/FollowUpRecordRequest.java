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
}

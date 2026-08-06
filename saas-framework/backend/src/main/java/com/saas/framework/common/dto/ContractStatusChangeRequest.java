package com.saas.framework.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ContractStatusChangeRequest {

    @NotBlank(message = "新合同状态不能为空")
    private String newStatus;

    private String changeReason;
}

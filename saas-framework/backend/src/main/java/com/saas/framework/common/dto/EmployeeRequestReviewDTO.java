package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 超级管理员审核员工申请 DTO
 */
@Data
public class EmployeeRequestReviewDTO {

    /** 审核动作：APPROVED-通过，REJECTED-拒绝 */
    @NotBlank(message = "审核动作不能为空")
    private String action;

    /** 审核意见 */
    private String reviewComment;
}

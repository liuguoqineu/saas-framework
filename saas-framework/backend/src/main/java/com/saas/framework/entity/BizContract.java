package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_contract")
public class BizContract {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String contractNo;
    private Long customerId;
    private String customerName;
    private LocalDate signDate;
    private LocalDate expireDate;
    private BigDecimal contractAmount;
    private String serviceContent;
    private String paymentMethod;
    private Long personInChargeId;
    private String personInCharge;
    private String contractStatus;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

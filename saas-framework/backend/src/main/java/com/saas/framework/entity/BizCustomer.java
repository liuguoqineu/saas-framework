package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户信息表 (biz_customer)
 * 支持多租户隔离，包含客户基本信息、业务属性和合作状态
 *
 * 业务类型两级分类：
 *   一级(businessCategory): 加气站类/商业用气/工业用气
 *   二级(businessType): CNG加气站/LPG加气站/餐饮类/团餐类/其他商业类/大型/中型/小型
 *
 * 合作状态两级分类：
 *   一级(cooperationCategory): 已合作/潜在/无效
 *   二级(cooperationStatus): 正常履约/终止合作/高潜力/中潜力/低潜力/无效客户
 */
@Data
@TableName("biz_customer")
public class BizCustomer {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户名称 */
    private String name;

    /** 客户地址 */
    private String address;

    /** 所属区域 */
    private String region;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 业务类型一级分类: 加气站类/商业用气/工业用气 */
    private String businessCategory;

    /** 业务类型二级分类: CNG加气站/LPG加气站/餐饮类/团餐类/其他商业类/大型/中型/小型 */
    private String businessType;

    /** 合作状态一级分类: 已合作/潜在/意向 */
    private String cooperationCategory;

    /** 合作状态二级分类: 正常履约/逾期客户/暂停合作/终止合作/高潜力/中潜力/低潜力/意向跟进 */
    private String cooperationStatus;

    /** 用气规模（工业客户辅助分类: 大型/中型/小型） */
    private String gasScale;

    /** 智慧燃气系统型号/部署情况 */
    private String smartGasSystem;

    /** 运维需求分类: 高频报修/常规运维/无报修 */
    private String maintenanceCategory;

    /** 合同信息摘要 */
    private String contractInfo;

    /** 合同到期时间（从合同管理模块关联，非数据库字段） */
    @TableField(exist = false)
    private LocalDate contractExpireDate;

    /** 是否无效: 0-正常, 1-无效（多次对接无回应、无需求或不符合服务范围） */
    private Integer isInvalid;

    /** 当前跟进人ID */
    private Long followUpPersonId;

    /** 当前跟进人姓名 */
    private String followUpPerson;

    /** 租户ID，用于数据隔离 */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}

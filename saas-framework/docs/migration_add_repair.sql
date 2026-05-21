-- ============================================================
-- 报修管理模块 - 数据库迁移脚本
-- ============================================================

USE saaslearn;

-- 报修单表
DROP TABLE IF EXISTS biz_repair_order;
CREATE TABLE biz_repair_order (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    repair_no           VARCHAR(50)     NOT NULL                 COMMENT '报修单号，自动生成',
    customer_id         BIGINT          DEFAULT NULL             COMMENT '关联客户ID',
    customer_name       VARCHAR(100)    DEFAULT NULL             COMMENT '客户名称',
    contact_person      VARCHAR(50)     DEFAULT NULL             COMMENT '联系人',
    contact_phone       VARCHAR(20)     DEFAULT NULL             COMMENT '联系电话',
    repair_content      VARCHAR(500)    NOT NULL                 COMMENT '报修内容',
    repair_type         VARCHAR(50)     DEFAULT NULL             COMMENT '报修类型: 智慧燃气系统故障/设备问题/管道泄漏/仪表故障/其他',
    repair_time         DATETIME        DEFAULT NULL             COMMENT '报修时间',
    repair_address      VARCHAR(255)    DEFAULT NULL             COMMENT '报修地点',
    urgency             VARCHAR(20)     NOT NULL DEFAULT '普通'  COMMENT '紧急程度: 普通/紧急',
    status              VARCHAR(20)     NOT NULL DEFAULT '未处理' COMMENT '报修状态: 未处理/处理中/已解决/无法解决',
    fault_description   VARCHAR(1000)   DEFAULT NULL             COMMENT '故障描述细化',
    assignee_id         BIGINT          DEFAULT NULL             COMMENT '分配的运维人员ID',
    assignee_name       VARCHAR(50)     DEFAULT NULL             COMMENT '分配的运维人员姓名',
    assign_time         DATETIME        DEFAULT NULL             COMMENT '分配时间',
    assigner_id         BIGINT          DEFAULT NULL             COMMENT '分配人ID',
    assigner_name       VARCHAR(50)     DEFAULT NULL             COMMENT '分配人姓名',
    process_time        DATETIME        DEFAULT NULL             COMMENT '处理时间',
    process_method      VARCHAR(500)    DEFAULT NULL             COMMENT '处理方式',
    replaced_parts      VARCHAR(500)    DEFAULT NULL             COMMENT '更换配件',
    fault_reason        VARCHAR(500)    DEFAULT NULL             COMMENT '故障原因',
    confirm_status      TINYINT(1)      DEFAULT 0                COMMENT '确认状态: 0-未确认 1-已确认',
    confirm_time        DATETIME        DEFAULT NULL             COMMENT '确认时间',
    confirm_person      VARCHAR(50)     DEFAULT NULL             COMMENT '确认人',
    is_exception        TINYINT(1)      DEFAULT 0                COMMENT '是否异常: 0-否 1-是',
    exception_reason    VARCHAR(500)    DEFAULT NULL             COMMENT '异常原因',
    second_plan         VARCHAR(500)    DEFAULT NULL             COMMENT '二次处理计划',
    second_remind_time  DATETIME        DEFAULT NULL             COMMENT '二次处理提醒时间',
    creator_id          BIGINT          DEFAULT NULL             COMMENT '录入人ID',
    creator_name        VARCHAR(50)     DEFAULT NULL             COMMENT '录入人姓名',
    tenant_id           BIGINT          NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_repair_no (repair_no),
    INDEX idx_customer_id (customer_id),
    INDEX idx_customer_name (customer_name),
    INDEX idx_status (status),
    INDEX idx_urgency (urgency),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_repair_time (repair_time),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_is_exception (is_exception),
    INDEX idx_confirm_status (confirm_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修单表';

-- 报修附件表
DROP TABLE IF EXISTS biz_repair_attachment;
CREATE TABLE biz_repair_attachment (
    id             BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    repair_id      BIGINT       NOT NULL                 COMMENT '关联报修单ID',
    file_name      VARCHAR(200) NOT NULL                 COMMENT '文件名',
    file_path      VARCHAR(500) NOT NULL                 COMMENT '文件存储路径',
    file_type      VARCHAR(50)  DEFAULT NULL             COMMENT '文件类型: 现场照片/故障截图/处理照片/其他',
    file_size      BIGINT       DEFAULT NULL             COMMENT '文件大小(字节)',
    tenant_id      BIGINT       NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_repair_id (repair_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修附件表';

-- 报修处理记录表
DROP TABLE IF EXISTS biz_repair_process_log;
CREATE TABLE biz_repair_process_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    repair_id       BIGINT       NOT NULL                 COMMENT '关联报修单ID',
    action          VARCHAR(50)  NOT NULL                 COMMENT '操作类型: 录入/补充/分配/进度更新/确认/异常标记',
    old_status      VARCHAR(20)  DEFAULT NULL             COMMENT '操作前状态',
    new_status      VARCHAR(20)  DEFAULT NULL             COMMENT '操作后状态',
    content         VARCHAR(1000) DEFAULT NULL            COMMENT '操作内容',
    operator_id     BIGINT       DEFAULT NULL             COMMENT '操作人ID',
    operator_name   VARCHAR(50)  DEFAULT NULL             COMMENT '操作人姓名',
    operate_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    tenant_id       BIGINT       NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_repair_id (repair_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修处理记录表';



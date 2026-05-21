-- ============================================================
-- 合同管理模块 - 数据库迁移脚本
-- 包含: 合同表、合同附件表、合同修改记录表、合同到期提醒表
-- 使用方式: 在 MySQL 中执行 source docs/migration_add_contract.sql
-- ============================================================

USE saaslearn;

-- 合同信息表
CREATE TABLE IF NOT EXISTS biz_contract (
    id                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    contract_no       VARCHAR(50)     NOT NULL                 COMMENT '合同编号',
    customer_id       BIGINT          NOT NULL                 COMMENT '关联客户ID',
    customer_name     VARCHAR(100)    NOT NULL                 COMMENT '客户名称（冗余，方便查询）',
    contract_type     VARCHAR(50)     DEFAULT NULL             COMMENT '合同类型: 智慧燃气系统部署/运维服务/售后服务/综合服务/其他',
    sign_date         DATE            DEFAULT NULL             COMMENT '签订日期',
    expire_date       DATE            DEFAULT NULL             COMMENT '到期日期',
    contract_amount   DECIMAL(12,2)   DEFAULT NULL             COMMENT '合同金额（元）',
    service_content   VARCHAR(500)    DEFAULT NULL             COMMENT '服务内容: 智慧燃气系统部署、运维、售后等',
    payment_method    VARCHAR(50)     DEFAULT NULL             COMMENT '付款方式: 一次性付款/分期付款/按季度付款/按年度付款/其他',
    person_in_charge_id BIGINT        DEFAULT NULL             COMMENT '负责人ID',
    person_in_charge  VARCHAR(50)     DEFAULT NULL             COMMENT '负责人姓名',
    contract_status   VARCHAR(20)     NOT NULL DEFAULT '未生效' COMMENT '合同状态: 未生效/已生效/已到期/已终止',
    remark            VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    tenant_id         BIGINT          NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_no (contract_no, tenant_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_contract_status (contract_status),
    INDEX idx_expire_date (expire_date),
    INDEX idx_sign_date (sign_date),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_person_in_charge_id (person_in_charge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同信息表';

-- 合同附件表（合同扫描件等）
CREATE TABLE IF NOT EXISTS biz_contract_attachment (
    id             BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    contract_id    BIGINT       NOT NULL                 COMMENT '关联合同ID',
    file_name      VARCHAR(200) NOT NULL                 COMMENT '文件名',
    file_path      VARCHAR(500) NOT NULL                 COMMENT '文件存储路径',
    file_type      VARCHAR(50)  DEFAULT NULL             COMMENT '文件类型: 合同扫描件/附件/其他',
    file_size      BIGINT       DEFAULT NULL             COMMENT '文件大小(字节)',
    tenant_id      BIGINT       NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同附件表';

-- 合同修改记录表
CREATE TABLE IF NOT EXISTS biz_contract_modify_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    contract_id    BIGINT       NOT NULL                 COMMENT '关联合同ID',
    field_name     VARCHAR(50)  NOT NULL                 COMMENT '修改字段名',
    old_value      VARCHAR(500) DEFAULT NULL             COMMENT '修改前的值',
    new_value      VARCHAR(500) DEFAULT NULL             COMMENT '修改后的值',
    modify_user_id BIGINT       DEFAULT NULL             COMMENT '修改人ID',
    modify_user    VARCHAR(50)  DEFAULT NULL             COMMENT '修改人用户名',
    modify_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    tenant_id      BIGINT       NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同修改记录表';

-- 合同到期提醒表
CREATE TABLE IF NOT EXISTS biz_contract_reminder (
    id                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    contract_id       BIGINT       NOT NULL                 COMMENT '关联合同ID',
    contract_no       VARCHAR(50)  NOT NULL                 COMMENT '合同编号（冗余）',
    customer_name     VARCHAR(100) DEFAULT NULL             COMMENT '客户名称（冗余）',
    remind_days       INT          NOT NULL DEFAULT 30      COMMENT '提前提醒天数: 30/15/7等',
    remind_date       DATE         NOT NULL                 COMMENT '提醒日期（到期日期-提前天数）',
    person_in_charge_id BIGINT     DEFAULT NULL             COMMENT '负责人ID',
    person_in_charge  VARCHAR(50)  DEFAULT NULL             COMMENT '负责人姓名',
    is_read           TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否已读: 0-未读, 1-已读',
    is_handled        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否已处理: 0-未处理, 1-已处理',
    tenant_id         BIGINT       NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_remind_date (remind_date),
    INDEX idx_is_read (is_read),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同到期提醒表';

-- ============================================================
-- 合同管理权限数据
-- ============================================================

-- 一级菜单 - 合同管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(8, '合同管理', 'contract', 'menu', 5, 3);

-- 按钮权限 - 合同管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(71, '合同列表',   'contract:list',      'button', 8, 1),
(72, '新增合同',   'contract:add',       'button', 8, 2),
(73, '编辑合同',   'contract:edit',      'button', 8, 3),
(74, '删除合同',   'contract:delete',    'button', 8, 4),
(75, '合同状态变更', 'contract:status',   'button', 8, 5),
(76, '合同到期提醒', 'contract:remind',   'button', 8, 6);

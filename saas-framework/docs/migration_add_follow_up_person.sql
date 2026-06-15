-- 迁移脚本：为客户表添加跟进人字段 + 创建缺失的业务表
-- 执行时间：2026-05-14
-- 说明：
--   1. 在 biz_customer 表中添加 follow_up_person_id 和 follow_up_person 字段
--   2. 创建 biz_follow_up_record 表（跟进记录表）
--   3. 创建 biz_customer_status_log 表（客户状态变更记录表）

USE saaslearn;

-- ========================================
-- 1. 为客户表添加跟进人字段
-- ========================================

-- 添加跟进人ID字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'saaslearn' AND TABLE_NAME = 'biz_customer' AND COLUMN_NAME = 'follow_up_person_id');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE biz_customer ADD COLUMN follow_up_person_id BIGINT DEFAULT NULL COMMENT ''当前跟进人ID'' AFTER contract_info', 
    'SELECT ''follow_up_person_id column already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加跟进人姓名字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'saaslearn' AND TABLE_NAME = 'biz_customer' AND COLUMN_NAME = 'follow_up_person');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE biz_customer ADD COLUMN follow_up_person VARCHAR(50) DEFAULT NULL COMMENT ''当前跟进人姓名'' AFTER follow_up_person_id', 
    'SELECT ''follow_up_person column already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 2. 创建跟进记录表（如果不存在）
-- ========================================

CREATE TABLE IF NOT EXISTS biz_follow_up_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id         BIGINT          NOT NULL                 COMMENT '关联客户ID',
    follow_up_time      DATETIME        NOT NULL                 COMMENT '跟进时间',
    follow_up_person_id BIGINT          DEFAULT NULL             COMMENT '跟进人ID',
    follow_up_person    VARCHAR(50)     DEFAULT NULL             COMMENT '跟进人姓名',
    follow_up_method    INT             NOT NULL                 COMMENT '跟进方式: 1-电话 2-微信 3-邮件 4-上门拜访 5-其他',
    follow_up_content   VARCHAR(500)    NOT NULL                 COMMENT '跟进内容',
    next_plan           VARCHAR(200)    DEFAULT NULL             COMMENT '下一步计划',
    follow_up_status    INT             NOT NULL DEFAULT 1       COMMENT '跟进状态: 1-待跟进 2-已跟进 3-已达成意向',
    attachments         VARCHAR(2000)   DEFAULT NULL             COMMENT '附件信息JSON',
    tenant_id           BIGINT          NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL             COMMENT '创建人ID',
    deleted             TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_follow_up_person_id (follow_up_person_id),
    INDEX idx_follow_up_status (follow_up_status),
    INDEX idx_follow_up_time (follow_up_time),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进记录表';

-- ========================================
-- 3. 创建客户状态变更记录表（如果不存在）
-- ========================================

CREATE TABLE IF NOT EXISTS biz_customer_status_log (
    id                        BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id               BIGINT          NOT NULL                 COMMENT '客户ID',
    old_cooperation_category  VARCHAR(50)     DEFAULT NULL             COMMENT '原状态一级分类',
    old_cooperation_status    VARCHAR(50)     DEFAULT NULL             COMMENT '原状态二级分类',
    new_cooperation_category  VARCHAR(50)     DEFAULT NULL             COMMENT '新状态一级分类',
    new_cooperation_status    VARCHAR(50)     DEFAULT NULL             COMMENT '新状态二级分类',
    change_reason             VARCHAR(200)    DEFAULT NULL             COMMENT '变更原因',
    follow_up_record_id       BIGINT          DEFAULT NULL             COMMENT '关联跟进记录ID',
    change_person_id          BIGINT          DEFAULT NULL             COMMENT '变更人ID',
    change_person             VARCHAR(50)     DEFAULT NULL             COMMENT '变更人姓名',
    change_time               DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    tenant_id                 BIGINT          NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time               DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户状态变更记录表';

-- ========================================
-- 验证结果
-- ========================================

SELECT '=== 验证 biz_customer 新增字段 ===' AS info;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'saaslearn' 
AND TABLE_NAME = 'biz_customer' 
AND COLUMN_NAME IN ('follow_up_person_id', 'follow_up_person');

SELECT '=== 验证 biz_follow_up_record 表 ===' AS info;
SELECT TABLE_NAME, TABLE_COMMENT 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'saaslearn' 
AND TABLE_NAME = 'biz_follow_up_record';

SELECT '=== 验证 biz_customer_status_log 表 ===' AS info;
SELECT TABLE_NAME, TABLE_COMMENT 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'saaslearn' 
AND TABLE_NAME = 'biz_customer_status_log';

-- ============================================================
-- 日报/周报/月报系统 - 数据库迁移脚本
-- ============================================================

-- 幂等添加 sys_user 表字段（兼容重复执行）
SET @exist_post_type = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'post_type');
SET @exist_leader_id = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'leader_id');

SET @sql = '';
IF @exist_post_type = 0 THEN SET @sql = CONCAT(@sql, 'ADD COLUMN post_type VARCHAR(16) DEFAULT NULL COMMENT ''岗位类型: DEV-研发, OPS-运维, CS-客服'', ');
END IF;
IF @exist_leader_id = 0 THEN SET @sql = CONCAT(@sql, 'ADD COLUMN leader_id BIGINT DEFAULT NULL COMMENT ''直属领导ID''');
END IF;

IF @sql != '' THEN
    SET @sql = CONCAT('ALTER TABLE sys_user ', @sql);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END IF;

-- 允许 rp_report.template_id 为空（支持无模板直接填报）
SET @exist_nullable = (SELECT COLUMN_TYPE FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rp_report' AND COLUMN_NAME = 'template_id');
SET @is_not_null = (SELECT IS_NULLABLE FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rp_report' AND COLUMN_NAME = 'template_id');
IF @is_not_null = 'NO' THEN
    ALTER TABLE rp_report MODIFY template_id BIGINT DEFAULT NULL COMMENT '模板ID';
END IF;

CREATE TABLE IF NOT EXISTS rp_template (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    template_code   VARCHAR(32)     NOT NULL                 COMMENT '模板编码',
    template_name   VARCHAR(64)     NOT NULL                 COMMENT '模板名称',
    post_type       VARCHAR(16)     NOT NULL                 COMMENT '岗位类型: DEV/OPS/CS',
    report_type     VARCHAR(16)     NOT NULL                 COMMENT '报表类型: DAILY/WEEKLY/MONTHLY',
    template_desc   TEXT            DEFAULT NULL             COMMENT '模板说明/填写指引',
    is_enabled      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否启用',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表模板表';

CREATE TABLE IF NOT EXISTS rp_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT          NOT NULL                 COMMENT '填报人ID',
    dept_id         BIGINT          DEFAULT NULL             COMMENT '所属部门ID',
    template_id     BIGINT          DEFAULT NULL             COMMENT '模板ID，允许为空支持无模板填报',
    report_type     VARCHAR(16)     NOT NULL                 COMMENT '报表类型',
    report_period   VARCHAR(32)     NOT NULL                 COMMENT '报表周期标识',
    content_text    TEXT            DEFAULT NULL             COMMENT '填报内容(纯文本)',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/SUBMITTED/APPROVED/REJECTED',
    submit_time     DATETIME        DEFAULT NULL             COMMENT '提交时间',
    tenant_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '租户ID',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_user_period (user_id, report_period),
    INDEX idx_dept_status (dept_id, status),
    INDEX idx_period_type (report_period, report_type),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表记录表';

CREATE TABLE IF NOT EXISTS rp_approval (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    report_id       BIGINT          NOT NULL                 COMMENT '报表ID',
    approver_id     BIGINT          NOT NULL                 COMMENT '审批人ID',
    approval_level  INT             NOT NULL DEFAULT 1       COMMENT '审批层级',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
    comment         VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
    approve_time    DATETIME        DEFAULT NULL             COMMENT '审批时间',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_report (report_id),
    INDEX idx_approver_status (approver_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

CREATE TABLE IF NOT EXISTS rp_report_revision (
    id                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    report_id         BIGINT          NOT NULL                 COMMENT '报表ID',
    revision_type     VARCHAR(16)     NOT NULL                 COMMENT '修改类型',
    content_snapshot  TEXT            DEFAULT NULL             COMMENT '内容快照(纯文本)',
    operator_id       BIGINT          NOT NULL                 COMMENT '操作人ID',
    create_time       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表修改记录表';

CREATE TABLE IF NOT EXISTS rp_overdue (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    report_type     VARCHAR(16)     NOT NULL                 COMMENT '报表类型',
    report_period   VARCHAR(32)     NOT NULL                 COMMENT '应填报周期',
    deadline        DATETIME        NOT NULL                 COMMENT '截止时间',
    is_reminded     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已提醒',
    tenant_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '租户ID',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_type_period (user_id, report_type, report_period),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逾期记录表';

CREATE TABLE IF NOT EXISTS rp_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    config_key      VARCHAR(64)     NOT NULL                 COMMENT '配置键',
    config_value    VARCHAR(512)    NOT NULL                 COMMENT '配置值',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '配置说明',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表系统配置表';

INSERT IGNORE INTO rp_config (config_key, config_value, description) VALUES
('daily_deadline', '18:00', '日报截止时间'),
('weekly_start_day', '1', '周报起始日'),
('weekly_deadline_day', '5', '周报截止日'),
('weekly_deadline_time', '18:00', '周报截止时间'),
('monthly_deadline_day', '3', '月报截止日'),
('overdue_remind_hours', '2', '逾期提醒间隔'),
('approval_timeout_hours', '24', '审批超时阈值');

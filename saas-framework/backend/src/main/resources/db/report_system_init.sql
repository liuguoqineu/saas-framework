-- ============================================
-- 日报/周报/月报系统 - 数据库建表脚本
-- 执行时间：2026-05-28
-- 说明：本脚本包含报表系统初始化数据
-- ============================================

-- 1. 初始化报表配置数据（如果不存在）
INSERT IGNORE INTO `rp_config` (`config_key`, `config_value`, `description`) VALUES
('daily_deadline', '18:00', '日报截止时间'),
('weekly_start_day', '1', '周报起始日（1=周一）'),
('weekly_deadline_day', '5', '周报截止日（5=周五）'),
('weekly_deadline_time', '18:00', '周报截止时间'),
('monthly_deadline_day', '3', '月报截止日（次月第N天）'),
('monthly_deadline_time', '18:00', '月报截止时间'),
('approval_timeout_hours', '24', '审批超时阈值（小时）'),
('approval_max_level', '2', '最大审批层级'),
('overdue_remind_interval_hours', '2', '逾期后每N小时提醒一次'),
('overdue_max_remind_count', '3', '最大提醒次数'),
('export_max_batch_size', '500', '单次批量导出上限'),
('archive_retention_months', '36', '归档保留月数');

-- 2. 确保 rp_template 表有 template_desc 列（兼容旧版本）
SET @exist_template_desc = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rp_template' AND COLUMN_NAME = 'template_desc');
SET @sql_add_col = '';
IF @exist_template_desc = 0 THEN
    SET @sql_add_col = 'ALTER TABLE rp_template ADD COLUMN template_desc TEXT DEFAULT NULL COMMENT ''模板说明/填写指引'' AFTER report_type';
    PREPARE stmt FROM @sql_add_col;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END IF;

-- 3. 确保 rp_report 表的 template_id 字段允许 NULL（支持无模板填报）
SET @exist_template_id_nullable = (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rp_report' AND COLUMN_NAME = 'template_id');
IF @exist_template_id_nullable = 'NO' THEN
    ALTER TABLE rp_report MODIFY COLUMN template_id BIGINT DEFAULT NULL COMMENT '模板ID，允许为空支持无模板填报';
END IF;

-- 4. 初始化9套模板数据（研发/运维/客服 × 日报/周报/月报）
INSERT IGNORE INTO `rp_template` (`template_code`, `template_name`, `post_type`, `report_type`, `template_desc`, `is_enabled`) VALUES
-- 研发岗位模板
('DEV_DAILY', '研发日报', 'DEV', 'DAILY', '研发人员每日工作汇报模板', 1),
('DEV_WEEKLY', '研发周报', 'DEV', 'WEEKLY', '研发人员每周工作汇总模板', 1),
('DEV_MONTHLY', '研发月报', 'DEV', 'MONTHLY', '研发人员每月工作总结模板', 1),

-- 运维岗位模板
('OPS_DAILY', '运维日报', 'OPS', 'DAILY', '运维人员每日工作汇报模板', 1),
('OPS_WEEKLY', '运维周报', 'OPS', 'WEEKLY', '运维人员每周工作汇总模板', 1),
('OPS_MONTHLY', '运维月报', 'OPS', 'MONTHLY', '运维人员每月工作总结模板', 1),

-- 客服岗位模板
('CS_DAILY', '客服日报', 'CS', 'DAILY', '客服人员每日工作汇报模板', 1),
('CS_WEEKLY', '客服周报', 'CS', 'WEEKLY', '客服人员每周工作汇总模板', 1),
('CS_MONTHLY', '客服月报', 'CS', 'MONTHLY', '客服人员每月工作总结模板', 1);

-- 5. 注意：以下索引已在 migration_add_report.sql 的建表语句中创建，无需重复创建
-- rp_report 表索引: idx_user_period, idx_dept_status, idx_period_type, idx_tenant_id
-- rp_approval 表索引: idx_report, idx_approver_status
-- rp_overdue 表索引: idx_user_type_period, idx_tenant_id

-- ============================================
-- 执行完成提示
-- ============================================
SELECT
    '数据库初始化完成' AS message,
    (SELECT COUNT(*) FROM rp_template) AS template_count,
    (SELECT COUNT(*) FROM rp_config) AS config_count;

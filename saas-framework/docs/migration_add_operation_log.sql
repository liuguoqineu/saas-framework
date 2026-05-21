-- ============================================================
-- 权限管理模块迁移脚本
-- 1. 操作日志表 sys_operation_log
-- 2. 拜访管理权限
-- 3. 财务管理权限
-- 4. 操作日志权限
-- ============================================================

USE saaslearn;

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT          DEFAULT NULL             COMMENT '操作人ID',
    username        VARCHAR(50)     DEFAULT NULL             COMMENT '操作人用户名',
    real_name       VARCHAR(50)     DEFAULT NULL             COMMENT '操作人真实姓名',
    operation       VARCHAR(50)     NOT NULL                 COMMENT '操作类型: CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT/LOGIN/OTHER',
    module          VARCHAR(50)     DEFAULT NULL             COMMENT '操作模块: 角色/员工/客户/合同/报修/拜访/统计/租户/权限',
    description     VARCHAR(500)    DEFAULT NULL             COMMENT '操作描述',
    method          VARCHAR(200)    DEFAULT NULL             COMMENT '请求方法',
    request_url     VARCHAR(500)    DEFAULT NULL             COMMENT '请求URL',
    request_params  TEXT            DEFAULT NULL             COMMENT '请求参数',
    ip              VARCHAR(50)     DEFAULT NULL             COMMENT '操作IP',
    tenant_id       BIGINT          DEFAULT NULL             COMMENT '租户ID',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_operation (operation),
    INDEX idx_module (module),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 拜访管理权限
INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('拜访管理', 'visit', 'menu', (SELECT id FROM (SELECT id FROM sys_permission WHERE code = 'business') t), 6);

SET @visit_menu_id = (SELECT id FROM sys_permission WHERE code = 'visit');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('拜访列表',   'visit:list',    'button', @visit_menu_id, 1),
('新增拜访',   'visit:add',     'button', @visit_menu_id, 2),
('编辑拜访',   'visit:edit',    'button', @visit_menu_id, 3),
('删除拜访',   'visit:delete',  'button', @visit_menu_id, 4),
('导出拜访',   'visit:export',  'button', @visit_menu_id, 5);

-- 财务管理权限
INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('财务管理', 'finance', 'menu', (SELECT id FROM (SELECT id FROM sys_permission WHERE code = 'business') t), 7);

SET @finance_menu_id = (SELECT id FROM sys_permission WHERE code = 'finance');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('合同金额查看', 'finance:contract',  'button', @finance_menu_id, 1),
('费用查看',     'finance:expense',   'button', @finance_menu_id, 2);

-- 操作日志权限（挂在系统管理下）
INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('操作日志',   'log',       'menu',   (SELECT id FROM (SELECT id FROM sys_permission WHERE code = 'system') t), 4);

SET @log_menu_id = (SELECT id FROM sys_permission WHERE code = 'log');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('日志列表',   'log:list',    'button', @log_menu_id, 1),
('导出日志',   'log:export',  'button', @log_menu_id, 2);

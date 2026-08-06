-- ============================================================
-- SaaS 多租户教学框架 - 数据库初始化脚本
-- 数据库: saaslearn
-- 字符集: utf8mb4, 引擎: InnoDB
-- 使用方式: 在 MySQL 中执行 source docs/init.sql 或用 Navicat 等工具导入
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS saaslearn
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE saaslearn;

-- ============================================================
-- 系统表 (sys_)
-- ============================================================

-- 权限表（无租户隔离，全局共享）
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name        VARCHAR(50)     NOT NULL                 COMMENT '权限名称',
    code        VARCHAR(100)    NOT NULL                 COMMENT '权限编码，如 student:list',
    type        VARCHAR(20)     NOT NULL DEFAULT 'menu'  COMMENT '权限类型: menu-菜单, button-按钮',
    parent_id   BIGINT          DEFAULT 0                COMMENT '父权限ID，0为根节点',
    sort        INT             DEFAULT 0                COMMENT '排序号',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name        VARCHAR(50)     NOT NULL                 COMMENT '角色名称',
    tenant_id   BIGINT          NOT NULL DEFAULT 0       COMMENT '租户ID，0为平台角色',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 角色-权限关联表（无租户隔离）
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id         BIGINT  NOT NULL COMMENT '角色ID',
    permission_id   BIGINT  NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username    VARCHAR(50)     NOT NULL                 COMMENT '用户名，登录用',
    password    VARCHAR(200)    NOT NULL                 COMMENT '密码，BCrypt加密存储',
    role_id     BIGINT          DEFAULT NULL             COMMENT '角色ID，关联sys_role.id',
    tenant_id   BIGINT          NOT NULL DEFAULT 0       COMMENT '租户ID，超级账户为0',
    real_name   VARCHAR(50)     DEFAULT NULL             COMMENT '真实姓名',
    status      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态: 1-启用, 0-禁用',
    post_type   VARCHAR(50)     DEFAULT NULL             COMMENT '岗位类型: DEV-研发, OPS-运维, CS-客服',
    leader_id   BIGINT          DEFAULT NULL             COMMENT '领导ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 租户信息表
DROP TABLE IF EXISTS sys_tenant;
CREATE TABLE sys_tenant (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID，即租户ID',
    name            VARCHAR(100)    NOT NULL                 COMMENT '租户名称（公司名称）',
    code            VARCHAR(50)     NOT NULL                 COMMENT '租户编码，唯一标识',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态: 1-启用, 0-禁用',
    admin_user_id   BIGINT          DEFAULT NULL             COMMENT '关联的管理员用户ID',
    admin_password  VARCHAR(50)     DEFAULT NULL             COMMENT '管理员初始密码',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户信息表';

-- ============================================================
-- 业务表 (biz_)
-- ============================================================

-- 客户信息表
DROP TABLE IF EXISTS biz_customer;
CREATE TABLE biz_customer (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name                VARCHAR(100)    NOT NULL                 COMMENT '客户名称',
    address             VARCHAR(255)    DEFAULT NULL             COMMENT '客户地址',
    region              VARCHAR(50)     DEFAULT NULL             COMMENT '所属区域',
    contact_person      VARCHAR(50)     DEFAULT NULL             COMMENT '联系人',
    contact_phone       VARCHAR(20)     DEFAULT NULL             COMMENT '联系电话',
    business_category   VARCHAR(50)     DEFAULT NULL             COMMENT '业务类型一级分类: 加气站类/商业用气/民业用气',
    business_type       VARCHAR(50)     DEFAULT NULL             COMMENT '业务类型二级分类: CNG加气站/LPG加气站/餐饮类/团餐类/其他商业类/大型/中型/小型',
    cooperation_status  VARCHAR(50)     DEFAULT '中潜力'          COMMENT '合作状态: 正常履约/终止合作/高潜力/中潜力/低潜力/无效客户',
    gas_scale           VARCHAR(50)     DEFAULT NULL             COMMENT '用气规模（工业客户辅助分类: 大型/中型/小型）',
    smart_gas_system    VARCHAR(255)    DEFAULT NULL             COMMENT '智慧燃气系统型号/部署情况',
    contract_info       VARCHAR(500)    DEFAULT NULL             COMMENT '合同信息摘要',
    is_invalid          TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否无效: 0-正常, 1-无效（冗余字段，业务逻辑统一使用 cooperation_status="无效客户" 判断）',
    follow_up_person_id BIGINT          DEFAULT NULL             COMMENT '当前跟进人ID',
    follow_up_person    VARCHAR(50)     DEFAULT NULL             COMMENT '当前跟进人姓名',
    tenant_id           BIGINT          NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_business_category (business_category),
    INDEX idx_business_type (business_type),
    INDEX idx_cooperation_status (cooperation_status),
    INDEX idx_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表';

-- 客户附件表
DROP TABLE IF EXISTS biz_customer_attachment;
CREATE TABLE biz_customer_attachment (
    id             BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id    BIGINT       NOT NULL                 COMMENT '关联客户ID',
    file_name      VARCHAR(200) NOT NULL                 COMMENT '文件名',
    file_path      VARCHAR(500) NOT NULL                 COMMENT '文件存储路径',
    file_type      VARCHAR(50)  DEFAULT NULL             COMMENT '文件类型: 合同扫描件/资质文件/现场照片/其他',
    file_size      BIGINT       DEFAULT NULL             COMMENT '文件大小(字节)',
    tenant_id      BIGINT       NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户附件表';

-- 客户修改记录表（SaaS业务表标准格式：含 tenant_id + deleted 逻辑删除标记）
DROP TABLE IF EXISTS biz_customer_modify_log;
CREATE TABLE biz_customer_modify_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id    BIGINT       NOT NULL                 COMMENT '关联客户ID',
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
    INDEX idx_customer_id (customer_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户修改记录表';

-- 跟进记录表
DROP TABLE IF EXISTS biz_follow_up_record;
CREATE TABLE biz_follow_up_record (
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

-- 客户状态变更记录表
DROP TABLE IF EXISTS biz_customer_status_log;
CREATE TABLE biz_customer_status_log (
    id                        BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    customer_id               BIGINT          NOT NULL                 COMMENT '客户ID',
    old_cooperation_status    VARCHAR(50)     DEFAULT NULL             COMMENT '原合作状态',
    new_cooperation_status    VARCHAR(50)     DEFAULT NULL             COMMENT '新合作状态',
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

-- ============================================================
-- 初始化权限数据
-- （超级账户和超级角色由 DataInitializer 在应用启动时自动创建）
-- ============================================================

-- 一级菜单
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(1,  '系统管理',   'system',        'menu', 0, 1),
(2,  '租户管理',   'tenant',        'menu', 1, 1),
(3,  '角色管理',   'role',          'menu', 1, 2),
(4,  '员工管理',   'user',          'menu', 1, 3),
(5,  '业务管理',   'business',      'menu', 0, 2);

-- 按钮权限 - 租户管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(11, '租户列表',   'tenant:list',   'button', 2, 1),
(12, '创建租户',   'tenant:add',    'button', 2, 2),
(13, '编辑租户',   'tenant:edit',   'button', 2, 3),
(14, '删除租户',   'tenant:delete', 'button', 2, 4);

-- 按钮权限 - 角色管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(21, '角色列表',   'role:list',     'button', 3, 1),
(22, '创建角色',   'role:add',      'button', 3, 2),
(23, '编辑角色',   'role:edit',     'button', 3, 3),
(24, '删除角色',   'role:delete',   'button', 3, 4);

-- 按钮权限 - 员工管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(31, '员工列表',   'user:list',     'button', 4, 1),
(32, '创建员工',   'user:add',      'button', 4, 2),
(33, '编辑员工',   'user:edit',     'button', 4, 3),
(34, '删除员工',   'user:delete',   'button', 4, 4);

-- 一级菜单 - 客户管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(7,  '客户管理',   'customer',       'menu', 5, 2);

-- 按钮权限 - 客户管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(51, '客户列表',   'customer:list',    'button', 7, 1),
(52, '新增客户',   'customer:add',     'button', 7, 2),
(53, '编辑客户',   'customer:edit',    'button', 7, 3),
(54, '删除客户',   'customer:delete',  'button', 7, 4),
(55, '标记无效',   'customer:invalid', 'button', 7, 5),
(56, '导入客户',   'customer:import',  'button', 7, 6),
(57, '导出客户',   'customer:export',  'button', 7, 7);

-- 按钮权限 - 客户跟进管理
INSERT INTO sys_permission (id, name, code, type, parent_id, sort) VALUES
(61, '跟进列表',   'followup:list',    'button', 7, 8),
(62, '新增跟进',   'followup:add',     'button', 7, 9),
(63, '编辑跟进',   'followup:edit',    'button', 7, 10),
(64, '删除跟进',   'followup:delete',  'button', 7, 11),
(65, '导出跟进',   'followup:export',  'button', 7, 12),
(66, '状态变更',   'followup:status',  'button', 7, 13);

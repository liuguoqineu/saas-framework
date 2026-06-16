-- ============================================
-- CRM手机端功能扩展 - 数据库迁移脚本
-- 执行时间：2026-06-16
-- 说明：个人信息修改、版本管理
-- ============================================

-- 1. sys_user 表增加字段
ALTER TABLE `sys_user` ADD COLUMN `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER `status`;
ALTER TABLE `sys_user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `avatar`;
ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER `phone`;

-- 2. APP版本管理表
CREATE TABLE IF NOT EXISTS `app_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `version_code` INT NOT NULL COMMENT '版本号（数字，用于比较大小）',
    `version_name` VARCHAR(20) NOT NULL COMMENT '版本名称（如1.0.0）',
    `platform` VARCHAR(20) NOT NULL COMMENT '平台：iOS/Android',
    `download_url` VARCHAR(500) NOT NULL COMMENT '下载地址',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    `md5` VARCHAR(32) DEFAULT NULL COMMENT '文件MD5校验值',
    `update_content` TEXT DEFAULT NULL COMMENT '更新内容',
    `force_update` TINYINT DEFAULT 0 COMMENT '是否强制更新：0-否，1-是',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户ID（0表示全局）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_platform_version` (`platform`, `version_code`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP版本管理表';

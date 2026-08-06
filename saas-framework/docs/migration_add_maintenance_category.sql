-- ============================================================
-- 迁移脚本：添加客户表运维需求分类字段
-- 文件名：migration_add_maintenance_category.sql
-- 说明：为 biz_customer 表添加 maintenance_category 字段
--       用于标记客户的运维需求等级（高频报修/常规运维/无报修）
-- 依赖：init.sql（基础表结构）
-- ============================================================

-- 添加运维需求分类字段
ALTER TABLE biz_customer
    ADD COLUMN maintenance_category VARCHAR(50) DEFAULT NULL
        COMMENT '运维需求分类: 高频报修/常规运维/无报修'
        AFTER smart_gas_system;

-- 添加索引以支持筛选查询
ALTER TABLE biz_customer
    ADD INDEX idx_maintenance_category (maintenance_category);

-- 添加详细地址字段到客户表
-- 执行时间: 2026-05-25
-- 描述: 为客户表添加详细地址字段，用于存储街道、门牌号等详细地址信息

ALTER TABLE biz_customer 
ADD COLUMN detail_address VARCHAR(255) DEFAULT NULL COMMENT '详细地址（街道、门牌号等）' 
AFTER address;

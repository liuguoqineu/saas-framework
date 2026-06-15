-- ============================================================
-- 修复 sys_user 表缺失字段
-- 执行方式: 在 MySQL 中执行此脚本
-- ============================================================

USE saaslearn;

-- 添加 post_type 字段
ALTER TABLE sys_user
ADD COLUMN post_type VARCHAR(50) DEFAULT NULL COMMENT '岗位类型: DEV-研发, OPS-运维, CS-客服'
AFTER status;

-- 添加 leader_id 字段
ALTER TABLE sys_user
ADD COLUMN leader_id BIGINT DEFAULT NULL COMMENT '领导ID'
AFTER post_type;

-- 验证字段是否添加成功
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'saaslearn'
  AND TABLE_NAME = 'sys_user'
  AND COLUMN_NAME IN ('post_type', 'leader_id');

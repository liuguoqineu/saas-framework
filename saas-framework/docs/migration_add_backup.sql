-- 数据库备份记录表
CREATE TABLE IF NOT EXISTS `sys_backup_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `backup_name` varchar(200) NOT NULL COMMENT '备份文件名',
  `backup_path` varchar(500) NOT NULL COMMENT '备份文件路径',
  `backup_size` bigint DEFAULT 0 COMMENT '备份文件大小（字节）',
  `backup_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '备份类型：MANUAL-手动备份，AUTO-自动备份',
  `status` varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '备份状态：SUCCESS-成功，FAILED-失败，PROCESSING-处理中',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注/错误信息',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID，0表示系统级备份',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_backup_type` (`backup_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库备份记录表';

-- ============================================================
-- 打卡管理模块 - 数据库迁移脚本
-- ============================================================

USE saaslearn;

-- 打卡记录表
DROP TABLE IF EXISTS biz_check_in;
CREATE TABLE biz_check_in (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id             BIGINT          NOT NULL                 COMMENT '打卡用户ID',
    user_name           VARCHAR(50)     DEFAULT NULL             COMMENT '打卡用户姓名',
    check_in_time       DATETIME        NOT NULL                 COMMENT '打卡时间',
    address             VARCHAR(255)    NOT NULL                 COMMENT '打卡地址',
    photo_path          VARCHAR(500)    DEFAULT NULL             COMMENT '打卡照片路径',
    remark              VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    tenant_id           BIGINT          NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_check_in_time (check_in_time),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

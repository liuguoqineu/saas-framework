-- ============================================================
-- 字典表迁移脚本
-- 新增系统字典表，用于管理客户分类等可配置选项
-- ============================================================

USE saaslearn;

-- 字典类型表
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    code        VARCHAR(100)    NOT NULL                 COMMENT '字典编码，如 business_category',
    name        VARCHAR(100)    NOT NULL                 COMMENT '字典名称，如 业务类型一级分类',
    description VARCHAR(500)    DEFAULT NULL             COMMENT '字典描述',
    status      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态: 1-启用, 0-禁用',
    sort        INT             DEFAULT 0                COMMENT '排序号',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表';

-- 字典项表
DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    dict_id     BIGINT          NOT NULL                 COMMENT '字典类型ID',
    value       VARCHAR(200)    NOT NULL                 COMMENT '字典项值',
    label       VARCHAR(200)    NOT NULL                 COMMENT '字典项标签（显示文本）',
    parent_value VARCHAR(200)   DEFAULT NULL             COMMENT '父项值（用于两级联动）',
    sort        INT             DEFAULT 0                COMMENT '排序号',
    status      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态: 1-启用, 0-禁用',
    remark      VARCHAR(500)    DEFAULT NULL             COMMENT '备注说明',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_dict_id (dict_id),
    INDEX idx_parent_value (parent_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典项表';

-- ============================================================
-- 插入客户相关字典数据
-- ============================================================

-- 1. 业务类型一级分类
INSERT INTO sys_dict (id, code, name, description, sort) VALUES
(1, 'business_category', '业务类型一级分类', '客户业务类型一级分类：加气站类/商业用气/工业用气', 1);

-- 2. 业务类型二级分类
INSERT INTO sys_dict (id, code, name, description, sort) VALUES
(2, 'business_type', '业务类型二级分类', '客户业务类型二级分类，关联一级分类', 2);

-- 3. 合作状态一级分类
INSERT INTO sys_dict (id, code, name, description, sort) VALUES
(3, 'cooperation_category', '合作状态一级分类', '客户合作状态一级分类：已合作/潜在/无效', 3);

-- 4. 合作状态二级分类
INSERT INTO sys_dict (id, code, name, description, sort) VALUES
(4, 'cooperation_status', '合作状态二级分类', '客户合作状态二级分类，关联一级分类', 4);

-- 5. 运维需求分类
INSERT INTO sys_dict (id, code, name, description, sort) VALUES
(5, 'maintenance_category', '运维需求分类', '客户运维需求分类：高频报修/常规运维/无报修', 5);

-- 6. 用气规模分类
INSERT INTO sys_dict (id, code, name, description, sort) VALUES
(6, 'gas_scale', '用气规模分类', '工业客户用气规模分类', 6);

-- ============================================================
-- 插入字典项数据
-- ============================================================

-- 业务类型一级分类项
INSERT INTO sys_dict_item (dict_id, value, label, sort, remark) VALUES
(1, '加气站类', '加气站类', 1, '加气站类客户，关联智慧燃气设备运维需求'),
(1, '商业用气', '商业用气', 2, '商业用气客户：餐饮/团餐/其他商业'),
(1, '工业用气', '工业用气', 3, '工业用气客户，按用气规模辅助分类');

-- 业务类型二级分类项（parent_value 关联一级分类）
INSERT INTO sys_dict_item (dict_id, value, label, parent_value, sort, remark) VALUES
-- 加气站类
(2, 'CNG加气站', 'CNG加气站', '加气站类', 1, 'CNG加气站'),
(2, 'LPG加气站', 'LPG加气站', '加气站类', 2, 'LPG加气站'),
-- 商业用气
(2, '餐饮类', '餐饮类（饭店、餐馆）', '商业用气', 3, '餐饮类客户'),
(2, '团餐类', '团餐类（大企业食堂、高校食堂）', '商业用气', 4, '团餐类客户'),
(2, '其他商业类', '其他商业类（酒店、商超后厨等）', '商业用气', 5, '其他商业类客户'),
-- 工业用气
(2, '大型', '大型', '工业用气', 6, '大型工业客户'),
(2, '中型', '中型', '工业用气', 7, '中型工业客户'),
(2, '小型', '小型', '工业用气', 8, '小型工业客户');

-- 合作状态一级分类项
INSERT INTO sys_dict_item (dict_id, value, label, sort, remark) VALUES
(3, '已合作', '已合作', 1, '已合作客户，已签合同，正在使用系统'),
(3, '潜在', '潜在', 2, '潜在客户，已对接，有关注和跟进需求'),
(3, '无效', '无效', 3, '无效客户，多次对接无回应或不符合服务范围');

-- 合作状态二级分类项（parent_value 关联一级分类）
INSERT INTO sys_dict_item (dict_id, value, label, parent_value, sort, remark) VALUES
-- 已合作
(4, '正常履约', '正常履约（已签合同，正在使用系统，无逾期）', '已合作', 1, '正常履约状态'),
(4, '终止合作', '终止合作（不再合作，留存历史数据）', '已合作', 2, '终止合作状态'),
-- 潜在
(4, '高潜力', '高潜力（明确需求，短期内可签约）', '潜在', 3, '高潜力客户'),
(4, '中潜力', '中潜力（有需求但时间不明确）', '潜在', 4, '中潜力客户'),
(4, '低潜力', '低潜力（需求不明确，需长期跟进）', '潜在', 5, '低潜力客户'),
-- 无效
(4, '无效客户', '无效客户（多次无回应或不符合服务范围）', '无效', 6, '无效客户');

-- 运维需求分类项
INSERT INTO sys_dict_item (dict_id, value, label, sort, remark) VALUES
(5, '高频报修', '高频报修客户（需重点关注）', 1, '报修次数多，需重点关注和优先响应'),
(5, '常规运维', '常规运维客户（按计划巡检）', 2, '正常报修频率，按计划定期巡检'),
(5, '无报修', '无报修客户（系统稳定）', 3, '系统运行稳定，暂无报修记录');

-- 用气规模分类项
INSERT INTO sys_dict_item (dict_id, value, label, sort, remark) VALUES
(6, '大型', '大型', 1, '大型用气规模'),
(6, '中型', '中型', 2, '中型用气规模'),
(6, '小型', '小型', 3, '小型用气规模');

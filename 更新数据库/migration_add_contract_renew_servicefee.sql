-- ============================================================
-- 合同表新增续签日期、服务费字段 + 充装介质字典新增3项
-- ============================================================

USE saaslearn;

-- 1. 合同表新增续签日期、服务费列
ALTER TABLE biz_contract
    ADD COLUMN renew_date  DATE             DEFAULT NULL COMMENT '续签日期' AFTER expire_date,
    ADD COLUMN service_fee DECIMAL(12, 2)   DEFAULT NULL COMMENT '服务费（元）' AFTER contract_amount;

-- 2. 充装介质(business_type)字典新增3项：LNG加气站、CNG LNG合建站、甲烷加气站
INSERT INTO sys_dict_item (dict_id, value, label, parent_value, sort, remark) VALUES
(2, 'LNG加气站', 'LNG加气站', '加气站类', 3, 'LNG加气站'),
(2, 'CNG LNG合建站', 'CNG LNG合建站', '加气站类', 4, 'CNG LNG合建站'),
(2, '甲烷加气站', '甲烷加气站', '加气站类', 5, '甲烷加气站');

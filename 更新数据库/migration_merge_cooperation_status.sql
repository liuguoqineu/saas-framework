-- ============================================================
-- 数据库迁移脚本（合并执行）
--   1. 业务分类：工业用气 → 民业用气
--   2. 合作分类：一级二级合并为统一合作状态
--
-- 执行时机：在部署新代码之前执行
-- ============================================================

-- ==================== 第一部分：业务分类重命名 ====================

-- 1. 更新业务类型一级分类字典项（工业用气 → 民业用气）
UPDATE sys_dict_item 
SET value = '民业用气', label = '民业用气', remark = '民业用气客户，按用气规模辅助分类' 
WHERE dict_id = (SELECT id FROM sys_dict WHERE code = 'business_category') AND value = '工业用气';

-- 2. 更新业务类型二级分类的 parent_value
UPDATE sys_dict_item 
SET parent_value = '民业用气', remark = REPLACE(remark, '工业', '民业')
WHERE dict_id = (SELECT id FROM sys_dict WHERE code = 'business_type') AND parent_value = '工业用气';

-- 3. 更新用气规模分类字典描述（工业 → 民业）
UPDATE sys_dict 
SET description = '民业客户用气规模分类'
WHERE code = 'gas_scale';

-- 4. 已有客户数据同步
UPDATE biz_customer SET business_category = '民业用气' WHERE business_category = '工业用气';


-- ==================== 第二部分：合作分类合并为统一合作状态 ====================

-- 5. 备份原数据（可选，建议执行）
-- CREATE TABLE biz_customer_backup AS SELECT * FROM biz_customer;
-- CREATE TABLE biz_customer_status_log_backup AS SELECT * FROM biz_customer_status_log;

-- 6. 合并 cooperation_category 和 cooperation_status 到新的 cooperation_status
UPDATE biz_customer 
SET cooperation_status = CASE 
    WHEN cooperation_category = '无效客户' THEN '无效客户'
    ELSE COALESCE(cooperation_status, '中潜力')
END
WHERE deleted = 0;

-- 7. 合并状态变更日志中的旧数据
UPDATE biz_customer_status_log 
SET old_cooperation_status = CASE 
    WHEN old_cooperation_category = '无效客户' THEN '无效客户'
    ELSE old_cooperation_status
END
WHERE old_cooperation_category IS NOT NULL;

UPDATE biz_customer_status_log 
SET new_cooperation_status = CASE 
    WHEN new_cooperation_category = '无效客户' THEN '无效客户'
    ELSE new_cooperation_status
END
WHERE new_cooperation_category IS NOT NULL;

-- 8. 删除旧的 cooperation_category 字典（sys_dict + sys_dict_item）
DELETE FROM sys_dict_item WHERE dict_id IN (SELECT id FROM sys_dict WHERE code = 'cooperation_category');
DELETE FROM sys_dict WHERE code = 'cooperation_category';

-- 9. 更新 cooperation_status 字典主记录（二级分类 → 合作状态）
UPDATE sys_dict 
SET name = '合作状态', description = '客户合作状态'
WHERE code = 'cooperation_status';

-- 10. 清空旧的二级字典项，插入新的扁平字典项（parent_value 全部置空）
DELETE FROM sys_dict_item WHERE dict_id IN (SELECT id FROM sys_dict WHERE code = 'cooperation_status');

INSERT INTO sys_dict_item (dict_id, value, label, parent_value, sort, remark) VALUES
((SELECT id FROM sys_dict WHERE code='cooperation_status'), '正常履约', '正常履约（已签合同，正在使用系统，无逾期）', NULL, 1, NULL),
((SELECT id FROM sys_dict WHERE code='cooperation_status'), '终止合作', '终止合作（不再合作，留存历史数据）', NULL, 2, NULL),
((SELECT id FROM sys_dict WHERE code='cooperation_status'), '高潜力', '高潜力（明确需求，短期内可签约）', NULL, 3, NULL),
((SELECT id FROM sys_dict WHERE code='cooperation_status'), '中潜力', '中潜力（有需求但时间不明确）', NULL, 4, NULL),
((SELECT id FROM sys_dict WHERE code='cooperation_status'), '低潜力', '低潜力（需求不明确，需长期跟进）', NULL, 5, NULL),
((SELECT id FROM sys_dict WHERE code='cooperation_status'), '无效客户', '无效客户', NULL, 6, NULL);


-- ==================== 验证结果 ====================

SELECT '=== sys_dict 验证 ===' as info;
SELECT id, code, name, description FROM sys_dict ORDER BY sort;

SELECT '=== 业务分类验证 ===' as info;
SELECT 
       SUM(CASE WHEN business_category = '民业用气' THEN 1 ELSE 0 END) as minye_count,
       SUM(CASE WHEN business_category = '工业用气' THEN 1 ELSE 0 END) as gongye_remain
FROM biz_customer WHERE deleted = 0;

SELECT '=== 合作状态验证 ===' as info;
SELECT 
       COUNT(*) as total,
       SUM(CASE WHEN cooperation_status = '无效客户' THEN 1 ELSE 0 END) as invalid_count,
       SUM(CASE WHEN cooperation_status IN ('正常履约','终止合作') THEN 1 ELSE 0 END) as cooperating_count,
       SUM(CASE WHEN cooperation_status IN ('高潜力','中潜力','低潜力') THEN 1 ELSE 0 END) as potential_count
FROM biz_customer WHERE deleted = 0;

SELECT '=== cooperation_status 字典项验证 ===' as info;
SELECT value, label, parent_value, sort FROM sys_dict_item 
WHERE dict_id = (SELECT id FROM sys_dict WHERE code = 'cooperation_status') AND deleted = 0 ORDER BY sort;


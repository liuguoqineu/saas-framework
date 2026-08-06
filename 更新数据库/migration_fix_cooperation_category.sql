-- ============================================================
-- 修复字典数据：将"无效"统一改为"无效客户"
-- 执行时间：2026-06-02
-- 问题：之前创建的字典数据使用了"无效"，但代码期望的是"无效客户"
-- ============================================================

USE saaslearn;

-- 1. 修复合作一级分类字典项：将"无效"改为"无效客户"
UPDATE sys_dict_item 
SET value = '无效客户', 
    label = '无效客户',
    remark = '无效客户，多次对接无回应或不符合服务范围'
WHERE dict_id = 3 
  AND value = '无效';

-- 2. 修复合作二级分类的parent_value关联：将"无效"改为"无效客户"
UPDATE sys_dict_item 
SET parent_value = '无效客户'
WHERE dict_id = 4 
  AND parent_value = '无效';

-- 3. 修复字典描述
UPDATE sys_dict 
SET description = '客户合作状态一级分类：已合作/潜在/无效客户'
WHERE code = 'cooperation_category';

-- 4. 修复已有客户数据中的cooperation_category字段（如果存在"无效"值）
UPDATE biz_customer 
SET cooperation_category = '无效客户',
    is_invalid = 1
WHERE cooperation_category = '无效' 
   OR cooperation_category = '潜在客户';  -- 同时修正旧的默认值"潜在客户"为"潜在"

-- 验证修复结果
SELECT '=== 合作一级分类 ===' AS info;
SELECT id, value, label, sort, remark FROM sys_dict_item WHERE dict_id = 3 ORDER BY sort;

SELECT '=== 合作二级分类 ===' AS info;
SELECT id, value, label, parent_value, sort FROM sys_dict_item WHERE dict_id = 4 ORDER BY sort;

SELECT '=== 客户合作状态分布 ===' AS info;
SELECT cooperation_category, COUNT(*) as count FROM biz_customer GROUP BY cooperation_category;

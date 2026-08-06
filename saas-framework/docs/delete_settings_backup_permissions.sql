-- ============================================================
-- 权限管理脚本：删除旧权限 + 设备管理模块权限初始化
-- ============================================================

-- ============================================================
-- 一、删除 settings:backup 相关权限（旧的三级格式权限）
-- ============================================================

DELETE FROM sys_role_permission WHERE permission_id IN (126, 127, 128, 129, 130, 131);
DELETE FROM sys_permission WHERE id IN (126, 127, 128, 129, 130, 131);

-- 验证删除结果（可选）
-- SELECT id, code, name, parent_id FROM sys_permission WHERE code LIKE 'settings:%';

-- ============================================================
-- 二、设备管理模块权限初始化SQL
-- ============================================================

-- 1. 先获取业务管理菜单的ID（如果不存在则创建）
SET @business_menu_id = (SELECT id FROM sys_permission WHERE code = 'business');
-- 如果不存在，先创建业务管理菜单（ID=5，根据init.sql）
INSERT INTO sys_permission (id, name, code, type, parent_id, sort)
SELECT 5, '业务管理', 'business', 'menu', 0, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'business');

-- 2. 创建设备管理一级菜单（挂在业务管理下）
INSERT INTO sys_permission (name, code, type, parent_id, sort)
SELECT '设备管理', 'device-management', 'menu', 5, 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'device-management');

SET @device_menu_id = (SELECT id FROM sys_permission WHERE code = 'device-management');

-- 3. 采购管理权限
INSERT INTO sys_permission (name, code, type, parent_id, sort)
SELECT '采购管理', 'purchase', 'menu', @device_menu_id, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase');

SET @purchase_menu_id = (SELECT id FROM sys_permission WHERE code = 'purchase');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('采购列表', 'purchase:list', 'button', @purchase_menu_id, 1),
('新增采购', 'purchase:add', 'button', @purchase_menu_id, 2),
('编辑采购', 'purchase:edit', 'button', @purchase_menu_id, 3),
('删除采购', 'purchase:delete', 'button', @purchase_menu_id, 4),
('导出采购', 'purchase:export', 'button', @purchase_menu_id, 5);

-- 4. 入库管理权限
INSERT INTO sys_permission (name, code, type, parent_id, sort)
SELECT '入库管理', 'stock-in', 'menu', @device_menu_id, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'stock-in');

SET @stockin_menu_id = (SELECT id FROM sys_permission WHERE code = 'stock-in');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('入库列表', 'stock-in:list', 'button', @stockin_menu_id, 1),
('新增入库', 'stock-in:add', 'button', @stockin_menu_id, 2),
('编辑入库', 'stock-in:edit', 'button', @stockin_menu_id, 3),
('删除入库', 'stock-in:delete', 'button', @stockin_menu_id, 4),
('导出入库', 'stock-in:export', 'button', @stockin_menu_id, 5);

-- 5. 库存台账权限
INSERT INTO sys_permission (name, code, type, parent_id, sort)
SELECT '库存台账', 'inventory', 'menu', @device_menu_id, 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory');

SET @inventory_menu_id = (SELECT id FROM sys_permission WHERE code = 'inventory');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('台账列表', 'inventory:list', 'button', @inventory_menu_id, 1),
('台账出库', 'inventory:stock-out', 'button', @inventory_menu_id, 2),
('导出台账', 'inventory:export', 'button', @inventory_menu_id, 3);

-- 6. 出库管理权限
INSERT INTO sys_permission (name, code, type, parent_id, sort)
SELECT '出库管理', 'stock-out', 'menu', @device_menu_id, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'stock-out');

SET @stockout_menu_id = (SELECT id FROM sys_permission WHERE code = 'stock-out');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('出库列表', 'stock-out:list', 'button', @stockout_menu_id, 1),
('新增出库', 'stock-out:add', 'button', @stockout_menu_id, 2),
('编辑出库', 'stock-out:edit', 'button', @stockout_menu_id, 3),
('删除出库', 'stock-out:delete', 'button', @stockout_menu_id, 4),
('导出出库', 'stock-out:export', 'button', @stockout_menu_id, 5);

-- 7. 设备档案权限
INSERT INTO sys_permission (name, code, type, parent_id, sort)
SELECT '设备档案', 'device', 'menu', @device_menu_id, 5
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'device');

SET @device_archive_menu_id = (SELECT id FROM sys_permission WHERE code = 'device');

INSERT INTO sys_permission (name, code, type, parent_id, sort) VALUES
('设备列表', 'device:list', 'button', @device_archive_menu_id, 1),
('新增设备', 'device:add', 'button', @device_archive_menu_id, 2),
('编辑设备', 'device:edit', 'button', @device_archive_menu_id, 3),
('删除设备', 'device:delete', 'button', @device_archive_menu_id, 4),
('导出设备', 'device:export', 'button', @device_archive_menu_id, 5);

-- 8. 为超级管理员角色(id=1)同步新增权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission 
WHERE code IN ('device-management', 'purchase', 'purchase:list', 'purchase:add', 'purchase:edit', 'purchase:delete', 'purchase:export',
               'stock-in', 'stock-in:list', 'stock-in:add', 'stock-in:edit', 'stock-in:delete', 'stock-in:export',
               'inventory', 'inventory:list', 'inventory:stock-out', 'inventory:export',
               'stock-out', 'stock-out:list', 'stock-out:add', 'stock-out:edit', 'stock-out:delete', 'stock-out:export',
               'device', 'device:list', 'device:add', 'device:edit', 'device:delete', 'device:export');

-- 查看新增的权限
SELECT * FROM sys_permission WHERE code LIKE '%purchase%' OR code LIKE '%stock%' OR code LIKE '%inventory%' OR code LIKE '%device%' OR code LIKE '%device-management%';
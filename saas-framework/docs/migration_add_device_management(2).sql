-- ============================================
-- 设备管理系统 - 数据库建表脚本
-- 文档版本：V1.1
-- 编制日期：2026-06-16
-- 数据库：MySQL 8.0+
-- 说明：设备资产档案闭环管理系统（采购→入库→库存→出库→安装→维修→更换→报废）
-- 变更记录：
--   V1.1 - device表增加warehouse_name/purchase_item_id字段，设备状态新增0-待入库
--        - 入库单增加device_id字段（设备入库时关联具体设备档案）
--        - 出库单device_id改为out_device_id（设备和配件完全独立，配件出库无需绑定设备）
--        - 更换记录主表移除设备关联字段，明细下沉到子表
--        - 更换明细子表增加old_device_id/new_device_id/new_stock_out_order_id字段
-- ============================================

-- ============================================
-- 1. 采购单主表（含供应商信息）
-- ============================================
CREATE TABLE IF NOT EXISTS `device_purchase_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(30) NOT NULL COMMENT '采购合同编号',
    `purchase_date` DATE NOT NULL COMMENT '采购日期',
    `supplier_name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
    `supplier_contact` VARCHAR(50) DEFAULT NULL COMMENT '供应商联系人',
    `supplier_phone` VARCHAR(20) DEFAULT NULL COMMENT '供应商联系电话',
    `supplier_address` VARCHAR(300) DEFAULT NULL COMMENT '供应商地址',
    `supplier_unified_code` VARCHAR(50) DEFAULT NULL COMMENT '供应商统一社会信用代码',
    `total_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '采购总金额',
    `purchaser` VARCHAR(50) NOT NULL COMMENT '采购负责人',
    `purchaser_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人联系方式',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待入库，1-部分入库，2-已入库',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_purchase_date` (`purchase_date`),
    KEY `idx_supplier_name` (`supplier_name`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单主表（含供应商信息）';

-- ============================================
-- 2. 采购明细表
-- ============================================
CREATE TABLE IF NOT EXISTS `device_purchase_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '采购单ID',
    `item_type` TINYINT NOT NULL COMMENT '类型：1-设备，2-配件',
    `item_name` VARCHAR(100) NOT NULL COMMENT '设备/配件名称',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `quantity` INT NOT NULL COMMENT '采购数量',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位（台/个/套）',
    `unit_price` DECIMAL(12,2) NOT NULL COMMENT '单价',
    `total_price` DECIMAL(12,2) NOT NULL COMMENT '总价',
    `factory_no` VARCHAR(100) DEFAULT NULL COMMENT '出厂编号',
    `cert_file` VARCHAR(500) DEFAULT NULL COMMENT '合格证附件URL',
    `inspect_file` VARCHAR(500) DEFAULT NULL COMMENT '质检报告附件URL',
    `delivery_file` VARCHAR(500) DEFAULT NULL COMMENT '送货单附件URL',
    `stocked_qty` INT NOT NULL DEFAULT 0 COMMENT '已入库数量',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_item_name_model` (`item_name`, `model`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购明细表';

-- ============================================
-- 3. 入库单表
-- 说明：设备入库时（item_type=1），需关联具体device_id，设备档案在采购时已创建
-- ============================================
CREATE TABLE IF NOT EXISTS `device_stock_in_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(30) NOT NULL COMMENT '入库单号',
    `purchase_order_id` BIGINT DEFAULT NULL COMMENT '关联采购单ID',
    `purchase_item_id` BIGINT DEFAULT NULL COMMENT '关联采购明细ID',
    `item_type` TINYINT NOT NULL COMMENT '类型：1-设备，2-配件',
    `item_name` VARCHAR(100) NOT NULL COMMENT '设备/配件名称',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `quantity` INT NOT NULL COMMENT '入库数量',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `device_id` BIGINT DEFAULT NULL COMMENT '关联设备档案ID（item_type=1设备入库时填写，标识具体哪台设备）',
    `warehouse_name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '存放位置（货架区域）',
    `check_status` TINYINT NOT NULL DEFAULT 0 COMMENT '验收状态：0-待验收，1-验收通过，2-验收不通过',
    `check_photo` VARCHAR(500) DEFAULT NULL COMMENT '验收照片URL',
    `handler` VARCHAR(50) NOT NULL COMMENT '入库经办人',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '入库备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_purchase_order_id` (`purchase_order_id`),
    KEY `idx_item_name_model` (`item_name`, `model`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_warehouse_name` (`warehouse_name`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- ============================================
-- 4. 出库单表
-- 说明：out_device_id为出库的设备ID（设备出库时），设备和配件完全独立，配件出库无需绑定设备
-- ============================================
CREATE TABLE IF NOT EXISTS `device_stock_out_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(30) NOT NULL COMMENT '出库单号',
    `item_type` TINYINT NOT NULL COMMENT '类型：1-设备，2-配件',
    `item_name` VARCHAR(100) NOT NULL COMMENT '设备/配件名称',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `quantity` INT NOT NULL COMMENT '出库数量',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `usage_type` TINYINT NOT NULL COMMENT '领用用途：1-新装设备，2-维修更换，3-抢修备用',
    `out_device_id` BIGINT DEFAULT NULL COMMENT '出库的设备ID（item_type=1设备出库时，标识出的是哪台设备）',
    `repair_order_id` BIGINT DEFAULT NULL COMMENT '关联维修工单ID',
    `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '领用部门',
    `receiver` VARCHAR(50) NOT NULL COMMENT '领用人',
    `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `reviewer` VARCHAR(50) DEFAULT NULL COMMENT '审核人',
    `out_photo` VARCHAR(500) DEFAULT NULL COMMENT '出库照片/领用签字附件URL',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_item_name_model` (`item_name`, `model`),
    KEY `idx_out_device_id` (`out_device_id`),
    KEY `idx_repair_order_id` (`repair_order_id`),
    KEY `idx_usage_type` (`usage_type`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- ============================================
-- 5. 实时库存表（合并仓库表，用warehouse_name存储仓库信息）
-- 说明：库存表只负责品类数量汇总，具体设备追踪走device表
-- ============================================
CREATE TABLE IF NOT EXISTS `device_inventory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `item_type` TINYINT NOT NULL COMMENT '类型：1-设备，2-配件',
    `item_name` VARCHAR(100) NOT NULL COMMENT '设备/配件名称',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `warehouse_name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
    `total_qty` INT NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    `stocked_in_qty` INT NOT NULL DEFAULT 0 COMMENT '累计入库数量',
    `stocked_out_qty` INT NOT NULL DEFAULT 0 COMMENT '累计出库数量',
    `min_stock_qty` INT DEFAULT NULL COMMENT '最低库存预警阈值',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_warehouse` (`item_type`, `item_name`, `model`, `spec`, `warehouse_name`, `tenant_id`),
    KEY `idx_warehouse_name` (`warehouse_name`),
    KEY `idx_item_name` (`item_name`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时库存表';

-- ============================================
-- 6. 设备档案主表
-- 说明：设备在采购时即创建档案记录并分配唯一编码，入库时填写warehouse_name，
--       出库时清空warehouse_name，安装时补充安装信息。
--       device表为设备唯一追踪主表，device_inventory为品类数量汇总表。
-- ============================================
CREATE TABLE IF NOT EXISTS `device` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_code` VARCHAR(30) NOT NULL COMMENT '设备唯一档案编码',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(100) DEFAULT NULL COMMENT '型号',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '设备分类',
    `purchase_order_id` BIGINT DEFAULT NULL COMMENT '关联采购单ID（采购时填写）',
    `purchase_item_id` BIGINT DEFAULT NULL COMMENT '关联采购明细ID（采购时填写，追溯采购源头）',
    `stock_in_order_id` BIGINT DEFAULT NULL COMMENT '关联入库单ID（入库时填写）',
    `stock_out_order_id` BIGINT DEFAULT NULL COMMENT '关联出库单ID（出库时填写）',
    `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '当前所在仓库名称（入库时填写，出库/安装后清空）',
    `install_location` VARCHAR(200) DEFAULT NULL COMMENT '安装位置（安装时填写）',
    `install_date` DATE DEFAULT NULL COMMENT '安装日期',
    `install_person` VARCHAR(50) DEFAULT NULL COMMENT '安装人员',
    `use_date` DATE DEFAULT NULL COMMENT '投用日期',
    `accept_record` VARCHAR(500) DEFAULT NULL COMMENT '验收记录',
    `install_file` VARCHAR(500) DEFAULT NULL COMMENT '安装调试资料附件URL',
    `accept_photo` VARCHAR(500) DEFAULT NULL COMMENT '验收照片URL',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待入库，1-待安装，2-在用，3-维修中，4-停用，5-报废',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`device_code`),
    KEY `idx_device_name` (`device_name`),
    KEY `idx_status` (`status`),
    KEY `idx_warehouse_name` (`warehouse_name`),
    KEY `idx_install_location` (`install_location`),
    KEY `idx_purchase_order_id` (`purchase_order_id`),
    KEY `idx_purchase_item_id` (`purchase_item_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备档案主表';

-- ============================================
-- 7. 维修工单表（扩展现有 biz_repair_order 表）
-- 说明：现有 biz_repair_order 表为客户报修场景，此处新增设备维修相关字段
-- ============================================
ALTER TABLE `biz_repair_order`
    ADD COLUMN `device_id` BIGINT DEFAULT NULL COMMENT '关联设备档案ID（设备维修时填写）' AFTER `creator_name`,
    ADD COLUMN `device_code` VARCHAR(30) DEFAULT NULL COMMENT '设备编码（冗余存储）' AFTER `device_id`,
    ADD COLUMN `fault_time` DATETIME DEFAULT NULL COMMENT '故障时间' AFTER `device_code`,
    ADD COLUMN `fault_part` VARCHAR(100) DEFAULT NULL COMMENT '故障部位' AFTER `fault_time`,
    ADD COLUMN `repair_start_time` DATETIME DEFAULT NULL COMMENT '维修开始时间' AFTER `fault_part`,
    ADD COLUMN `repair_end_time` DATETIME DEFAULT NULL COMMENT '维修结束时间' AFTER `repair_start_time`,
    ADD COLUMN `repair_duration` DECIMAL(6,1) DEFAULT NULL COMMENT '维修时长（小时）' AFTER `repair_end_time`,
    ADD COLUMN `repair_photo_before` VARCHAR(500) DEFAULT NULL COMMENT '维修前照片URL' AFTER `repair_duration`,
    ADD COLUMN `repair_photo_after` VARCHAR(500) DEFAULT NULL COMMENT '维修后照片URL' AFTER `repair_photo_before`,
    ADD COLUMN `has_replacement` TINYINT NOT NULL DEFAULT 0 COMMENT '是否更换配件：0-否，1-是' AFTER `repair_photo_after`,
    ADD COLUMN `replacement_id` BIGINT DEFAULT NULL COMMENT '关联更换记录主表ID（has_replacement=1时填写）' AFTER `has_replacement`,
    ADD INDEX `idx_device_id` (`device_id`),
    ADD INDEX `idx_has_replacement` (`has_replacement`);

-- ============================================
-- 8. 更换记录主表（统一管理配件更换和整机更换）
-- 说明：主表只记录整体更换信息，设备/配件明细全部在子表 device_replacement_item 中
-- ============================================
CREATE TABLE IF NOT EXISTS `device_replacement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `replacement_no` VARCHAR(30) NOT NULL COMMENT '更换单号',
    `replacement_type` TINYINT NOT NULL COMMENT '更换类型：1-配件更换，2-整机更换',
    `repair_order_id` BIGINT DEFAULT NULL COMMENT '关联维修工单ID',
    `replace_time` DATETIME NOT NULL COMMENT '更换时间',
    `replace_person` VARCHAR(50) NOT NULL COMMENT '更换人员',
    `replace_reason` VARCHAR(500) DEFAULT NULL COMMENT '更换原因',
    `replace_photo` VARCHAR(500) DEFAULT NULL COMMENT '现场照片URL',
    `operator` VARCHAR(50) NOT NULL COMMENT '操作人',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_replacement_no` (`replacement_no`),
    KEY `idx_replacement_type` (`replacement_type`),
    KEY `idx_repair_order_id` (`repair_order_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='更换记录主表';

-- ============================================
-- 9. 更换明细子表
-- 说明：存储本次更换涉及的设备/配件明细，一条主单可对应多条明细
--       item_type=1(配件)：old_item_name/new_item_name描述配件，stock_out_order_id关联出库
--       item_type=2(设备)：old_device_id/new_device_id关联具体设备，new_stock_out_order_id关联出库
-- ============================================
CREATE TABLE IF NOT EXISTS `device_replacement_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `replacement_id` BIGINT NOT NULL COMMENT '更换主单ID',
    `item_type` TINYINT NOT NULL COMMENT '明细类型：1-配件，2-设备',
    `old_device_id` BIGINT DEFAULT NULL COMMENT '旧设备ID（item_type=2设备时填写）',
    `old_item_name` VARCHAR(100) NOT NULL COMMENT '旧件名称',
    `old_item_model` VARCHAR(100) DEFAULT NULL COMMENT '旧件型号',
    `old_item_status` TINYINT DEFAULT NULL COMMENT '旧件处理：1-报废，2-返修，3-留用',
    `new_device_id` BIGINT DEFAULT NULL COMMENT '新设备ID（item_type=2设备时填写）',
    `new_item_name` VARCHAR(100) NOT NULL COMMENT '新件名称',
    `new_item_model` VARCHAR(100) DEFAULT NULL COMMENT '新件型号',
    `new_item_qty` INT NOT NULL COMMENT '新件数量',
    `stock_out_order_id` BIGINT DEFAULT NULL COMMENT '关联出库单ID（item_type=1配件出库领用）',
    `new_stock_out_order_id` BIGINT DEFAULT NULL COMMENT '新设备出库单ID（item_type=2设备出库时填写）',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_replacement_id` (`replacement_id`),
    KEY `idx_old_device_id` (`old_device_id`),
    KEY `idx_new_device_id` (`new_device_id`),
    KEY `idx_stock_out_order_id` (`stock_out_order_id`),
    KEY `idx_new_stock_out_order_id` (`new_stock_out_order_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='更换明细子表';

-- ============================================
-- 10. 设备履历时间线表（只增不删）
-- ============================================
CREATE TABLE IF NOT EXISTS `device_timeline` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `event_type` TINYINT NOT NULL COMMENT '事件类型：1-采购，2-入库，3-出库，4-安装，5-报修，6-维修，7-配件更换，8-整机更换，9-报废',
    `event_time` DATETIME NOT NULL COMMENT '事件时间',
    `event_title` VARCHAR(100) NOT NULL COMMENT '事件标题',
    `event_desc` VARCHAR(500) DEFAULT NULL COMMENT '事件描述',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `related_order_no` VARCHAR(30) DEFAULT NULL COMMENT '关联单号',
    `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备履历时间线表';

-- ============================================
-- 11. 操作日志表（只增不删）
-- ============================================
CREATE TABLE IF NOT EXISTS `device_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
    `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
    `target_id` BIGINT DEFAULT NULL COMMENT '操作目标ID',
    `target_no` VARCHAR(30) DEFAULT NULL COMMENT '操作目标单号',
    `detail` TEXT DEFAULT NULL COMMENT '操作详情（JSON格式）',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_module` (`module`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备操作日志表';

-- 客户表新增备注字段
ALTER TABLE biz_customer ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '客户备注' AFTER follow_up_person;

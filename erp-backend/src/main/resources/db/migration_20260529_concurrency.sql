-- ============================================
-- 并发安全优化迁移
-- 日期: 2026-05-29
-- ============================================

-- 1. sales_order 添加乐观锁版本号
ALTER TABLE sales_order ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 2. customer_shipping_address 添加唯一默认地址约束
--    MySQL 8.0 虚拟生成列 + UNIQUE: 只对 is_default=1 的行生效
ALTER TABLE customer_shipping_address ADD COLUMN default_marker BIGINT
    GENERATED ALWAYS AS (IF(is_default = 1, customer_id, NULL)) VIRTUAL
    COMMENT '默认地址唯一标记';
ALTER TABLE customer_shipping_address ADD UNIQUE INDEX uk_customer_default_address (default_marker);

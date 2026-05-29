-- ============================================
-- 数据库约束修复 + 废弃列清理
-- 日期: 2026-05-29
-- ============================================

-- === P0: 缺失的 UNIQUE 约束 ===

-- 1. 销售账户名唯一（核心业务标识）
ALTER TABLE sales_account ADD UNIQUE INDEX uk_account_name (account_name);

-- 2. 顾客微信号唯一（业务确认：一个顾客对应一个微信号）
ALTER TABLE customer ADD UNIQUE INDEX uk_wechat_account (wechat_account);

-- 3. 同一订单不能重复导入相同运单号
ALTER TABLE order_tracking ADD UNIQUE INDEX uk_order_tracking (order_id, tracking_no);

-- === P0: 缺失的 FOREIGN KEY ===

-- 4. 订单→销售账户
ALTER TABLE sales_order ADD CONSTRAINT fk_order_sales_account
    FOREIGN KEY (sales_account_id) REFERENCES sales_account(id);

-- === P1: FOREIGN KEY ===

-- 5. 订单→收件地址
ALTER TABLE sales_order ADD CONSTRAINT fk_order_shipping_address
    FOREIGN KEY (shipping_address_id) REFERENCES customer_shipping_address(id);

-- 6. 订单→收款渠道类型
ALTER TABLE sales_order ADD CONSTRAINT fk_order_payment_channel_type
    FOREIGN KEY (payment_channel_type_id) REFERENCES payment_channel_type(id);

-- === P2: 审计字段 FOREIGN KEY ===

-- 7. 销售账户创建人/更新人
ALTER TABLE sales_account ADD CONSTRAINT fk_sales_account_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user(id);
ALTER TABLE sales_account ADD CONSTRAINT fk_sales_account_updated_by
    FOREIGN KEY (updated_by) REFERENCES sys_user(id);

-- 8. 订单最后更新人
ALTER TABLE sales_order ADD CONSTRAINT fk_order_updated_by
    FOREIGN KEY (updated_by) REFERENCES sys_user(id);

-- 9. 产品创建人/更新人
ALTER TABLE product ADD CONSTRAINT fk_product_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user(id);
ALTER TABLE product ADD CONSTRAINT fk_product_updated_by
    FOREIGN KEY (updated_by) REFERENCES sys_user(id);

-- 10. 产品套装→父产品自引用
ALTER TABLE product ADD CONSTRAINT fk_product_parent
    FOREIGN KEY (parent_id) REFERENCES product(id);

-- === P3: 清理废弃列和索引 ===

-- 删除 sales_order.sales_wechat_id (旧版字段，实体类已移除)
ALTER TABLE sales_order DROP INDEX idx_sales_wechat_id;
ALTER TABLE sales_order DROP COLUMN sales_wechat_id;

-- 删除 product.category_id (分类已移除)
ALTER TABLE product DROP INDEX idx_category_id;
ALTER TABLE product DROP COLUMN category_id;

-- 删除 customer 废弃列 (email/address 已有替代表)
ALTER TABLE customer DROP COLUMN email;
ALTER TABLE customer DROP COLUMN address;

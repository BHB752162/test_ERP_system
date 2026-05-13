-- ============================================
-- 电商ERP系统 - 数据库初始化脚本
-- MySQL 8.4+
-- ============================================

CREATE DATABASE IF NOT EXISTS erp_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE erp_db;

-- ============================================
-- 1. 系统角色表
-- ============================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)     NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)     NOT NULL COMMENT '角色编码 ADMIN/SALES_MANAGER/SALES_PERSON',
    description VARCHAR(255)    DEFAULT NULL COMMENT '描述',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- ============================================
-- 2. 系统用户表
-- ============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL COMMENT '用户名',
    password    VARCHAR(255)    NOT NULL COMMENT 'BCrypt加密密码',
    real_name   VARCHAR(100)    DEFAULT NULL COMMENT '真实姓名',
    phone       VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    role_id     BIGINT          NOT NULL COMMENT '角色ID',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ============================================
-- 3. 销售微信号表
-- ============================================
DROP TABLE IF EXISTS sales_wechat;
CREATE TABLE sales_wechat (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    sales_person_id BIGINT          NOT NULL COMMENT '所属销售ID',
    wechat_account  VARCHAR(100)    NOT NULL COMMENT '微信号',
    wechat_nickname VARCHAR(100)    DEFAULT NULL COMMENT '微信昵称',
    qr_code         VARCHAR(500)    DEFAULT NULL COMMENT '二维码图片URL',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sales_person_id (sales_person_id),
    UNIQUE KEY uk_wechat_account (wechat_account),
    CONSTRAINT fk_wechat_sales FOREIGN KEY (sales_person_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售微信号';

-- ============================================
-- 4. 客户主数据表
-- ============================================
DROP TABLE IF EXISTS customer;
CREATE TABLE customer (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    customer_name   VARCHAR(200)    NOT NULL COMMENT '客户名称',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    address         TEXT            DEFAULT NULL COMMENT '地址',
    level           TINYINT         NOT NULL DEFAULT 0 COMMENT '等级 0=普通 1=银卡 2=金卡 3=钻石',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    remark          TEXT            DEFAULT NULL COMMENT '备注',
    created_by      BIGINT          DEFAULT NULL COMMENT '创建人ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_phone (phone),
    KEY idx_created_by (created_by),
    KEY idx_status (status),
    CONSTRAINT fk_customer_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户主数据';

-- ============================================
-- 5. 客户付款渠道表
-- ============================================
DROP TABLE IF EXISTS customer_payment_channel;
CREATE TABLE customer_payment_channel (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT          NOT NULL COMMENT '客户ID',
    channel_type    VARCHAR(50)     NOT NULL COMMENT '渠道类型 ALIPAY/WECHAT/BANK_CARD/CASH/OTHER',
    channel_account VARCHAR(200)    DEFAULT NULL COMMENT '账号/卡号',
    account_name    VARCHAR(100)    DEFAULT NULL COMMENT '户名',
    bank_name       VARCHAR(200)    DEFAULT NULL COMMENT '开户行（银行卡时）',
    is_default      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=非默认 1=默认',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_customer_id (customer_id),
    CONSTRAINT fk_payment_channel_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户付款渠道';

-- ============================================
-- 6. 客户联系人表
-- ============================================
DROP TABLE IF EXISTS customer_contact;
CREATE TABLE customer_contact (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT          NOT NULL COMMENT '客户ID',
    contact_name    VARCHAR(100)    NOT NULL COMMENT '联系人姓名',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    position        VARCHAR(100)    DEFAULT NULL COMMENT '职位',
    is_primary      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=非主要 1=主要联系人',
    remark          TEXT            DEFAULT NULL COMMENT '备注',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_customer_id (customer_id),
    CONSTRAINT fk_contact_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户联系人';

-- ============================================
-- 7. 产品分类表
-- ============================================
DROP TABLE IF EXISTS product_category;
CREATE TABLE product_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    category_name   VARCHAR(100)    NOT NULL COMMENT '分类名称',
    parent_id       BIGINT          DEFAULT NULL COMMENT '父分类ID，NULL为顶级',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES product_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品分类';

-- ============================================
-- 8. 产品表
-- ============================================
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_name    VARCHAR(200)    NOT NULL COMMENT '产品名称',
    category_id     BIGINT          DEFAULT NULL COMMENT '分类ID',
    product_code    VARCHAR(50)     DEFAULT NULL COMMENT 'SKU编号',
    description     TEXT            DEFAULT NULL COMMENT '描述',
    price           DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '销售单价',
    cost_price      DECIMAL(10,2)   DEFAULT 0.00 COMMENT '成本价',
    stock_quantity  INT             NOT NULL DEFAULT 0 COMMENT '库存数量',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=下架 1=上架',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品';

-- ============================================
-- 9. 销售订单表
-- ============================================
DROP TABLE IF EXISTS sales_order;
CREATE TABLE sales_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_no        VARCHAR(50)     NOT NULL COMMENT '订单号',
    customer_id     BIGINT          NOT NULL COMMENT '客户ID',
    sales_person_id BIGINT          NOT NULL COMMENT '所属销售ID',
    sales_wechat_id BIGINT          DEFAULT NULL COMMENT '下单所用微信号ID',
    total_amount    DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '总金额',
    discount_amount DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '折扣金额',
    final_amount    DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '最终金额',
    status          VARCHAR(30)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/COMPLETED/CANCELLED',
    remark          TEXT            DEFAULT NULL COMMENT '备注',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer_id (customer_id),
    KEY idx_sales_person_id (sales_person_id),
    KEY idx_sales_wechat_id (sales_wechat_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_order_sales FOREIGN KEY (sales_person_id) REFERENCES sys_user(id),
    CONSTRAINT fk_order_wechat FOREIGN KEY (sales_wechat_id) REFERENCES sales_wechat(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

-- ============================================
-- 10. 订单行项目表
-- ============================================
DROP TABLE IF EXISTS sales_order_item;
CREATE TABLE sales_order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    product_id      BIGINT          NOT NULL COMMENT '产品ID',
    product_name    VARCHAR(200)    DEFAULT NULL COMMENT '产品名称（快照）',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price      DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '单价',
    subtotal        DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '小计',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_product_id (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES sales_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单行项目';

-- ============================================
-- 11. 订单审批日志表
-- ============================================
DROP TABLE IF EXISTS order_audit_log;
CREATE TABLE order_audit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    action          VARCHAR(30)     NOT NULL COMMENT '操作类型 SUBMIT/APPROVE/REJECT/CANCEL/COMPLETE',
    operator_id     BIGINT          NOT NULL COMMENT '操作人ID',
    comment         TEXT            DEFAULT NULL COMMENT '审批意见',
    operated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_operator_id (operator_id),
    CONSTRAINT fk_audit_order FOREIGN KEY (order_id) REFERENCES sales_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单审批日志';

-- ============================================
-- 12. 微信号-客户绑定关系表
-- ============================================
DROP TABLE IF EXISTS customer_wechat_binding;
CREATE TABLE customer_wechat_binding (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    sales_wechat_id BIGINT          NOT NULL COMMENT '销售微信号ID',
    customer_id     BIGINT          NOT NULL COMMENT '客户ID',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=解绑 1=绑定',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wechat_customer (sales_wechat_id, customer_id),
    KEY idx_customer_id (customer_id),
    CONSTRAINT fk_binding_wechat FOREIGN KEY (sales_wechat_id) REFERENCES sales_wechat(id) ON DELETE CASCADE,
    CONSTRAINT fk_binding_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信号-客户绑定关系';

-- ============================================
-- 种子数据
-- ============================================

-- 插入角色
INSERT INTO sys_role (role_name, role_code, description) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('销售经理', 'SALES_MANAGER', '销售经理，可审批订单、管理绑定'),
('销售人员', 'SALES_PERSON', '销售人员，可下单和管理自己的客户');

-- 插入测试产品分类
-- 注意：管理员账号由应用启动时的 DataInitializer 自动创建（admin/admin123）
INSERT INTO product_category (category_name, parent_id, sort_order) VALUES
('电子产品', NULL, 1),
('服装鞋帽', NULL, 2),
('食品饮料', NULL, 3),
('手机', 1, 1),
('电脑', 1, 2),
('男装', 2, 1),
('女装', 2, 2);

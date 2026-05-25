# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

电商ERP系统 — 订单管理 + 顾客管理 + 产品管理。

- **后端**: Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + Spring Security + JWT (jjwt 0.11.5), JDK 17
- **前端**: Vue 3.4 + Vite 5 + Element Plus 2.5 + Pinia 2.1 + Vue Router 4.3 + Axios 1.6
- **数据库**: MySQL 8.0+, 17 张 InnoDB 表, utf8mb4
- **项目路径**: `E:\project\test_ERP_system\`

## Run Commands

```bash
# === 启动 MySQL (Windows 服务) ===
sc start MySQL84

# === 后端 (port 8080) ===
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
export MAVEN_HOME="/c/Program Files/Maven/apache-maven-3.9.15"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
cd E:/project/test_ERP_system/erp-backend && mvn spring-boot:run

# === 前端 (port 3000, 代理 /api -> :8080) ===
cd E:/project/test_ERP_system/erp-frontend && npm run dev
```

登录账号: `admin` / `admin123`

## Architecture

### Backend Package Layout (`com.erp`)

- **`common/`** — 全局配置: SecurityConfig (URL角色拦截), MyMetaObjectHandler (自动填充时间), GlobalExceptionHandler, ApiResponse/PageResult 统一响应
- **`security/`** — JWT认证: JwtUtil (生成/解析), JwtAuthenticationFilter (Bearer Token提取 + SecurityContext设置)
- **`module/{module}/`** — 每个业务模块按包分层: `controller/` → `service/` → `mapper/` → `entity/` + `dto/`

**模块对应关系:**

| 模块 | 数据库表 | 说明 |
|------|---------|------|
| `auth` | — | 登录, 获取用户信息, 个人信息编辑, 修改密码 |
| `user` + `role` | sys_user, sys_role | 用户管理 (ADMIN); 工号/姓名/手机/邮箱/角色/状态; 密码重置; 审计字段(创建/更新人) |
| `salesaccount` | sales_account, sales_account_user_binding | 销售账户管理, 增改查, 多对多绑定用户 |
| `customer` | customer, customer_payment_channel, customer_contact, customer_shipping_address, payment_channel_type | 顾客信息 + 付款渠道 + 联系人 + 收件地址 + 渠道类型管理(含审计字段) |
| `product` | product | 产品管理（分类已移除） |
| `binding` | customer_sales_account_binding | 销售账户↔顾客多对多绑定 |
| `order` | sales_order, sales_order_item, sales_order_payment, order_tracking, order_tracking_item | 下单 + 审批工作流 + 收款记录 + 运单号导入/查询 |
| `audit` | order_audit_log | 审批日志查询 |

**模块路径端点:**

| 子资源 | 端点前缀 | 说明 |
|--------|---------|------|
| 付款渠道 | `/api/customers/{cid}/payment-channels` | CRUD |
| 联系人 | `/api/customers/{cid}/contacts` | CRUD |
| 收件地址 | `/api/customers/{cid}/shipping-addresses` | CRUD, 默认地址互斥 |
| 渠道类型 | `/api/payment-channel-types` | 独立CRUD, POST/PUT/DELETE仅 ADMIN |
| 销售账户 | `/api/sales-accounts` | 独立CRUD, 仅 ADMIN; 编辑时可绑定/解绑用户 |

### Security Rules (SecurityConfig.java + @PreAuthorize)

```
POST /api/auth/login            → permitAll
GET /api/auth/user-info         → authenticated (获取个人信息)
PUT /api/auth/user-info         → authenticated (更新个人信息)
POST /api/auth/change-password  → authenticated (修改密码, 需原密码)
POST /api/auth/verify-password  → authenticated (验证原密码)
/api/users/**, /api/roles/**    → hasRole('ADMIN')
/api/sales-bindings/**          → hasRole('ADMIN')
/api/orders/*/approve|reject|ship|deliver|refund → hasRole('ADMIN')
/api/orders/import-tracking → hasRole('ADMIN')
/api/sales-accounts/**          → hasRole('ADMIN')
POST/PUT/DELETE /api/payment-channel-types → @PreAuthorize("hasRole('ADMIN')")
所有其他 /api/**                → authenticated
无状态会话, JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前
```

### Order State Machine

```
SAVED → (提交) → PENDING_APPROVAL → (通过) → APPROVED → (发货) → SHIPPED → (妥投) → DELIVERED
                                      → (驳回) → REJECTED
SAVED / PENDING_APPROVAL → (取消) → CANCELLED
APPROVED / SHIPPED / DELIVERED → (退款) → REFUNDED
```

- 状态枚举: SAVED, PENDING_APPROVAL, APPROVED, SHIPPED, DELIVERED, REJECTED, CANCELLED, REFUNDED
- 权限: SALES_PERSON 只能操作自己的订单; ADMIN 可看全部; 销售人员只能编辑/取消 SAVED 或 REJECTED 状态的订单
- 下单核心校验: 选择销售账户 → 该账户必须已绑定所选顾客 (customer_sales_account_binding)
- 订单号生成: `B`+yyMMdd+6位顺序号 (每日重置, 独立于订单ID)
- 操作记录: 每次状态变更写入 order_audit_log
- 发货方式: (1) 手动点击"发货"按钮 → SHIPPED; (2) **运单号导入** → 自动将 APPROVED 订单变更为 SHIPPED
- 订单保存字段: submittedAt(提交审批时间), approvedAt(审批通过时间), updatedBy(最后更新人), tag(标签: 空=无/DELAYED=延迟发货/WASH_CARE=洗护订单), mallOrderInfo(关联商场订单号)
- 收件地址快照: 创建订单时从 customer_shipping_address 快照到 sales_order(recipient_name/phone/address)
- 订单编辑: SAVED/REJECTED 状态可编辑（弹窗复用 OrderCreateDialog），已提交审批后不可编辑

### Order Controller Endpoints

```
GET    /api/orders              → 订单列表 (分页 + 筛选, 含运单信息)
GET    /api/orders/{id}         → 订单详情
POST   /api/orders              → 创建订单 (保存草稿)
PUT    /api/orders/{id}         → 更新订单 (仅 SAVED/REJECTED)
POST   /api/orders/{id}/submit  → 提交审批
POST   /api/orders/{id}/approve → 审批通过
POST   /api/orders/{id}/reject  → 驳回 (JSON body: {comment})
POST   /api/orders/{id}/cancel  → 取消 (仅 SAVED/REJECTED)
POST   /api/orders/{id}/ship    → 发货 (手动)
POST   /api/orders/{id}/deliver → 确认妥投
POST   /api/orders/{id}/refund  → 退款
GET    /api/orders/export       → 导出Excel (mode=order|product)
POST   /api/orders/import-tracking  → 运单号导入 (ADMIN, JSON数组)
GET    /api/orders/{id}/tracking     → 查询订单运单号列表
```

### Frontend Structure (`src/`)

- **`api/`** — Axios 封装 (`request.js`) + `download.js` (blob下载) + 每个模块的 API 函数
- **`router/`** — 路由懒加载 + 导航守卫 (未登录→/login; 角色不够→/dashboard)
- **`store/`** — Pinia auth store (token 持久化到 localStorage, key `erp_auth`)
- **`layout/`** — AppLayout (Sidebar + Navbar + router-view)
- **`views/`** — 每个功能一个文件夹; 含 `profile/` (个人信息), `channel-type/` (渠道类型管理), `sales-account/` (销售账户管理), `admin-tools/` (管理员工具: 运单号导入), `order/OrderCreateDialog.vue` (创建/编辑订单弹窗)
- **`components/`** — Pagination, StatusTag 全局组件
- **`composables/`** — `useCrudList` (通用列表加载体/分页/搜索/重置)

### Frontend Patterns

- `request.js` 拦截器: 请求加 `Authorization: Bearer` header; 响应处理统一错误弹窗, 401 时清除 `token` 和 `erp_auth` (Pinia持久化key) 后跳转登录
- `download.js`: 使用 `fetch` + `Authorization` header 绕过 Axios JSON 拦截器, 直接处理 blob 下载
- `router/index.js` navigation guard: 检查 token, 按需调用 `fetchUserInfo()` 恢复用户状态, 校验 `meta.roles`
- `Sidebar.vue` 菜单顺序: 仪表盘 → 订单管理(仅订单列表) → 顾客管理 → 产品管理(ADMIN) → 系统管理(销售账户+渠道类型+用户管理) → 审批管理 → 管理员工具(ADMIN: 运单号导入)
- `Navbar.vue` 头像下拉: "个人信息"跳转 /profile
- 创建订单: **弹窗形式** (`OrderCreateDialog.vue`), 在订单列表页打开, 不作为独立路由页面
- 用户管理页: 工号代替用户名, 含创建人/更新人/更新时间审计列, 操作列含编辑/重置密码/删除
- 仪表盘: 统计数据过滤逻辑删除(status=0)的记录

### Audit Pattern

多数表包含审计字段: `created_by` (BIGINT), `updated_by` (BIGINT), `created_at`, `updated_at`。在后端 Service 中通过 `SecurityUtils.getCurrentUserId()` 手动设置，配合批量查询 (`sysUserMapper.selectBatchIds`) 加载 `createdByName`/`updatedByName` 用于前端展示。DTO 中通过 `@TableField(exist = false)` 标记瞬态姓名字段。

### Batch Loading Pattern (避免 N+1)

订单列表/详情/导出共用批量加载辅助方法:
- `loadCustomerNames(Set<Long> ids)` — 1次查所有客户名
- `loadUserNames(Set<Long> ids)` — 1次查所有用户名
- `loadSalesAccounts(Set<Long> ids)` — 1次查所有销售账户
- `loadItemsByOrderIds(List<Long> ids)` — 1次查所有订单明细
- `loadPaymentsByOrderIds(List<Long> ids)` — 1次查所有收款记录
- `loadTrackingsByOrderIds(List<Long> ids)` — 2次查所有运单号 + SKU明细, 回查 product_code 填充产品名称
- `loadChannelTypeNames(Set<Long> ids)` — 1次查所有渠道类型名称

结果存入 Map 做 O(1) 关联, 总计 ~7 次查询, 不随数据量增加。

### 销售账户绑定

- 销售账户通过 `sales_account_user_binding` 与 `sys_user` 多对多关联
- 编辑销售账户时可在弹窗中绑定/解绑用户，绑定的用户即为可使用该账户的销售人员
- 创建订单时, 用户选择自己的销售账户 → 系统根据 `customer_sales_account_binding` 过滤该账户已绑定的客户

### Key Data Relationships

```
sys_role ─1:N→ sys_user ─1:N→ sales_account_user_binding ─M:1→ sales_account
                                                                   │ (M:N through customer_sales_account_binding)
                                                                   └───────── customer
sys_user ─1:N→ sales_order (sales_person_id 记录谁下的单)
sales_account ─1:N→ sales_order (sales_account_id 记录用哪个销售账户下的单)
sales_order ─1:N→ sales_order_item ─N:1→ product
sales_order ─1:N→ sales_order_payment ─M:1→ payment_channel_type
sales_order ─1:N→ order_audit_log
sales_order ─1:N→ order_tracking ─1:N→ order_tracking_item (运单号 + SKU明细)
customer ─1:N→ customer_payment_channel
customer ─1:N→ customer_contact
customer ─1:N→ customer_shipping_address
```

### 运单号导入

- **数据模型**: `order_tracking` (运单号主表, 含妥投金额/发货类型/发货时间) 1:N `order_tracking_item` (SKU明细, 含产品SKU编码+数量)
- **前端页面**: `admin-tools/TrackingImport.vue`, Excel 拖拽上传 → `xlsx` 库解析 → 预览表格 → 确认导入
- **Excel 列顺序**: 订单号 | 运单号 | 发货类型 | 发货时间 | 产品SKU | 数量 | 妥投金额
- **必填校验**: 订单号、运单号、产品SKU、数量为必填; 发货类型填"洗护"或不填; 妥投金额不填默认0; 发货时间不填默认系统时间
- **后端逻辑**: 按 (orderNo, trackingNo) 分组 → 校验订单状态(APPROVED/SHIPPED/DELIVERED) → 插主表+子表 → APPROVED 自动变更为 SHIPPED → 写审计日志
- **订单列表展示**: 订单列表 API 含运单信息, 前端展示运单号标签(洗护橙色/普通灰色) + 发货时间 + 妥投金额 + 产品名称×数量明细
- **产品名称匹配**: 运单 SKU 明细通过 `product_code` 回查 `product` 表获取产品名称, 未匹配则回退显示 SKU 编码

### API Response Format

```json
{"code": 200, "message": "操作成功", "data": {...}, "timestamp": 1778660642815}
```

错误时 code 非200, data 可能为 null 或错误详情。分页接口返回 `{records: [...], total: N}`。

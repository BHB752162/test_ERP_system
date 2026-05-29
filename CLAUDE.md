# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

时黛王妃业务系统 — 订单管理 + 顾客管理 + 产品管理。

- **后端**: Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + Spring Security + JWT (jjwt 0.11.5), JDK 17
- **前端**: Vue 3.4 + Vite 5 + Element Plus 2.5 + Pinia 2.1 + Vue Router 4.3 + Axios 1.6
- **数据库**: MySQL 8.0+, 19 张 InnoDB 表, utf8mb4
- **项目路径**: `E:\project\test_ERP_system\`

## Run Commands

### 开发模式 (单人开发/调试)

```bash
# === 启动 MySQL ===
sc start MySQL84

# === 后端 (port 8080) ===
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
export MAVEN_HOME="/c/Program Files/Maven/apache-maven-3.9.15"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
cd E:/project/test_ERP_system/erp-backend && mvn spring-boot:run

# === 前端 (port 3000, Vite HMR) ===
cd E:/project/test_ERP_system/erp-frontend && npm run dev
```

### 生产模式 (50 人并发, 推荐日常使用)

```bash
# === 1. 启动 MySQL ===
sc start MySQL84

# === 2. 后端 jar 包 (port 8080) ===
# 首次或配置变更后需重新打包: mvn package -DskipTests
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
java -Xms256m -Xmx512m -jar E:/project/test_ERP_system/erp-backend/target/erp-system.jar

# === 3. 前端构建 ===
cd E:/project/test_ERP_system/erp-frontend && npm run build

# === 4. 启动 Nginx (port 80) ===
# 下载 nginx Windows 版: https://nginx.org/en/download.html (推荐 Stable 版)
# 解压后覆盖配置:
cp E:/project/test_ERP_system/nginx.conf /c/nginx/nginx-1.30.2/conf/nginx.conf
/c/nginx/nginx-1.30.2/nginx.exe -p /c/nginx/nginx-1.30.2/

# 重新加载配置（不中断服务）: /c/nginx/nginx-1.30.2/nginx.exe -s reload
# 停止: /c/nginx/nginx-1.30.2/nginx.exe -s stop
```

**生产模式访问**: `http://localhost` (80 端口，无需加端口号)

登录账号: `admin` / `admin123`

### Docker 部署模式 (交付/跨平台部署)

三容器架构: `erp-frontend` (nginx:alpine) → `/api` 反向代理 → `erp-backend` (Spring Boot) → `erp-mysql` (MySQL 8.4)

**docker/ 目录结构:**

| 文件 | 说明 |
|------|------|
| `docker/.env` | 环境变量 (数据库密码、JWT密钥、端口) |
| `docker/docker-compose.yml` | 三容器编排 (MySQL healthcheck + depends_on) |
| `docker/init.sql` | 数据库初始化 (19张表按依赖顺序排列 + 种子数据) |
| `docker/backend/Dockerfile` | 后端镜像 (eclipse-temurin:17-jre-alpine) |
| `docker/frontend/Dockerfile` | 前端镜像 (nginx:alpine) |
| `docker/frontend/nginx.conf` | Docker 版 nginx 配置 (proxy_pass http://backend:8080) |
| `docker/.dockerignore` | 构建排除项 |
| `docker/DOCKER部署指南.md` | 完整部署指南 (打包/部署/配置/常见问题) |

**打包 (开发电脑):**

```bash
# 1. 后端 jar
cd erp-backend && mvn clean package -DskipTests

# 2. 前端 dist
cd erp-frontend && npm run build

# 3. 构建 Docker 镜像
docker build -t erp-backend:1.0.0 -f docker/backend/Dockerfile .
docker build -t erp-frontend:1.0.0 -f docker/frontend/Dockerfile .

# 4. 导出镜像 + 交付物打包
docker save -o erp-images.tar erp-backend:1.0.0 erp-frontend:1.0.0
```

**部署 (目标电脑):**

```bash
docker load -i erp-images.tar
docker compose up -d
# 访问 http://localhost, 账号 admin / admin123
```

- MySQL 数据持久化在 Docker 命名卷 `mysql-data`，`docker compose down` 不会丢失数据
- `docker compose down -v` 会删除所有数据，谨慎执行
- 详细说明: `docker/DOCKER部署指南.md` / `Docker打包与部署说明.txt`

## Architecture

### Backend Package Layout (`com.erp`)

- **`common/`** — 全局配置: SecurityConfig (URL角色拦截), MyMetaObjectHandler (自动填充时间), GlobalExceptionHandler, ApiResponse/PageResult 统一响应, MyBatisPlusConfig (分页插件 + 乐观锁插件)
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
| `order` | sales_order, sales_order_item, sales_order_payment, order_tracking, order_tracking_item | 下单 + 审批工作流 + 收款记录 + 运单号导入/查询 + 多条件筛选 |
| `audit` | order_audit_log, customer_audit_log | 审批日志查询 + 顾客审计日志 |
| `paymentdashboard` | — | 支付看板: 按收款/审批时间汇总各渠道金额 + Excel导出 |

**模块路径端点:**

| 子资源 | 端点前缀 | 说明 |
|--------|---------|------|
| 付款渠道 | `/api/customers/{cid}/payment-channels` | CRUD |
| 联系人 | `/api/customers/{cid}/contacts` | CRUD |
| 收件地址 | `/api/customers/{cid}/shipping-addresses` | CRUD, 默认地址互斥 |
| 渠道类型 | `/api/payment-channel-types` | 独立CRUD, POST/PUT/DELETE仅 ADMIN |
| 销售账户 | `/api/sales-accounts` | 独立CRUD, 仅 ADMIN; 编辑时可绑定/解绑用户 |
| 支付看板 | `/api/payment-dashboard` | summary / by-approval-time / export-by-approval-time, 仅 ADMIN |

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
/api/orders/tracking/** → hasRole('ADMIN')
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
- **妥投金额驱动状态**: 运单号妥投金额总数 vs 收款金额决定订单状态:
  - `totalDelivery == totalPayment` → DELIVERED (自动确认妥投)
  - `totalDelivery < totalPayment` → SHIPPED (保持已发货)
  - `totalDelivery > totalPayment` → 阻止操作并报错（导入/编辑时校验）
  - 删除运单号后重新计算，无可发货记录则回退到 APPROVED
  - 手动点击"确认妥投"时也校验金额一致性，不一致则拒绝
- 订单保存字段: submittedAt(提交审批时间), approvedAt(审批通过时间), updatedBy(最后更新人), tag(标签: 空=无/DELAYED=延迟发货/WASH_CARE=洗护订单), mallOrderInfo(关联商场订单号)
- 收件地址快照: 创建/编辑订单时从 customer_shipping_address 快照到 sales_order(recipient_name/phone/address)，收件地址为必填项
- 订单编辑: SAVED/REJECTED 状态可编辑（弹窗复用 OrderCreateDialog），编辑时校验妥投金额≤收款金额，已提交审批后不可编辑

### Order Controller Endpoints

```
GET    /api/orders              → 订单列表 (分页 + 多条件筛选, 含运单信息)
                                  Query: OrderQueryDTO (orderNo, customerName, wechatAccount,
                                  recipientPhone, recipientName, salesPersonName, tag, status,
                                  salesAccountId, createdStart/End, submittedStart/End, customerId)
GET    /api/orders/{id}         → 订单详情
POST   /api/orders              → 创建订单 (保存草稿)
PUT    /api/orders/{id}         → 更新订单 (仅 SAVED/REJECTED)
POST   /api/orders/{id}/submit  → 提交审批
POST   /api/orders/{id}/approve → 审批通过
POST   /api/orders/{id}/reject  → 驳回 (JSON body: {comment})
POST   /api/orders/{id}/cancel  → 取消 (仅 SAVED/REJECTED)
POST   /api/orders/{id}/ship    → 发货 (手动)
POST   /api/orders/{id}/deliver → 确认妥投 (校验妥投金额=收款金额)
POST   /api/orders/{id}/refund  → 退款
GET    /api/orders/export       → 导出Excel (mode=order|product, 复用 OrderQueryDTO 筛选)
POST   /api/orders/import-tracking  → 运单号导入 (ADMIN, JSON数组)
GET    /api/orders/{id}/tracking     → 查询订单运单号列表
DELETE /api/orders/tracking/{trackingId} → 删除运单号 (ADMIN, 重算状态)
```

### Order Query (OrderQueryDTO)

订单列表和导出均使用 `OrderQueryDTO` 统一参数绑定，支持以下筛选字段:

| 字段 | 类型 | 匹配方式 |
|------|------|---------|
| `orderNo` | String | 模糊 (sales_order.order_no) |
| `customerName` | String | 跨表模糊 → customer.customer_name → customerId IN |
| `wechatAccount` | String | 跨表模糊 → customer.wechat_account → customerId IN |
| `salesPersonName` | String | 跨表模糊 → sys_user.real_name → salesPersonId IN |
| `recipientName` | String | 模糊 (sales_order.recipient_name) |
| `recipientPhone` | String | 模糊 (sales_order.recipient_phone) |
| `tag` | String | 精确 (DELAYED / WASH_CARE) |
| `status` | String | 精确 (状态枚举值) |
| `salesAccountId` | Long | 精确 |
| `customerId` | Long | 精确 (从顾客页路由过来) |
| `createdStart/End` | LocalDate | 日期范围 |
| `submittedStart/End` | LocalDate | 日期范围 |

所有条件 AND 叠加，通过 `buildOrderQueryWrapper()` 统一构建。

### Payment Dashboard Endpoints

```
GET /api/payment-dashboard/summary                   → 今日/7天/30天各渠道收款汇总
GET /api/payment-dashboard/by-approval-time          → 按审批时间范围统计 (startDate, endDate)
GET /api/payment-dashboard/export-by-approval-time   → 导出收款明细Excel (订单号/状态/提交时间/销售账号/支付渠道/金额)
```

### Frontend Structure (`src/`)

- **`api/`** — Axios 封装 (`request.js`) + `download.js` (blob下载) + 每个模块的 API 函数
- **`router/`** — 路由懒加载 + 导航守卫 (未登录→/login; 角色不够→/dashboard)
- **`store/`** — Pinia auth store (token 持久化到 localStorage, key `erp_auth`)
- **`layout/`** — AppLayout (Sidebar + Navbar + router-view)
- **`views/`** — 每个功能一个文件夹; 含 `profile/` (个人信息), `channel-type/` (渠道类型管理), `sales-account/` (销售账户管理), `admin-tools/` (管理员工具: 运单号导入), `order/OrderCreateDialog.vue` (创建/编辑订单弹窗), `order/TrackingDetail.vue` (运单详情), `payment-dashboard/PaymentDashboard.vue` (支付看板, 仅ADMIN)
- **`components/`** — Pagination, StatusTag 全局组件
- **`composables/`** — `useCrudList` (通用列表加载体/分页/搜索/重置)

### Frontend Patterns

- `request.js` 拦截器: 请求加 `Authorization: Bearer` header; 响应处理统一错误弹窗, 401 时清除 `token` 和 `erp_auth` (Pinia持久化key) 后跳转登录
- `download.js`: 使用 `fetch` + `Authorization` header 绕过 Axios JSON 拦截器, 直接处理 blob 下载
- `router/index.js` navigation guard: 检查 token, 按需调用 `fetchUserInfo()` 恢复用户状态, 校验 `meta.roles`
- `Sidebar.vue` 菜单顺序: 仪表盘 → 订单管理(仅订单列表) → 顾客管理 → 产品管理(ADMIN) → 系统管理(销售账户+渠道类型+用户管理+支付看板) → 审批管理 → 管理员工具(ADMIN: 运单号导入)
- `Navbar.vue` 头像下拉: "个人信息"跳转 /profile
- 创建订单: **弹窗形式** (`OrderCreateDialog.vue`), 在订单列表页打开, 不作为独立路由页面; 含「新增顾客」按钮, 通过 `window.open` + `postMessage` 跨标签页通信, 新建顾客后自动回选
- 顾客创建页: `from=order` 参数控制返回按钮用 `window.close()` 而非 `router.back()`(新标签页无历史记录), 创建成功后通过 `postMessage` 回传顾客ID/名称给父窗口
- 用户管理页: 工号代替用户名, 含创建人/更新人/更新时间审计列, 操作列含编辑/重置密码/删除
- 仪表盘: 统计数据过滤逻辑删除(status=0)的记录

### Audit Pattern

多数表包含审计字段: `created_by` (BIGINT), `updated_by` (BIGINT), `created_at`, `updated_at`。在后端 Service 中通过 `SecurityUtils.getCurrentUserId()` 手动设置，配合批量查询 (`sysUserMapper.selectBatchIds`) 加载 `createdByName`/`updatedByName` 用于前端展示。DTO 中通过 `@TableField(exist = false)` 标记瞬态姓名字段。

- **customer_audit_log**: 记录顾客信息变更 (customer_id, field_name, old_value, new_value, action, operator_id, operated_at)。用于微信号修改频率控制等审计场景。
- **order_audit_log**: 记录订单状态变更 (order_id, action, operator_id, comment, operated_at)。
- **微信号修改限制**: 非 ADMIN 用户每年仅可修改一次顾客微信号。通过查询 `customer_audit_log` 中 field_name='微信号' 的最近一次 UPDATE 记录判断，365天内有过修改则拒绝。管理员不受限。

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
- **后端逻辑 (两阶段)**:
  1. 按 (orderNo, trackingNo) 分组 → 校验订单状态(APPROVED/SHIPPED/DELIVERED) → 校验所有SKU存在 → 计算已有+新增妥投总额 vs 收款金额(超过则报错)
  2. 插主表+子表 → 根据金额对比自动更新状态(见妥投金额驱动状态规则) → 写审计日志
- **运单号删除**: ADMIN 可删除, 删除后重算剩余妥投金额 vs 收款, 自动调整订单状态(无记录则回退APPROVED)
- **订单列表展示**: 订单列表 API 含运单信息, 前端展示运单号标签(洗护橙色/普通灰色) + 发货时间 + 妥投金额 + 产品名称×数量明细
- **产品名称匹配**: 运单 SKU 明细通过 `product_code` 回查 `product` 表获取产品名称, 未匹配则回退显示 SKU 编码

### 数据库约束 (37 个: 8 UNIQUE + 29 FOREIGN KEY)

2026-05-29 完成约束全面加固，新增 3 个 UNIQUE + 7 个 FOREIGN KEY:

**新增 UNIQUE 约束:**

| 表 | 约束 | 说明 |
|-----|------|------|
| `sales_account` | `uk_account_name` | 账户名全局唯一 |
| `customer` | `uk_wechat_account` | 微信号全局唯一（NULL 允许重复） |
| `order_tracking` | `uk_order_tracking (order_id, tracking_no)` | 同一订单不能重复导入同运单号 |
| `customer_shipping_address` | `uk_customer_default_address (default_marker)` | 虚拟生成列: `IF(is_default=1, customer_id, NULL)`，保证每客户最多一个默认地址 |

**新增 FOREIGN KEY:**

| 表 | 约束 | 引用 |
|-----|------|------|
| `sales_order` | `fk_order_sales_account` | `sales_account(id)` |
| `sales_order` | `fk_order_shipping_address` | `customer_shipping_address(id)` |
| `sales_order` | `fk_order_payment_channel_type` | `payment_channel_type(id)` |
| `sales_order` | `fk_order_updated_by` | `sys_user(id)` |
| `sales_account` | `fk_sales_account_created_by/updated_by` | `sys_user(id)` |
| `product` | `fk_product_created_by/updated_by/parent` | `sys_user(id)` / `product(id)` |

**迁移脚本:** `erp-backend/src/main/resources/db/migration_20260529.sql` (约束) + `migration_20260529_concurrency.sql` (version列+默认地址虚拟列)

### 并发安全机制

**乐观锁 (Optimistic Locking):**

- `sales_order` 表新增 `version INT NOT NULL DEFAULT 0` 列，实体类 `SalesOrder.version` 标注 `@Version`
- `MyBatisPlusConfig` 注册 `OptimisticLockerInnerInterceptor` 插件
- `updateById` 自动生成 `UPDATE ... WHERE id=? AND version=?`，版本不匹配时影响 0 行
- `OrderServiceImpl.updateOrderWithVersion()` 辅助方法检测 0 行 → 抛出 `BusinessException("订单已被其他操作修改，请刷新后重试")`
- 覆盖 12 个调用点: `submitForApproval`, `approveOrder`, `rejectOrder`, `cancelOrder`, `shipOrder`, `deliverOrder`, `refundOrder`, `updateOrder`, `importTracking`(状态变更), `deleteTracking`(状态变更)

**事务保护:**

- `CustomerServiceImpl.update` 加 `@Transactional`，将 select→微信号校验→update→审计日志纳入同一事务，缩小竞态窗口
- `BindingServiceImpl.create` 加 `@Transactional`，binding insert + audit log insert 原子化
- `SalesAccountServiceImpl.create`、`CustomerServiceImpl.create`、`ProductServiceImpl.create` 加 `@Transactional`

**DuplicateKeyException 兜底:**

所有 create 方法的 `insert` 调用包裹 `try-catch(DuplicateKeyException)`，并发绕过应用层查重时给出友好中文提示而非原始 SQL 错误:
- 销售账户名已存在 / 该微信号已被其他顾客使用 / 产品编码已存在 / 该顾客已绑定此销售账户

**关键并发风险已知局限:**

- 通用实体编辑 (顾客/产品/销售账户) 的 "selectById → 修改 → updateById" 模式仍未加乐观锁，两个操作员同时编辑同一实体时后保存者静默覆盖前者
- 订单号生成 `synchronized` + `AtomicInteger` 仅在单实例部署安全，多实例需改用数据库序列

### API Response Format

```json
{"code": 200, "message": "操作成功", "data": {...}, "timestamp": 1778660642815}
```

错误时 code 非200, data 可能为 null 或错误详情。分页接口返回 `{records: [...], total: N}`。

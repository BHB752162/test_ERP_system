# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

电商ERP系统 — 订单管理 + 顾客管理 + 产品管理。

- **后端**: Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + Spring Security + JWT (jjwt 0.11.5), JDK 17
- **前端**: Vue 3.4 + Vite 5 + Element Plus 2.5 + Pinia 2.1 + Vue Router 4.3 + Axios 1.6
- **数据库**: MySQL 8.0+, 15 张 InnoDB 表 + 4 个 ALTER TABLE, utf8mb4
- **项目路径**: `E:\project\test_ERP_system\`

## Run Commands

```bash
# === 启动 MySQL ===
# 手动启动 (Windows 服务未注册时):
"C:/Program Files/MySQL/MySQL Server 8.4/bin/mysqld" --standalone --console

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
| `salesaccount` | sales_account | 销售账户管理, 增改查, 账户类型(微信等) |
| `wechat` | sales_wechat | 销售微信号 CRUD, 一个销售可多个 |
| `customer` | customer, customer_payment_channel, customer_contact, customer_shipping_address, payment_channel_type | 顾客信息 + 付款渠道 + 联系人 + 收件地址 + 渠道类型管理(含审计字段) |
| `product` | product | 产品（分类已移除） |
| `binding` | customer_wechat_binding | 微信号↔顾客多对多绑定 |
| `order` | sales_order, sales_order_item | 下单 + 审批工作流 |
| `audit` | order_audit_log | 审批日志查询 |

**顾客模块子资源端点:**

| 子资源 | 端点前缀 | 说明 |
|--------|---------|------|
| 付款渠道 | `/api/customers/{cid}/payment-channels` | CRUD |
| 联系人 | `/api/customers/{cid}/contacts` | CRUD |
| 收件地址 | `/api/customers/{cid}/shipping-addresses` | CRUD, 默认地址互斥 |
| 渠道类型 | `/api/payment-channel-types` | 独立CRUD, POST/PUT/DELETE仅 ADMIN/MANAGER |
| 销售账户 | `/api/sales-accounts` | 独立CRUD, ADMIN/MANAGER |

### Security Rules (SecurityConfig.java + @PreAuthorize)

```
POST /api/auth/login            → permitAll
GET /api/auth/user-info         → authenticated (获取个人信息)
PUT /api/auth/user-info         → authenticated (更新个人信息)
POST /api/auth/change-password  → authenticated (修改密码, 需原密码)
POST /api/auth/verify-password  → authenticated (验证原密码)
/api/users/**, /api/roles/**    → hasRole('ADMIN')
/api/sales-bindings/**          → hasAnyRole('ADMIN', 'SALES_MANAGER')
/api/orders/*/approve|reject    → hasAnyRole('ADMIN', 'SALES_MANAGER')
/api/sales-accounts/**          → hasAnyRole('ADMIN', 'SALES_MANAGER')
POST/PUT/DELETE /api/payment-channel-types → @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
所有其他 /api/**                → authenticated
无状态会话, JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前
```

### Order State Machine

```
DRAFT → (提交) → PENDING_APPROVAL → (通过) → APPROVED → (完成) → COMPLETED
                                    → (驳回) → REJECTED
DRAFT / PENDING_APPROVAL → (取消) → CANCELLED
```

- 权限: SALES_PERSON 只能操作自己的订单; ADMIN/SALES_MANAGER 可看全部
- 下单核心校验: 选择微信号 → 该微信号必须已绑定所选顾客 (customer_wechat_binding)
- 订单号生成: `B`+yyMMdd+6位顺序号 (每日重置)
- 操作记录: 每次状态变更写入 order_audit_log
- 订单保存字段: submittedAt(提交审批时间), approvedAt(审批通过时间), updatedBy(最后更新人), tag(标签: 空=无/DELAYED=延迟发货), mallOrderInfo(关联商场订单号)
- 收件地址快照: 创建订单时从 customer_shipping_address 快照到 sales_order(recipient_name/phone/address)

### Frontend Structure (`src/`)

- **`api/`** — Axios 封装 + 每个模块的 API 函数
- **`router/`** — 路由懒加载 + 导航守卫 (未登录→/login; 角色不够→/dashboard)
- **`store/`** — Pinia auth store (token 持久化到 localStorage)
- **`layout/`** — AppLayout (Sidebar + Navbar + router-view)
- **`views/`** — 每个功能一个文件夹, 按功能拆分列表/表单/详情; 另含 `profile/` (个人信息), `channel-type/` (渠道类型管理)
- **`components/`** — Pagination, StatusTag 全局组件

### Frontend Pattern

- `request.js` 拦截器: 请求加 `Authorization: Bearer` header; 响应处理统一错误弹窗, 401 自动跳转登录
- `router/index.js` navigation guard: 检查 token 和 meta.roles
- `Sidebar.vue` 菜单顺序: 仪表盘 → 订单管理 → 顾客管理 → 微信号管理 → 产品管理 → 系统管理(销售账户管理+渠道类型+用户管理) → 审批管理
- `Navbar.vue` 头像下拉: "个人信息"跳转 /profile
- 用户管理页: 工号代替用户名, 含创建人/更新人/更新时间审计列, 操作列含编辑/重置密码/删除

### Audit Pattern

多数表包含审计字段: `created_by` (BIGINT), `updated_by` (BIGINT), `created_at`, `updated_at`。在后端 Service 中通过 `SecurityUtils.getCurrentUserId()` 手动设置，配合批量查询 (`sysUserMapper.selectBatchIds`) 加载 `createdByName`/`updatedByName` 用于前端展示。DTO 中通过 `@TableField(exist = false)` 标记瞬态姓名字段。

### 销售账户绑定

销售账户通过 `sales_account_user_binding` 与 `sys_user` 多对多关联。编辑销售账户时可在弹窗中绑定/解绑用户，绑定的用户即为可使用该账户的销售人员。

### Key Data Relationships

```
sys_role ─1:N→ sys_user ─1:N→ sales_wechat
                                    │ (M:N through customer_wechat_binding)
                                    └───────── customer
sys_user ─1:N→ sales_order (sales_person_id 记录谁下的单)
sales_wechat ─1:N→ sales_order (sales_wechat_id 记录用哪个微信号下的单)
sales_order ─1:N→ sales_order_item ─N:1→ product
sales_order ─1:N→ order_audit_log
customer ─1:N→ customer_payment_channel
customer ─1:N→ customer_contact
customer ─1:N→ customer_shipping_address
```

### API Response Format

```json
{"code": 200, "message": "操作成功", "data": {...}, "timestamp": 1778660642815}
```

错误时 code 非200, data 可能为 null 或错误详情。分页接口返回 `{records: [...], total: N}`。

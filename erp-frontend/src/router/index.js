import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'customers',
        name: 'CustomerList',
        component: () => import('../views/customer/CustomerList.vue'),
        meta: { title: '顾客管理' }
      },
      {
        path: 'customers/create',
        name: 'CustomerCreate',
        component: () => import('../views/customer/CustomerForm.vue'),
        meta: { title: '新增顾客' }
      },
      {
        path: 'customers/:id/edit',
        name: 'CustomerEdit',
        component: () => import('../views/customer/CustomerForm.vue'),
        meta: { title: '编辑顾客' }
      },
      {
        path: 'customers/:id',
        name: 'CustomerDetail',
        component: () => import('../views/customer/CustomerDetail.vue'),
        meta: { title: '顾客详情' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('../views/product/ProductList.vue'),
        meta: { title: '产品管理', roles: ['ADMIN', 'SALES_MANAGER'] }
      },
      {
        path: 'products/create',
        name: 'ProductCreate',
        component: () => import('../views/product/ProductForm.vue'),
        meta: { title: '新增产品', roles: ['ADMIN', 'SALES_MANAGER'] }
      },
      {
        path: 'products/:id/edit',
        name: 'ProductEdit',
        component: () => import('../views/product/ProductForm.vue'),
        meta: { title: '编辑产品', roles: ['ADMIN', 'SALES_MANAGER'] }
      },
      {
        path: 'categories',
        name: 'CategoryList',
        component: () => import('../views/product/CategoryList.vue'),
        meta: { title: '产品分类', roles: ['ADMIN', 'SALES_MANAGER'] }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'orders/create',
        name: 'OrderCreate',
        component: () => import('../views/order/OrderForm.vue'),
        meta: { title: '创建订单' }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('../views/order/OrderDetail.vue'),
        meta: { title: '订单详情' }
      },
      {
        path: 'wechats',
        name: 'WechatList',
        component: () => import('../views/wechat/WechatList.vue'),
        meta: { title: '微信号管理', roles: ['ADMIN', 'SALES_MANAGER'] }
      },
      {
        path: 'bindings',
        name: 'BindingList',
        component: () => import('../views/binding/BindingList.vue'),
        meta: { title: '绑定管理', roles: ['ADMIN', 'SALES_MANAGER'] }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('../views/user/UserList.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'audit-logs',
        name: 'AuditLogList',
        component: () => import('../views/audit/AuditLog.vue'),
        meta: { title: '审批日志' }
      },
      {
        path: 'channel-types',
        name: 'ChannelTypeList',
        component: () => import('../views/channel-type/ChannelTypeList.vue'),
        meta: { title: '渠道类型', roles: ['ADMIN', 'SALES_MANAGER'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore()
  if (to.name !== 'Login' && !auth.token) {
    return next('/login')
  }
  // 刷新页面后 token 存在但 userInfo 为 null，需恢复用户信息
  if (auth.token && !auth.userInfo) {
    const ok = await auth.fetchUserInfo()
    if (!ok) return next('/login')
  }
  if (to.meta.roles && !auth.hasAnyRole(to.meta.roles)) {
    return next('/dashboard')
  }
  next()
})

export default router

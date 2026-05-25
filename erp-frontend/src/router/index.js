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
        meta: { title: '产品管理', roles: ['ADMIN'] }
      },
      {
        path: 'products/create',
        name: 'ProductCreate',
        component: () => import('../views/product/ProductForm.vue'),
        meta: { title: '新增产品', roles: ['ADMIN'] }
      },
      {
        path: 'products/:id/edit',
        name: 'ProductEdit',
        component: () => import('../views/product/ProductForm.vue'),
        meta: { title: '编辑产品', roles: ['ADMIN'] }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { title: '订单管理' }
      },

      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('../views/order/OrderDetail.vue'),
        meta: { title: '订单详情' }
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
        path: 'sales-accounts',
        name: 'SalesAccountList',
        component: () => import('../views/sales-account/SalesAccountList.vue'),
        meta: { title: '销售账户管理', roles: ['ADMIN'] }
      },
      {
        path: 'channel-types',
        name: 'ChannelTypeList',
        component: () => import('../views/channel-type/ChannelTypeList.vue'),
        meta: { title: '渠道类型', roles: ['ADMIN'] }
      },
      {
        path: 'admin-tools/tracking-import',
        name: 'TrackingImport',
        component: () => import('../views/admin-tools/TrackingImport.vue'),
        meta: { title: '运单号导入', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/profile/Profile.vue'),
        meta: { title: '个人信息' }
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
  // 目标不是登录页且无 token，跳转登录
  if (to.name !== 'Login' && !auth.token) {
    return next('/login')
  }
  // token 存在但 userInfo 还未加载（HMR/刷新后），尝试恢复
  if (auth.token && !auth.userInfo) {
    const ok = await auth.fetchUserInfo()
    // fetchUserInfo 失败（非401）时不破坏已有 token，导航继续
    // 页面自己的 API 调用会在 401 时由响应拦截器统一跳转登录
    if (!ok && !auth.token) {
      // clearAuth 已被调用（401），跳转登录
      return next('/login')
    }
  }
  if (to.meta.roles && !auth.hasAnyRole(to.meta.roles)) {
    return next('/dashboard')
  }
  next()
})

export default router

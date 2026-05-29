<template>
  <el-aside width="220px" class="sidebar-container">
    <div class="sidebar-logo">
      <div class="sidebar-logo-icon">
        <el-icon :size="22" color="#fff"><ShoppingCart /></el-icon>
      </div>
      <span>时黛王妃业务系统</span>
    </div>
    <el-menu
      :default-active="route.path"
      :default-openeds="openedMenus"
      background-color="#1f2d3d"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><Odometer /></el-icon>
        <span>仪表盘</span>
      </el-menu-item>

      <el-sub-menu index="order">
        <template #title>
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </template>
        <el-menu-item index="/orders">
          <el-icon><Menu /></el-icon>
          <span>订单列表</span>
        </el-menu-item>

      </el-sub-menu>

      <el-sub-menu index="customer">
        <template #title>
          <el-icon><User /></el-icon>
          <span>顾客管理</span>
        </template>
        <el-menu-item index="/customers">
          <el-icon><Menu /></el-icon>
          <span>顾客列表</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="auth.hasAnyRole(['ADMIN'])" index="product">
        <template #title>
          <el-icon><Goods /></el-icon>
          <span>产品管理</span>
        </template>
        <el-menu-item index="/products">
          <el-icon><Menu /></el-icon>
          <span>产品列表</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="auth.hasAnyRole(['ADMIN'])" index="system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/sales-accounts">
          <el-icon><Avatar /></el-icon>
          <span>销售账户管理</span>
        </el-menu-item>
        <el-menu-item index="/channel-types">
          <el-icon><Coin /></el-icon>
          <span>渠道类型</span>
        </el-menu-item>
        <el-menu-item v-if="auth.hasAnyRole(['ADMIN'])" index="/payment-dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>支付看板</span>
        </el-menu-item>
        <el-menu-item v-if="auth.hasRole('ADMIN')" index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="audit">
        <template #title>
          <el-icon><DocumentChecked /></el-icon>
          <span>审批管理</span>
        </template>
        <el-menu-item index="/audit-logs">
          <el-icon><Clock /></el-icon>
          <span>审批日志</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="auth.hasAnyRole(['ADMIN'])" index="admin-tools">
        <template #title>
          <el-icon><Tools /></el-icon>
          <span>管理员工具</span>
        </template>
        <el-menu-item index="/admin-tools/tracking-import">
          <el-icon><Van /></el-icon>
          <span>运单号导入</span>
        </el-menu-item>
      </el-sub-menu>

    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()

const openedMenus = computed(() => {
  const path = route.path
  if (path.startsWith('/orders')) return ['order']
  if (path.startsWith('/customers')) return ['customer']
  if (path.startsWith('/products')) return ['product']
  if (path.startsWith('/channel-types') || path.startsWith('/users') || path.startsWith('/sales-accounts') || path.startsWith('/payment-dashboard')) return ['system']
  if (path.startsWith('/audit')) return ['audit']
  if (path.startsWith('/admin-tools')) return ['admin-tools']
  return []
})
</script>

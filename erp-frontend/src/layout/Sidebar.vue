<template>
  <el-aside width="220px" style="background: #304156">
    <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid #3a4a5e">
      电商ERP系统
    </div>
    <el-menu
      :default-active="route.path"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
      style="border-right: none"
    >
      <el-menu-item index="/dashboard">
        <el-icon><Odometer /></el-icon>
        <span>仪表盘</span>
      </el-menu-item>

      <el-sub-menu index="customer">
        <template #title>
          <el-icon><User /></el-icon>
          <span>顾客管理</span>
        </template>
        <el-menu-item index="/customers">顾客列表</el-menu-item>
        <el-menu-item index="/customers/create">新增顾客</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="product">
        <template #title>
          <el-icon><Goods /></el-icon>
          <span>产品管理</span>
        </template>
        <el-menu-item index="/products">产品列表</el-menu-item>
        <el-menu-item index="/categories">产品分类</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="wechat">
        <template #title>
          <el-icon><ChatDotRound /></el-icon>
          <span>微信号管理</span>
        </template>
        <el-menu-item index="/wechats">微信号列表</el-menu-item>
        <el-menu-item index="/bindings">绑定管理</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="order">
        <template #title>
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </template>
        <el-menu-item index="/orders">订单列表</el-menu-item>
        <el-menu-item index="/orders/create">创建订单</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="audit">
        <template #title>
          <el-icon><DocumentChecked /></el-icon>
          <span>审批管理</span>
        </template>
        <el-menu-item index="/audit-logs">审批日志</el-menu-item>
      </el-sub-menu>

      <el-menu-item v-if="auth.hasRole('ADMIN')" index="/users">
        <el-icon><Setting /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()
</script>

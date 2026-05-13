<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; gap: 16px">
            <el-icon :size="40" color="#409eff"><User /></el-icon>
            <div>
              <div style="font-size: 12px; color: #909399">顾客总数</div>
              <div style="font-size: 24px; font-weight: bold; margin-top: 4px">--</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; gap: 16px">
            <el-icon :size="40" color="#67c23a"><Goods /></el-icon>
            <div>
              <div style="font-size: 12px; color: #909399">产品总数</div>
              <div style="font-size: 24px; font-weight: bold; margin-top: 4px">--</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; gap: 16px">
            <el-icon :size="40" color="#e6a23c"><List /></el-icon>
            <div>
              <div style="font-size: 12px; color: #909399">订单总数</div>
              <div style="font-size: 24px; font-weight: bold; margin-top: 4px">--</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; gap: 16px">
            <el-icon :size="40" color="#f56c6c"><ChatDotRound /></el-icon>
            <div>
              <div style="font-size: 12px; color: #909399">微信号数</div>
              <div style="font-size: 24px; font-weight: bold; margin-top: 4px">--</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>欢迎使用电商ERP系统</span>
      </template>
      <div style="color: #606266; line-height: 2">
        <p><strong>当前用户：</strong>{{ auth.userInfo?.realName || auth.userInfo?.username }}</p>
        <p><strong>当前角色：</strong>{{ auth.userInfo?.roleName }}</p>
        <p><strong>系统功能：</strong>顾客管理 / 产品管理 / 微信号管理 / 订单管理 / 审批管理</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useAuthStore } from '../../store/auth'
const auth = useAuthStore()
</script>

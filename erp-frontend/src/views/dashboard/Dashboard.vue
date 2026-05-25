<template>
  <div v-loading="loading">
    <el-row :gutter="24">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div style="display: flex; align-items: center; gap: 16px">
            <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #337ecc)">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div>
              <div class="stat-label">顾客总数</div>
              <div class="stat-value" style="color: #409eff">{{ stats.customerCount ?? '--' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div style="display: flex; align-items: center; gap: 16px">
            <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a, #529b2e)">
              <el-icon :size="24"><Goods /></el-icon>
            </div>
            <div>
              <div class="stat-label">产品总数</div>
              <div class="stat-value" style="color: #67c23a">{{ stats.productCount ?? '--' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div style="display: flex; align-items: center; gap: 16px">
            <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #cf9236)">
              <el-icon :size="24"><List /></el-icon>
            </div>
            <div>
              <div class="stat-label">订单总数</div>
              <div class="stat-value" style="color: #e6a23c">{{ stats.orderCount ?? '--' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="flex-between">
              <span>欢迎使用电商ERP系统</span>
              <el-tag size="small" type="info" effect="plain">v1.0</el-tag>
            </div>
          </template>
          <div style="color: #606266; line-height: 2.2; font-size: 14px">
            <p>
              <el-icon style="margin-right: 6px; vertical-align: middle"><UserFilled /></el-icon>
              <strong>当前用户：</strong>{{ auth.userInfo?.realName || auth.userInfo?.username }}
            </p>
            <p>
              <el-icon style="margin-right: 6px; vertical-align: middle"><Avatar /></el-icon>
              <strong>当前角色：</strong>
              <el-tag :type="roleTagType" size="small" style="vertical-align: middle">
                {{ auth.userInfo?.roleName }}
              </el-tag>
            </p>
            <p>
              <el-icon style="margin-right: 6px; vertical-align: middle"><Tools /></el-icon>
              <strong>系统功能：</strong>顾客管理 / 产品管理 / 订单管理 / 审批管理
            </p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>快捷入口</span>
          </template>
          <div style="display: flex; flex-direction: column; gap: 12px">
            <el-button type="primary" size="large" style="justify-content: flex-start" @click="$router.push('/orders')">
              <el-icon style="margin-right: 8px"><Edit /></el-icon> 订单管理
            </el-button>
            <el-button type="success" size="large" style="justify-content: flex-start" @click="$router.push('/customers')">
              <el-icon style="margin-right: 8px"><User /></el-icon> 顾客管理
            </el-button>
            <el-button type="warning" size="large" style="justify-content: flex-start" @click="$router.push('/orders')">
              <el-icon style="margin-right: 8px"><List /></el-icon> 订单列表
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import { getStats } from '../../api/dashboard'

const auth = useAuthStore()
const loading = ref(false)
const stats = reactive({
  customerCount: null,
  productCount: null,
  orderCount: null
})

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', SALES_PERSON: 'success' }
  return map[auth.userInfo?.roleCode] || 'info'
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getStats()
    Object.assign(stats, res.data)
  } catch { /* handled by interceptor */ }
  finally {
    loading.value = false
  }
})
</script>

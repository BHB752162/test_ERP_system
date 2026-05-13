<template>
  <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; height: 50px">
    <div style="font-size: 16px; color: #333">
      {{ route.meta?.title || '' }}
    </div>
    <div style="display: flex; align-items: center; gap: 16px">
      <el-tag v-if="auth.userInfo" size="small" :type="roleTagType" effect="plain">
        {{ auth.userInfo.roleName }}
      </el-tag>
      <span style="font-size: 14px; color: #666">
        {{ auth.userInfo?.realName || auth.userInfo?.username }}
      </span>
      <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', SALES_MANAGER: 'warning', SALES_PERSON: 'success' }
  return map[auth.userInfo?.roleCode] || 'info'
})

function handleLogout() {
  auth.clearAuth()
  router.push('/login')
}
</script>

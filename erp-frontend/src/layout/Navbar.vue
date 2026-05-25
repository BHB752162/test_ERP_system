<template>
  <el-header class="navbar">
    <div class="navbar-left">
      <el-icon style="margin-right: 8px; color: #909399; font-size: 18px">
        <component :is="currentIcon" />
      </el-icon>
      <span class="navbar-title">{{ route.meta?.title || '' }}</span>
    </div>
    <div class="navbar-right">
      <el-tag v-if="auth.userInfo" size="small" :type="roleTagType" effect="light">
        {{ auth.userInfo.roleName }}
      </el-tag>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="navbar-user">
          <el-avatar :size="28" style="background: #409eff; vertical-align: middle">
            {{ avatarText }}
          </el-avatar>
          <span class="navbar-username">{{ auth.userInfo?.realName || auth.userInfo?.username }}</span>
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="User">
              个人信息
            </el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
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

const iconMap = {
  '仪表盘': 'Odometer',
  '顾客管理': 'User',
  '产品管理': 'Goods',
  '微信号管理': 'ChatDotRound',
  '订单管理': 'List',
  '审批管理': 'DocumentChecked',
  '用户管理': 'Setting',
}

const currentIcon = computed(() => {
  const title = route.meta?.title || ''
  return iconMap[title] || 'Menu'
})

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', SALES_PERSON: 'success' }
  return map[auth.userInfo?.roleCode] || 'info'
})

const avatarText = computed(() => {
  const name = auth.userInfo?.realName || auth.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    auth.clearAuth()
    router.push('/login')
  }
}
</script>

<style scoped>
.navbar {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 50px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
}

.navbar-left {
  display: flex;
  align-items: center;
}

.navbar-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.navbar-user:hover {
  background: #f5f7fa;
}

.navbar-username {
  font-size: 14px;
  color: #606266;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

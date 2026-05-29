<template>
  <div class="login-page">
    <div class="bg-circles">
      <span /><span /><span /><span />
    </div>
    <el-card shadow="never" class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <el-icon :size="28" color="#fff"><ShoppingCart /></el-icon>
        </div>
        <h2>时黛王妃业务系统</h2>
        <p>登录您的账户以继续</p>
      </div>
      <div class="login-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" size="large" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password clearable>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item style="margin-top: 8px">
            <el-button type="primary" size="large" style="width: 100%; height: 44px; font-size: 15px" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="login-footer">
        默认管理员: admin / admin123
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../store/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

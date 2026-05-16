<template>
  <div style="max-width: 900px; margin: 0 auto">
    <el-row :gutter="24" align="stretch">
      <!-- 左侧：个人信息展示 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" style="height: 100%">
          <template #header><span>个人信息</span></template>
          <div style="text-align: center; padding: 16px 0">
            <el-avatar :size="72" style="background: #409eff; font-size: 28px">
              {{ avatarText }}
            </el-avatar>
            <h3 style="margin: 12px 0 4px">{{ userInfo?.realName || userInfo?.username }}</h3>
            <el-tag size="small" :type="roleTagType" effect="light">{{ userInfo?.roleName }}</el-tag>
          </div>
          <el-descriptions :column="1" border style="margin-top: 8px">
            <el-descriptions-item label="工号">{{ userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="真实姓名">{{ userInfo?.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userInfo?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userInfo?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ userInfo?.roleName }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：编辑资料 + 修改密码 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header><span>编辑资料</span></template>
          <el-form ref="infoFormRef" :model="infoForm" label-width="100px" v-loading="saving">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="infoForm.realName" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveInfo">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px">
          <template #header><span>修改密码</span></template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password :disabled="pwdVerified" style="width: 200px" />
              <el-button
                v-if="!pwdVerified"
                size="small"
                type="primary"
                :loading="verifying"
                style="margin-left: 8px"
                @click="handleVerifyPwd"
              >验证</el-button>
              <el-tag v-else type="success" size="small" style="margin-left: 8px">已验证</el-tag>
            </el-form-item>
            <template v-if="pwdVerified">
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changing" @click="handleChangePwd">保存密码</el-button>
                <el-button @click="cancelPwdChange">取消</el-button>
              </el-form-item>
            </template>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../store/auth'
import { updateUserInfo, changePassword } from '../../api/auth'
import request from '../../api/request'

const auth = useAuthStore()

const userInfo = computed(() => auth.userInfo)

const avatarText = computed(() => {
  const name = userInfo.value?.realName || userInfo.value?.username || ''
  return name.charAt(0).toUpperCase()
})

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', SALES_MANAGER: 'warning', SALES_PERSON: 'success' }
  return map[userInfo.value?.roleCode] || 'info'
})

// === 基本信息编辑 ===
const infoFormRef = ref(null)
const saving = ref(false)
const infoForm = reactive({ realName: '', phone: '', email: '' })

onMounted(() => {
  if (userInfo.value) {
    infoForm.realName = userInfo.value.realName || ''
    infoForm.phone = userInfo.value.phone || ''
    infoForm.email = userInfo.value.email || ''
  }
})

async function handleSaveInfo() {
  const valid = await infoFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateUserInfo(infoForm)
    if (auth.userInfo) {
      auth.userInfo.realName = infoForm.realName
      auth.userInfo.phone = infoForm.phone
      auth.userInfo.email = infoForm.email
    }
    ElMessage.success('保存成功')
  } catch { /* handled by interceptor */ }
  finally { saving.value = false }
}

// === 修改密码 ===
const pwdFormRef = ref(null)
const verifying = ref(false)
const changing = ref(false)
const pwdVerified = ref(false)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function handleVerifyPwd() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  verifying.value = true
  try {
    await request.post('/auth/verify-password', { oldPassword: pwdForm.oldPassword })
    pwdVerified.value = true
    ElMessage.success('原密码验证通过')
  } catch { /* handled by interceptor */ }
  finally { verifying.value = false }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  changing.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    cancelPwdChange()
  } catch { /* handled by interceptor */ }
  finally { changing.value = false }
}

function cancelPwdChange() {
  pwdVerified.value = false
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.resetFields()
}
</script>

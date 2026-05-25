<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="工号">
          <el-input v-model="query.keyword" placeholder="工号/姓名" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-actions">
        <span></span>
        <el-button type="primary" @click="showDialog(null)">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 新增用户
        </el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="error || (query.keyword ? '没有找到匹配的用户' : '暂无用户数据')" />
        </template>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="工号" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="roleName" label="角色" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column prop="createdByName" label="创建人员" min-width="100" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column prop="updatedByName" label="更新人员" min-width="100" />
        <el-table-column label="操作" width="260" fixed="right" :resizable="false">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-popconfirm title="确认将密码重置为 123456？" @confirm="handleResetPwd(row.id)">
              <template #reference>
                <el-button type="warning" link>重置密码</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px" @close="closeDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="工号" prop="username">
          <el-input v-model="formData.username" :disabled="isEdit" />
        </el-form-item>
	        <el-form-item v-if="!isEdit" label="密码" prop="password">
	          <el-input v-model="formData.password" type="password" show-password />
	        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="formData.roleId" placeholder="请选择" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :value="r.id" :label="r.roleName" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsers, createUser, updateUser, deleteUser, listRoles, resetPassword } from '../../api/user'
import { useCrudList, useDeleteAction } from '../../composables/useCrudList'
import { ENABLE_STATUS_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, error, query, fetchData, search, reset, onPageChange } = useCrudList(listUsers, { defaultQuery: { keyword: '' } })
const { handleDelete } = useDeleteAction(deleteUser, fetchData)

async function handleResetPwd(id) {
  try {
    await resetPassword(id)
    ElMessage.success('密码已重置为 123456')
  } catch { /* handled by interceptor */ }
}

const roles = ref([])
const submitting = ref(false)
const formRef = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)

const defaultForm = { username: '', password: '', realName: '', phone: '', email: '', roleId: null, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = reactive({
  username: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
})

function showDialog(row) {
  isEdit.value = !!row
  for (const key in formData) {
    delete formData[key]
  }
  if (row) {
    Object.assign(formData, row, { password: '' })
    rules.password = [{ min: 6, message: '密码至少6位', trigger: 'blur' }]
  } else {
    Object.assign(formData, { ...defaultForm })
    rules.password = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码至少6位', trigger: 'blur' }
    ]
  }
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...formData }
    if (isEdit.value && !data.password) delete data.password
    if (isEdit.value) {
      await updateUser(data.id, data)
    } else {
      await createUser(data)
    }
    closeDialog()
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  fetchData()
  try {
    const res = await listRoles()
    roles.value = res.data || []
  } catch { /* ignore */ }
})
</script>

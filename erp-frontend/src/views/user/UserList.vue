<template>
  <div>
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="showDialog(null)">新增用户</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column prop="roleName" label="角色" width="100" />
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="statusMap" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
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
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialog.isEdit" />
        </el-form-item>
        <el-form-item :label="dialog.isEdit ? '新密码' : '密码'" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="dialog.isEdit ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :value="r.id" :label="r.roleName" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsers, createUser, updateUser, deleteUser, listRoles } from '../../api/user'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const statusMap = { 0: { label: '禁用', type: 'danger' }, 1: { label: '启用', type: 'success' } }

const list = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])
const submitting = ref(false)
const formRef = ref(null)

const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const dialog = reactive({ visible: false, isEdit: false, editId: null })
const form = reactive({ username: '', password: '', realName: '', phone: '', email: '', roleId: null, status: 1 })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listUsers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

function showDialog(row) {
  dialog.isEdit = !!row
  dialog.editId = row?.id || null
  if (row) {
    Object.assign(form, row)
    form.password = ''
  } else {
    Object.assign(form, { username: '', password: '', realName: '', phone: '', email: '', roleId: null, status: 1 })
  }
  dialog.visible = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const submitData = { ...form }
    if (dialog.isEdit && !submitData.password) delete submitData.password
    if (dialog.isEdit) {
      await updateUser(dialog.editId, submitData)
      ElMessage.success('更新成功')
    } else {
      await createUser(submitData)
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  await deleteUser(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(async () => {
  fetchData()
  const res = await listRoles()
  roles.value = res.data || []
})
</script>

<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="销售账户">
          <el-input v-model="query.accountName" placeholder="销售账户" clearable style="width: 160px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="销售账户名称">
          <el-input v-model="query.displayName" placeholder="销售账户名称" clearable style="width: 180px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>销售账户管理</span>
          <el-button type="primary" size="small" @click="showDialog(null)">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 新增账户
          </el-button>
        </div>
      </template>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
    <el-table :data="list" border stripe v-loading="loading" row-key="id">
      <template #empty>
        <el-empty :description="error || (hasActiveFilter ? '没有找到匹配的销售账户' : '暂无数据')" />
      </template>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="accountName" label="销售账户" min-width="150" />
      <el-table-column prop="displayName" label="销售账户名称" min-width="150" />
      <el-table-column prop="accountType" label="账户类型" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.accountType === 'WECHAT'" type="success" size="small">微信</el-tag>
          <el-tag v-else size="small">{{ row.accountType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column prop="createdByName" label="创建人员" min-width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="160" />
      <el-table-column prop="updatedByName" label="更新人员" min-width="100" />
      <el-table-column label="操作" width="160" fixed="right">
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑销售账户' : '新增销售账户'" width="600px" destroy-on-close @close="closeDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="销售账户" prop="accountName">
          <el-input v-model="formData.accountName" placeholder="请输入销售账户" />
        </el-form-item>
        <el-form-item label="销售账户名称" prop="displayName">
          <el-input v-model="formData.displayName" placeholder="请输入销售账户名称" />
        </el-form-item>
        <el-form-item label="账户类型" prop="accountType">
          <el-select v-model="formData.accountType" placeholder="请选择" style="width: 100%">
            <el-option value="WECHAT" label="微信" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="绑定用户">
          <div style="width: 100%">
            <div style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-select v-model="selectedUserId" placeholder="选择用户" style="flex: 1" clearable filterable>
                <el-option
                  v-for="u in availableUsers"
                  :key="u.id"
                  :value="u.id"
                  :label="`${u.username} - ${u.realName || ''}`"
                  :disabled="boundUserIds.includes(u.id)"
                />
              </el-select>
              <el-button type="primary" :disabled="!selectedUserId" @click="handleBindUser">绑定</el-button>
            </div>
            <el-tag
              v-for="uid in boundUserIds"
              :key="uid"
              closable
              :disable-transitions="true"
              style="margin: 2px 4px 2px 0"
              @close="handleUnbindUser(uid)"
            >
              {{ getUserLabel(uid) }}
            </el-tag>
            <span v-if="boundUserIds.length === 0" style="color: #999; font-size: 13px">暂未绑定用户</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { listSalesAccounts, createSalesAccount, updateSalesAccount, deleteSalesAccount, getBoundUserIds, bindUser, unbindUser } from '../../api/salesAccount'
import { listUsers } from '../../api/user'
import { ENABLE_STATUS_MAP } from '../../constants'
import StatusTag from '../../components/StatusTag.vue'

const list = ref([])
const loading = ref(false)
const error = ref('')
const submitting = ref(false)
const formRef = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

// 搜索条件
const query = reactive({ accountName: '', displayName: '' })
const hasActiveFilter = computed(() => !!(query.accountName || query.displayName))

// 用户绑定相关
const allUsers = ref([])
const boundUserIds = ref([])
const selectedUserId = ref(null)

const defaultForm = { accountName: '', displayName: '', accountType: 'WECHAT', status: 1 }
const formData = reactive({ ...defaultForm })
const rules = {
  accountName: [{ required: true, message: '请输入销售账户', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

const userMap = computed(() => {
  const map = {}
  for (const u of allUsers.value) {
    map[u.id] = u
  }
  return map
})

const availableUsers = computed(() => {
  return allUsers.value.filter(u => u.status !== 0)
})

function getUserLabel(uid) {
  const u = userMap.value[uid]
  return u ? `${u.username} - ${u.realName || ''}` : `用户ID:${uid}`
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    if (query.accountName) params.accountName = query.accountName
    if (query.displayName) params.displayName = query.displayName
    const res = await listSalesAccounts(params)
    list.value = res.data
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  fetchData()
}

function reset() {
  query.accountName = ''
  query.displayName = ''
  fetchData()
}

async function showDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  for (const key in formData) {
    delete formData[key]
  }
  Object.assign(formData, row ? { ...row } : { ...defaultForm })
  dialogVisible.value = true

  // 加载用户列表和绑定数据
  await loadAllUsers()
  if (isEdit.value) {
    await loadBoundUsers(row.id)
  } else {
    boundUserIds.value = []
  }
}

async function loadAllUsers() {
  try {
    const res = await listUsers({ page: 1, pageSize: 999 })
    allUsers.value = res.data?.records || []
  } catch { /* ignore */ }
}

async function loadBoundUsers(accountId) {
  try {
    const res = await getBoundUserIds(accountId)
    boundUserIds.value = res.data || []
  } catch { /* ignore */ }
}

async function handleBindUser() {
  if (!selectedUserId.value) return
  if (isEdit.value) {
    try {
      await bindUser(editId.value, selectedUserId.value)
    } catch { return }
  }
  boundUserIds.value.push(selectedUserId.value)
  selectedUserId.value = null
  ElMessage.success('绑定成功')
}

async function handleUnbindUser(userId) {
  if (isEdit.value) {
    try {
      await unbindUser(editId.value, userId)
    } catch { return }
  }
  boundUserIds.value = boundUserIds.value.filter(id => id !== userId)
  ElMessage.success('已解绑')
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
  boundUserIds.value = []
  selectedUserId.value = null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!isEdit.value && boundUserIds.value.length === 0) {
    ElMessage.warning('请至少绑定一个用户')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSalesAccount(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      // 创建后再绑定用户
      const newId = (await createSalesAccount(formData)).data
      for (const userId of boundUserIds.value) {
        await bindUser(newId, userId)
      }
      ElMessage.success('创建成功')
    }
    closeDialog()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteSalesAccount(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* handled by interceptor */ }
}

onMounted(fetchData)
</script>

<style scoped>
.search-card :deep(.el-form-item) {
  margin-bottom: 14px;
  margin-right: 12px;
}
.search-card :deep(.el-form--inline .el-form-item) {
  margin-bottom: 14px;
}
</style>

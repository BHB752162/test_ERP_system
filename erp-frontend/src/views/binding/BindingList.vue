<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="微信号">
          <el-select v-model="query.wechatId" placeholder="选择微信号" filterable clearable style="width: 200px" @change="search">
            <el-option v-for="w in wechats" :key="w.id" :value="w.id" :label="`${w.wechatAccount}(${w.wechatNickname || ''})`" />
          </el-select>
        </el-form-item>
        <el-form-item label="顾客">
          <el-select v-model="query.customerId" placeholder="选择顾客" filterable clearable style="width: 200px" @change="search">
            <el-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.customerName" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-actions">
        <span></span>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 新建绑定
        </el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty description="暂无绑定数据" />
        </template>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="wechatAccount" label="微信号" width="130" />
        <el-table-column prop="wechatNickname" label="微信昵称" width="120" />
        <el-table-column prop="salesPersonName" label="所属销售" width="100" />
        <el-table-column prop="customerName" label="顾客名称" min-width="140" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="BINDING_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="绑定时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确认解绑？" @confirm="handleUnbind(row.id)">
              <template #reference>
                <el-button type="danger" link>解绑</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>

    <!-- Create Binding Dialog -->
    <el-dialog v-model="createDialogVisible" title="新建绑定" width="500px" @close="closeDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="微信号" prop="salesWechatId">
          <el-select v-model="formData.salesWechatId" placeholder="选择微信号" filterable style="width: 100%">
            <el-option v-for="w in wechats" :key="w.id" :value="w.id" :label="`${w.wechatAccount}(${w.wechatNickname || ''})`" />
          </el-select>
        </el-form-item>
        <el-form-item label="顾客" prop="customerId">
          <el-select v-model="formData.customerId" placeholder="选择顾客" filterable style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.customerName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listBindings, createBinding, unbind } from '../../api/binding'
import { listWechats } from '../../api/wechat'
import { listCustomers } from '../../api/customer'
import { useCrudList } from '../../composables/useCrudList'
import { BINDING_STATUS_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, query, fetchData, search, reset, onPageChange } = useCrudList(listBindings, {
  defaultQuery: { wechatId: null, customerId: null }
})

const wechats = ref([])
const customers = ref([])
const submitting = ref(false)
const formRef = ref(null)
const createDialogVisible = ref(false)

const formData = reactive({ salesWechatId: null, customerId: null })
const rules = {
  salesWechatId: [{ required: true, message: '请选择微信号', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择顾客', trigger: 'change' }]
}

function showCreateDialog() {
  formData.salesWechatId = null
  formData.customerId = null
  createDialogVisible.value = true
}

function closeDialog() {
  createDialogVisible.value = false
  formRef.value?.resetFields()
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createBinding(formData)
    createDialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleUnbind(id) {
  try {
    await unbind(id)
    fetchData()
  } catch { /* handled by interceptor */ }
}

onMounted(async () => {
  fetchData()
  try {
    const [wRes, cRes] = await Promise.all([
      listWechats({ page: 1, pageSize: 200 }),
      listCustomers({ page: 1, pageSize: 200 })
    ])
    wechats.value = wRes.data.records || []
    customers.value = cRes.data.records || []
  } catch { /* ignore */ }
})
</script>

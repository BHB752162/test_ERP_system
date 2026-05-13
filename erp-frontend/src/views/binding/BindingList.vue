<template>
  <div>
    <el-card>
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
          <el-button type="primary" @click="showCreateDialog">新建绑定</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="wechatAccount" label="微信号" width="130" />
        <el-table-column prop="wechatNickname" label="微信昵称" width="120" />
        <el-table-column prop="salesPersonName" label="所属销售" width="100" />
        <el-table-column prop="customerName" label="顾客名称" min-width="140" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="bindStatusMap" />
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
    <el-dialog v-model="createDialog.visible" title="新建绑定" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="微信号" prop="salesWechatId">
          <el-select v-model="form.salesWechatId" placeholder="选择微信号" filterable style="width: 100%">
            <el-option v-for="w in wechats" :key="w.id" :value="w.id" :label="`${w.wechatAccount}(${w.wechatNickname || ''})`" />
          </el-select>
        </el-form-item>
        <el-form-item label="顾客" prop="customerId">
          <el-select v-model="form.customerId" placeholder="选择顾客" filterable style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.customerName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listBindings, createBinding, unbind } from '../../api/binding'
import { listWechats } from '../../api/wechat'
import { listCustomers } from '../../api/customer'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const bindStatusMap = { 0: { label: '解绑', type: 'danger' }, 1: { label: '绑定', type: 'success' } }

const list = ref([])
const total = ref(0)
const loading = ref(false)
const wechats = ref([])
const customers = ref([])
const submitting = ref(false)
const formRef = ref(null)

const query = reactive({ wechatId: null, customerId: null, page: 1, pageSize: 10 })
const createDialog = reactive({ visible: false })
const form = reactive({ salesWechatId: null, customerId: null })
const rules = {
  salesWechatId: [{ required: true, message: '请选择微信号', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择顾客', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listBindings(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.wechatId = null; query.customerId = null; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

async function showCreateDialog() {
  form.salesWechatId = null
  form.customerId = null
  createDialog.visible = true
}

async function handleCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createBinding(form)
    ElMessage.success('绑定成功')
    createDialog.visible = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleUnbind(id) {
  await unbind(id)
  ElMessage.success('解绑成功')
  fetchData()
}

onMounted(async () => {
  fetchData()
  const [wRes, cRes] = await Promise.all([
    listWechats({ page: 1, pageSize: 999 }),
    listCustomers({ page: 1, pageSize: 999 })
  ])
  wechats.value = wRes.data.records || []
  customers.value = cRes.data.records || []
})
</script>

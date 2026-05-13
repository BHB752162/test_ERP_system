<template>
  <div>
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="客户名称">
          <el-input v-model="query.keyword" placeholder="名称/电话/邮箱" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="$router.push('/customers/create')">新增顾客</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="customerName" label="客户名称" min-width="140" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column prop="level" label="等级" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.level" :map="levelMap" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="statusMap" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/customers/${row.id}`)">详情</el-button>
            <el-button type="primary" link @click="$router.push(`/customers/${row.id}/edit`)">编辑</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listCustomers, deleteCustomer } from '../../api/customer'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const levelMap = { 0: { label: '普通', type: 'info' }, 1: { label: '银卡', type: 'success' }, 2: { label: '金卡', type: 'warning' }, 3: { label: '钻石', type: 'danger' } }
const statusMap = { 0: { label: '停用', type: 'danger' }, 1: { label: '启用', type: 'success' } }

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', page: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await listCustomers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

async function handleDelete(id) {
  await deleteCustomer(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

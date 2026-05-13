<template>
  <div>
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="query.action" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="(v, k) in actionMap" :key="k" :value="k" :label="v.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.action" :map="actionMap" />
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="comment" label="意见" min-width="200" />
        <el-table-column prop="operatedAt" label="操作时间" width="160" />
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listOrders } from '../../api/order'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const actionMap = {
  SUBMIT: { label: '提交审批', type: 'primary' },
  APPROVE: { label: '通过', type: 'success' },
  REJECT: { label: '驳回', type: 'danger' },
  CANCEL: { label: '取消', type: 'info' },
  COMPLETE: { label: '完成', type: 'success' }
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ orderNo: '', action: '', page: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.orderNo) params.keyword = query.orderNo
    const res = await listOrders(params)
    // Fetch audit logs for all orders
    const { getAuditLogs } = await import('../../api/order')
    const allLogs = []
    for (const order of (res.data.records || [])) {
      try {
        const logRes = await getAuditLogs(order.id)
        const logs = (logRes.data || []).map(l => ({
          ...l,
          orderNo: order.orderNo
        }))
        allLogs.push(...logs)
      } catch { /* skip */ }
    }
    // Filter by action if specified
    let filtered = query.action ? allLogs.filter(l => l.action === query.action) : allLogs
    // Filter by orderNo
    if (query.orderNo) {
      filtered = filtered.filter(l => l.orderNo?.includes(query.orderNo))
    }
    list.value = filtered
    total.value = filtered.length
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.orderNo = ''; query.action = ''; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

onMounted(fetchData)
</script>

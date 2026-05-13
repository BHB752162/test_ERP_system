<template>
  <div>
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="订单号">
          <el-input v-model="query.keyword" placeholder="订单号/客户名" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search">
            <el-option v-for="(v, k) in statusMap" :key="k" :value="k" :label="v.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="$router.push('/orders/create')">创建订单</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="customerName" label="客户" width="120" />
        <el-table-column prop="salesPersonName" label="销售" width="100" />
        <el-table-column label="所用微信号" width="160">
          <template #default="{ row }">
            <span v-if="row.salesWechatAccount">{{ row.salesWechatAccount }}<span v-if="row.salesWechatNickname">({{ row.salesWechatNickname }})</span></span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="finalAmount" label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ row.finalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="statusMap" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/orders/${row.id}`)">详情</el-button>
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
import { listOrders } from '../../api/order'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_APPROVAL: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: '', page: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await listOrders(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.status = ''; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

onMounted(fetchData)
</script>

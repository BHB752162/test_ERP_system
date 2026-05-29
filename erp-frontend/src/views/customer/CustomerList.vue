<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="客户名称">
          <el-input v-model="query.customerName" placeholder="客户名称" clearable style="width: 160px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="query.phone" placeholder="电话" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="微信号">
          <el-input v-model="query.wechatAccount" placeholder="微信号" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="创建人员">
          <el-input v-model="query.createdByName" placeholder="创建人姓名" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="绑定销售账户">
          <el-autocomplete
            v-model="query.salesAccountName"
            :fetch-suggestions="searchAccounts"
            placeholder="输入销售账户名称"
            clearable
            style="width: 200px"
            @select="search"
            @clear="search"
            @keyup.enter="search"
          />
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
        <el-button type="primary" @click="$router.push('/customers/create')">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 新增顾客
        </el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="error || (hasActiveFilter ? '没有找到匹配的客户' : '暂无客户数据')" />
        </template>
        <el-table-column label="ID" width="70">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/customers/${row.id}`)">{{ row.id }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="140" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="wechatAccount" label="微信号" width="140" />
        <el-table-column prop="addFriendTime" label="加粉时间" width="160" />
        <el-table-column prop="level" label="等级" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.level" :map="CUSTOMER_LEVEL_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人员" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/customers/${row.id}/edit`)">编辑</el-button>
            <el-button type="primary" link @click="openOrders(row)">
              订单<span v-if="row.orderCount" class="order-count">({{ row.orderCount > 99 ? '99+' : row.orderCount }})</span>
            </el-button>
            <el-button type="primary" link @click="showLogDialog(row)">日志</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>

    <!-- 日志对话框 -->
    <el-dialog v-model="logDialog.visible" :title="`修改日志 - ${logDialog.customerName}`" width="750px" destroy-on-close>
      <el-table :data="logDialog.logs" border stripe size="small" v-loading="logDialog.loading">
        <template #empty>
          <el-empty description="暂无修改记录" />
        </template>
        <el-table-column prop="action" label="操作" width="80">
          <template #default="{ row }">
            <el-tag :type="row.action === 'CREATE' ? 'success' : 'primary'" size="small">{{ row.action === 'CREATE' ? '创建' : '修改' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldName" label="字段" width="100" />
        <el-table-column prop="oldValue" label="旧值" min-width="120" show-overflow-tooltip />
        <el-table-column prop="newValue" label="新值" min-width="120" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="operatedAt" label="操作时间" width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import { listCustomers, listAuditLogs, listMyAccounts } from '../../api/customer'
import { listSalesAccounts } from '../../api/salesAccount'
import { useCrudList } from '../../composables/useCrudList'
import { ENABLE_STATUS_MAP, CUSTOMER_LEVEL_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const auth = useAuthStore()
const isManager = computed(() => auth.hasRole('ADMIN'))

function openOrders(row) {
  const params = new URLSearchParams()
  params.set('customerId', row.id)
  params.set('customerName', row.customerName || '')
  window.open(`/orders?${params.toString()}`, '_blank')
}

const EMPTY_QUERY = {
  customerName: '', phone: '', wechatAccount: '', createdByName: '',
  salesAccountName: ''
}

const { list, total, loading, error, query, fetchData, search, onPageChange } = useCrudList(
  (params) => listCustomers({ ...params }),
  { defaultQuery: { ...EMPTY_QUERY } }
)

const hasActiveFilter = computed(() => {
  return !!(query.customerName || query.phone || query.wechatAccount || query.createdByName || query.salesAccountName)
})

async function searchAccounts(keyword, cb) {
  if (!keyword) { cb([]); return }
  try {
    const kw = keyword.toLowerCase()
    const raw = isManager.value
      ? await listSalesAccounts({ keyword })
      : await listMyAccounts()
    const list = raw.data?.records || raw.data || []
    const filtered = isManager.value
      ? list
      : list.filter(a => (a.displayName || a.accountName || '').toLowerCase().includes(kw))
    cb(filtered.map(a => ({ value: a.displayName || a.accountName })))
  } catch { cb([]) }
}

const logDialog = reactive({ visible: false, customerName: '', logs: [], loading: false })

async function showLogDialog(row) {
  logDialog.customerName = row.customerName
  logDialog.visible = true
  logDialog.loading = true
  try {
    const res = await listAuditLogs(row.id)
    logDialog.logs = res.data || []
  } catch { /* handled by interceptor */ }
  finally { logDialog.loading = false }
}

function reset() {
  Object.assign(query, { ...EMPTY_QUERY, page: 1, pageSize: 10 })
  fetchData()
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
.order-count {
  color: #409EFF;
  font-weight: 600;
}
</style>

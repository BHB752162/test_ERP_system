<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 160px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="客户名">
          <el-input v-model="query.customerName" placeholder="客户名" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="微信号">
          <el-input v-model="query.wechatAccount" placeholder="微信号" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="销售">
          <el-input v-model="query.salesPersonName" placeholder="销售姓名" clearable style="width: 130px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="query.tag" placeholder="全部" clearable style="width: 130px" @update:model-value="search">
            <el-option label="延迟发货" value="DELAYED" />
            <el-option label="洗护订单" value="WASH_CARE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="(v, k) in ORDER_STATUS_MAP" :key="k" :value="k" :label="v.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人">
          <el-input v-model="query.recipientName" placeholder="收件人姓名" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="收件电话">
          <el-input v-model="query.recipientPhone" placeholder="收件人电话" clearable style="width: 140px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="销售账户">
          <el-autocomplete
            v-model="query.salesAccountName"
            :fetch-suggestions="searchAccounts"
            placeholder="输入销售账户名称"
            clearable
            style="width: 180px"
            @select="search"
            @clear="search"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="createdDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="submittedDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" :loading="exportingByOrder" @click="handleExport('order')">
            <el-icon style="margin-right: 4px"><Download /></el-icon>按订单导出
          </el-button>
          <el-button type="warning" :loading="exportingByProduct" @click="handleExport('product')">
            <el-icon style="margin-right: 4px"><Download /></el-icon>按产品导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert v-if="filterCustomerName" type="info" :closable="false" style="margin-top: 12px">
      <template #title>
        当前查看顾客「{{ filterCustomerName }}」的订单
        <el-button type="primary" link @click="$router.push('/orders')">清除筛选</el-button>
      </template>
    </el-alert>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-actions">
        <span></span>
        <el-button type="primary" @click="editingOrderId = null; showCreateDialog = true">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 创建订单
        </el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="error || (hasActiveFilter ? '没有找到匹配的订单' : '暂无订单数据')" />
        </template>
        <el-table-column label="订单号" width="170">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/orders/${row.id}`)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="120" />
        <el-table-column prop="salesPersonName" label="销售" width="100" />
        <el-table-column label="所用销售账户" width="160">
          <template #default="{ row }">
            <span v-if="row.salesAccountName">{{ row.salesAccountName }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="finalAmount" label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ row.finalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ORDER_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="标签" width="90">
          <template #default="{ row }">
            <StatusTag :status="row.tag || ''" :map="ORDER_TAG_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="recipientName" label="收件人" width="100" />
        <el-table-column prop="recipientPhone" label="收件人电话" width="130" />
        <el-table-column prop="recipientAddress" label="收件地址" min-width="160" show-overflow-tooltip />
        <el-table-column label="收款金额" width="110" align="right">
          <template #default="{ row }">¥{{ row.paymentAmountDisplay }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column prop="submittedAt" label="提交审批时间" width="160" />
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push({ name: 'TrackingDetail', params: { orderId: row.id }, query: { orderNo: row.orderNo } })">运单</el-button>
            <el-button v-if="['SAVED', 'REJECTED'].includes(row.status) && (row.salesPersonId === auth.userInfo?.id || isManager)" type="primary" link @click="handleSubmitOrder(row.id)">提交审批</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL' && isManager" type="success" link @click="handleApproveOrder(row.id)">通过</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL' && isManager" type="warning" link @click="showRejectDialog(row.id)">驳回</el-button>
            <el-button v-if="row.status === 'APPROVED' && isManager" type="primary" link @click="handleShipOrder(row.id)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPED' && isManager" type="success" link @click="handleDeliverOrder(row.id)">确认妥投</el-button>
            <el-button v-if="['APPROVED', 'SHIPPED', 'DELIVERED'].includes(row.status) && isManager" type="warning" link @click="handleRefundOrder(row.id)">退款</el-button>
            <el-button v-if="isManager || (['SAVED', 'REJECTED'].includes(row.status) && row.salesPersonId === auth.userInfo?.id)" type="primary" link @click="editOrder(row.id)">编辑</el-button>
            <el-button v-if="['SAVED', 'REJECTED'].includes(row.status)" type="danger" link @click="handleCancelOrder(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectVisible" title="驳回订单" width="400px" destroy-on-close>
      <el-input v-model="rejectComment" type="textarea" :rows="4" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRejectConfirm">确认驳回</el-button>
      </template>
    </el-dialog>

    <OrderCreateDialog v-model:visible="showCreateDialog" :order-id="editingOrderId" @success="editingOrderId = null; fetchData()" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, listAccountsForOrder, submitOrder, approveOrder, rejectOrder, cancelOrder, shipOrder, deliverOrder, refundOrder, exportOrders } from '../../api/order'
import { listSalesAccounts } from '../../api/salesAccount'
import { useCrudList } from '../../composables/useCrudList'
import { ORDER_STATUS_MAP, ORDER_TAG_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'
import OrderCreateDialog from './OrderCreateDialog.vue'

const route = useRoute()
const auth = useAuthStore()
const isManager = computed(() => auth.hasRole('ADMIN'))
const hasActiveFilter = computed(() => {
  return !!(query.orderNo || query.customerName || query.wechatAccount || query.salesPersonName
    || query.tag || query.status || query.recipientName || query.recipientPhone
    || query.salesAccountName || query.customerId || query.createdStart || query.submittedStart)
})

const filterCustomerId = computed(() => route.query.customerId ? Number(route.query.customerId) : undefined)
const filterCustomerName = computed(() => route.query.customerName || '')

const showCreateDialog = ref(false)
const editingOrderId = ref(null)

const HIGH_RISK_STATUSES = ['APPROVED', 'SHIPPED', 'DELIVERED', 'REFUNDED', 'CANCELLED']
const STATUS_LABEL = { APPROVED: '已审批', SHIPPED: '已发货', DELIVERED: '已妥投', REFUNDED: '已退款', CANCELLED: '已取消' }

async function editOrder(id) {
  const order = list.value.find(o => o.id === id)
  if (order && HIGH_RISK_STATUSES.includes(order.status)) {
    const label = STATUS_LABEL[order.status] || order.status
    try {
      await ElMessageBox.confirm(
        `该订单当前状态为「${label}」，编辑后将写入审计日志。确认要继续编辑吗？`,
        '高风险操作提醒',
        { confirmButtonText: '确认编辑', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }
  editingOrderId.value = id
  showCreateDialog.value = true
}

const EMPTY_QUERY = {
  orderNo: '', customerName: '', wechatAccount: '', salesPersonName: '',
  tag: '', status: '', recipientName: '', recipientPhone: '',
  salesAccountName: '', customerId: undefined,
  createdStart: '', createdEnd: '', submittedStart: '', submittedEnd: ''
}

const { list, total, loading, error, query, fetchData, search: baseSearch, onPageChange } = useCrudList(
  (params) => listOrders({ ...params }),
  { defaultQuery: { ...EMPTY_QUERY } }
)

const createdDateRange = ref(null)
const submittedDateRange = ref(null)
async function searchAccounts(keyword, cb) {
  if (!keyword) { cb([]); return }
  try {
    const kw = keyword.toLowerCase()
    const raw = isManager.value
      ? await listSalesAccounts({ keyword })
      : await listAccountsForOrder()
    const list = raw.data?.records || raw.data || []
    const filtered = isManager.value
      ? list
      : list.filter(a => (a.displayName || a.accountName || '').toLowerCase().includes(kw))
    cb(filtered.map(a => ({ value: a.displayName || a.accountName })))
  } catch { cb([]) }
}

function syncDateRangeToQuery(refVal, startKey, endKey) {
  if (refVal.value && refVal.value.length === 2) {
    query[startKey] = refVal.value[0]
    query[endKey] = refVal.value[1]
  } else {
    query[startKey] = ''
    query[endKey] = ''
  }
}

function search() {
  syncDateRangeToQuery(createdDateRange, 'createdStart', 'createdEnd')
  syncDateRangeToQuery(submittedDateRange, 'submittedStart', 'submittedEnd')
  baseSearch()
}

function handleReset() {
  Object.assign(query, { ...EMPTY_QUERY, page: 1, pageSize: 10 })
  createdDateRange.value = null
  submittedDateRange.value = null
  fetchData()
}

function applyCustomerFilter() {
  const cid = route.query.customerId ? Number(route.query.customerId) : undefined
  query.customerId = cid
}

onMounted(() => {
  applyCustomerFilter()
  fetchData()
})

watch(() => route.query.customerId, () => {
  applyCustomerFilter()
  search()
})

// Reject dialog state
const rejectOrderId = ref(null)
const rejectComment = ref('')
const rejectVisible = ref(false)

// Export state
const exportingByOrder = ref(false)
const exportingByProduct = ref(false)

async function handleExport(mode) {
  const loadingRef = mode === 'order' ? 'exportingByOrder' : 'exportingByProduct'
  const setLoading = (v) => {
    if (mode === 'order') exportingByOrder.value = v
    else exportingByProduct.value = v
  }
  setLoading(true)
  try {
    syncDateRangeToQuery(createdDateRange, 'createdStart', 'createdEnd')
    syncDateRangeToQuery(submittedDateRange, 'submittedStart', 'submittedEnd')
    const blob = await exportOrders(mode, { ...query })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const suffix = mode === 'product' ? 'by_product' : 'by_order'
    a.href = url
    a.download = `orders_${suffix}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(err?.message || '导出失败')
  } finally {
    setLoading(false)
  }
}

function showRejectDialog(orderId) {
  rejectOrderId.value = orderId
  rejectComment.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectComment.value) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    await rejectOrder(rejectOrderId.value, { comment: rejectComment.value })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function handleSubmitOrder(id) {
  try {
    await submitOrder(id)
    ElMessage.success('已提交审批')
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function handleApproveOrder(id) {
  try {
    await approveOrder(id)
    ElMessage.success('审批通过')
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function handleCancelOrder(id) {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' })
    await cancelOrder(id)
    ElMessage.success('已取消')
    fetchData()
  } catch (err) {
    if (err !== 'cancel') return
  }
}

async function handleShipOrder(id) {
  try {
    await ElMessageBox.confirm('确认发货？', '提示', { type: 'info' })
    await shipOrder(id)
    ElMessage.success('已发货')
    fetchData()
  } catch { /* cancelled */ }
}

async function handleDeliverOrder(id) {
  try {
    await ElMessageBox.confirm('确认该订单已妥投？', '提示', { type: 'success' })
    await deliverOrder(id)
    ElMessage.success('已确认妥投')
    fetchData()
  } catch { /* cancelled */ }
}

async function handleRefundOrder(id) {
  try {
    await ElMessageBox.confirm('确认对该订单进行退款？', '提示', { type: 'warning' })
    await refundOrder(id)
    ElMessage.success('已退款')
    fetchData()
  } catch { /* cancelled */ }
}
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
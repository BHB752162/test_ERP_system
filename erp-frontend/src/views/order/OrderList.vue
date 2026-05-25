<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="订单号">
          <el-input v-model="query.keyword" placeholder="订单号/客户名" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search">
            <el-option v-for="(v, k) in ORDER_STATUS_MAP" :key="k" :value="k" :label="v.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" :loading="exportingByOrder" @click="handleExport('order')">
            <el-icon style="margin-right: 4px"><Download /></el-icon>按订单导出
          </el-button>
          <el-button type="warning" :loading="exportingByProduct" @click="handleExport('product')">
            <el-icon style="margin-right: 4px"><Download /></el-icon>按产品导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

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
          <el-empty :description="error || (query.keyword || query.status ? '没有找到匹配的订单' : '暂无订单数据')" />
        </template>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="customerName" label="客户" width="120" />
        <el-table-column prop="salesPersonName" label="销售" width="100" />
        <el-table-column label="所用销售账户" width="160">
          <template #default="{ row }">
            <span v-if="row.salesAccountName">{{ row.salesAccountName }}<span v-if="row.salesAccountDisplayName">({{ row.salesAccountDisplayName }})</span></span>
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
        <el-table-column label="运单信息" min-width="260">
          <template #default="{ row }">
            <template v-if="(row.trackings || []).length">
              <div v-for="t in row.trackings" :key="t.id" style="margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px dashed #ebeef5">
                <div>
                  <el-tag size="small" :type="t.shipmentType === 'WASH_CARE' ? 'warning' : 'info'">
                    {{ t.trackingNo }}<span v-if="t.shipmentType === 'WASH_CARE'"> · 洗护</span>
                  </el-tag>
                  <span style="font-size: 12px; color: #909399; margin-left: 6px">{{ t.shippingTime || '-' }}</span>
                  <span style="font-size: 12px; font-weight: 500; margin-left: 6px">¥{{ t.deliveryAmount || 0 }}</span>
                </div>
                <div v-if="(t.items || []).length" style="margin-top: 3px; padding-left: 4px">
                  <span v-for="(item, idx) in t.items" :key="item.id" style="font-size: 12px; color: #606266">
                    <span v-if="idx > 0">，</span>{{ item.productName || item.productSku }} ×{{ item.quantity }}
                  </span>
                </div>
              </div>
            </template>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="收款渠道" width="100">
          <template #default="{ row }">
            {{ row.paymentChannelTypeName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="收款金额" width="110" align="right">
          <template #default="{ row }">¥{{ row.paymentAmountDisplay }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/orders/${row.id}`)">详情</el-button>
            <el-button v-if="['SAVED', 'REJECTED'].includes(row.status) && (row.salesPersonId === auth.userInfo?.id || isManager)" type="primary" link @click="handleSubmitOrder(row.id)">提交审批</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL' && isManager" type="success" link @click="handleApproveOrder(row.id)">通过</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL' && isManager" type="warning" link @click="showRejectDialog(row.id)">驳回</el-button>
            <el-button v-if="row.status === 'APPROVED' && isManager" type="primary" link @click="handleShipOrder(row.id)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPED' && isManager" type="success" link @click="handleDeliverOrder(row.id)">确认妥投</el-button>
            <el-button v-if="['APPROVED', 'SHIPPED', 'DELIVERED'].includes(row.status) && isManager" type="warning" link @click="handleRefundOrder(row.id)">退款</el-button>
            <el-button v-if="['SAVED', 'REJECTED'].includes(row.status) && (row.salesPersonId === auth.userInfo?.id || isManager)" type="primary" link @click="editOrder(row.id)">编辑</el-button>
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

    <OrderCreateDialog v-model:visible="showCreateDialog" :order-id="editingOrderId" @success="editingOrderId = null; fetchData" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, submitOrder, approveOrder, rejectOrder, cancelOrder, shipOrder, deliverOrder, refundOrder, exportOrders } from '../../api/order'
import { useCrudList } from '../../composables/useCrudList'
import { ORDER_STATUS_MAP, ORDER_TAG_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'
import OrderCreateDialog from './OrderCreateDialog.vue'

const auth = useAuthStore()
const isManager = computed(() => auth.hasRole('ADMIN'))

const showCreateDialog = ref(false)
const editingOrderId = ref(null)

function editOrder(id) {
  editingOrderId.value = id
  showCreateDialog.value = true
}

const { list, total, loading, error, query, fetchData, search, reset, onPageChange } = useCrudList(listOrders, {
  defaultQuery: { keyword: '', status: '' }
})

onMounted(fetchData)

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
    const blob = await exportOrders(mode, {
      keyword: query.keyword || '',
      status: query.status || ''
    })
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
<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <div style="display: flex; align-items: center; gap: 12px">
            <span>订单详情</span>
            <StatusTag :status="order.status" :map="ORDER_STATUS_MAP" />
            <span style="color: #909399; font-size: 13px">{{ order.orderNo }}</span>
          </div>
          <div style="display: flex; gap: 8px">
            <el-button v-if="['SAVED', 'REJECTED'].includes(order.status)" type="primary" size="small" @click="handleSubmit">
              <el-icon style="margin-right: 4px"><Upload /></el-icon>提交审批
            </el-button>
            <el-button v-if="order.status === 'PENDING_APPROVAL' && isManager" type="success" size="small" @click="handleApprove">
              <el-icon style="margin-right: 4px"><Select /></el-icon>通过
            </el-button>
            <el-button v-if="order.status === 'PENDING_APPROVAL' && isManager" type="warning" size="small" @click="showRejectDialog">
              <el-icon style="margin-right: 4px"><Close /></el-icon>驳回
            </el-button>
            <el-button v-if="order.status === 'APPROVED' && isManager" type="primary" size="small" @click="handleShip">
              <el-icon style="margin-right: 4px"><Van /></el-icon>发货
            </el-button>
            <el-button v-if="order.status === 'SHIPPED' && isManager" type="success" size="small" @click="handleDeliver">
              <el-icon style="margin-right: 4px"><Check /></el-icon>确认妥投
            </el-button>
            <el-button v-if="['APPROVED', 'SHIPPED', 'DELIVERED'].includes(order.status) && isManager" type="warning" size="small" @click="handleRefund">
              <el-icon style="margin-right: 4px"><Coin /></el-icon>退款
            </el-button>
            <el-button v-if="['SAVED', 'REJECTED'].includes(order.status)" type="primary" size="small" @click="editingOrderId = order.id; showEditDialog = true">
              <el-icon style="margin-right: 4px"><Edit /></el-icon>编辑
            </el-button>
            <el-button v-if="['SAVED', 'REJECTED'].includes(order.status)" type="danger" size="small" @click="handleCancel">
              <el-icon style="margin-right: 4px"><CircleClose /></el-icon>取消订单
            </el-button>
            <el-button @click="$router.back()" size="small">
              <el-icon style="margin-right: 4px"><ArrowLeft /></el-icon>返回
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号" min-width="160">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="order.status" :map="ORDER_STATUS_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">
          <el-icon style="margin-right: 4px; vertical-align: middle"><User /></el-icon>
          {{ order.customerName }}
        </el-descriptions-item>
        <el-descriptions-item label="销售姓名">
          <el-icon style="margin-right: 4px; vertical-align: middle"><Avatar /></el-icon>
          {{ order.salesPersonName }}
        </el-descriptions-item>
        <el-descriptions-item label="所用销售账户">
          <span v-if="order.salesAccountName">
            <el-tag size="small" type="info" effect="plain">
              {{ order.salesAccountName }}
            </el-tag>
            <span v-if="order.salesAccountDisplayName" style="margin-left: 4px; color: #909399">({{ order.salesAccountDisplayName }})</span>
          </span>
          <span v-else style="color: #c0c4cc">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="折扣金额">¥{{ order.discountAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最终金额">
          <span style="font-size: 16px; font-weight: 700; color: #f56c6c">¥{{ order.finalAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="收款信息" :span="2">
          <el-table :data="order.payments || []" border size="small" style="width: 100%">
            <template #empty>
              <span style="color: #c0c4cc">-</span>
            </template>
            <el-table-column label="收款渠道" min-width="150">
              <template #default="{ row: p }">
                <el-tag size="small" type="info" effect="plain">{{ p.paymentChannelTypeName || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="收款金额" width="140" align="right">
              <template #default="{ row: p }">
                <span style="font-weight: 600">¥{{ p.paymentAmount }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ order.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>订单明细</span>
      </template>
      <el-table :data="order.items || []" border stripe size="small">
        <template #empty>
          <el-empty description="暂无明细" />
        </template>
        <el-table-column prop="productName" label="产品名称" min-width="160" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="unitPrice" label="单价" width="110" align="right">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column prop="subtotal" label="小计" width="120" align="right">
          <template #default="{ row }">
            <span style="font-weight: 600">¥{{ row.subtotal }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>审批记录</span>
      </template>
      <el-timeline v-if="auditLogs.length">
        <el-timeline-item v-for="log in auditLogs" :key="log.id" :timestamp="log.operatedAt" :type="timelineType(log.action)">
          <div>
            <strong>{{ log.operatorName }}</strong>
            <StatusTag :status="log.action" :map="ORDER_ACTION_MAP" style="margin-left: 8px" />
          </div>
          <div v-if="log.comment" style="color: #606266; margin-top: 6px; background: #f5f7fa; padding: 8px 12px; border-radius: 4px; font-size: 13px">
            {{ log.comment }}
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" />
    </el-card>

    <!-- Edit Dialog -->
    <OrderCreateDialog v-model:visible="showEditDialog" :order-id="editingOrderId" @success="editingOrderId = null; fetchData" />

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="驳回订单" width="400px" destroy-on-close>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="驳回原因" prop="comment">
          <el-input v-model="rejectForm.comment" type="textarea" :rows="4" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejecting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, getAuditLogs, submitOrder, approveOrder, rejectOrder, cancelOrder, shipOrder, deliverOrder, refundOrder } from '../../api/order'
import { useAuthStore } from '../../store/auth'
import { ORDER_STATUS_MAP, ORDER_ACTION_MAP } from '../../constants'
import StatusTag from '../../components/StatusTag.vue'
import OrderCreateDialog from './OrderCreateDialog.vue'

const route = useRoute()
const auth = useAuthStore()
const orderId = route.params.id

const isManager = computed(() => auth.hasRole('ADMIN'))

const order = ref({ items: [] })
const auditLogs = ref([])
const loading = ref(false)

// Reject
const rejectDialogVisible = ref(false)
const rejectForm = reactive({ comment: '' })
const rejectRules = { comment: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }] }
const rejectFormRef = ref(null)
const rejecting = ref(false)

// Edit
const editingOrderId = ref(null)
const showEditDialog = ref(false)

function timelineType(action) {
  const map = { SUBMIT: 'primary', APPROVE: 'success', REJECT: 'danger', SHIP: 'primary', DELIVER: 'success', REFUND: 'warning', CANCEL: 'info' }
  return map[action] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const [oRes, aRes] = await Promise.all([
      getOrder(orderId),
      getAuditLogs(orderId)
    ])
    order.value = oRes.data
    auditLogs.value = aRes.data || []
  } catch { /* handled by interceptor */ }
  finally {
    loading.value = false
  }
}

async function handleSubmit() {
  try {
    await submitOrder(orderId)
    ElMessage.success('已提交审批')
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function handleApprove() {
  try {
    await approveOrder(orderId)
    ElMessage.success('审批通过')
    fetchData()
  } catch { /* handled by interceptor */ }
}

function showRejectDialog() {
  rejectForm.comment = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return
  rejecting.value = true
  try {
    await rejectOrder(orderId, rejectForm)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    fetchData()
  } finally {
    rejecting.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' })
    await cancelOrder(orderId)
    ElMessage.success('已取消')
    fetchData()
  } catch (err) {
    if (err !== 'cancel') return
  }
}

async function handleShip() {
  try {
    await ElMessageBox.confirm('确认发货该订单？', '提示', { type: 'info' })
    await shipOrder(orderId)
    ElMessage.success('已发货')
    fetchData()
  } catch {
    // cancelled dialog
  }
}

async function handleDeliver() {
  try {
    await ElMessageBox.confirm('确认该订单已妥投？', '提示', { type: 'success' })
    await deliverOrder(orderId)
    ElMessage.success('已确认妥投')
    fetchData()
  } catch {
    // cancelled dialog
  }
}

async function handleRefund() {
  try {
    await ElMessageBox.confirm('确认对该订单进行退款？', '提示', { type: 'warning' })
    await refundOrder(orderId)
    ElMessage.success('已退款')
    fetchData()
  } catch {
    // cancelled dialog
  }
}

onMounted(fetchData)
</script>

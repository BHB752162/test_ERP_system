<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>订单详情 - {{ order.orderNo }}</span>
          <div>
            <el-button v-if="order.status === 'DRAFT'" type="primary" size="small" @click="handleSubmit">提交审批</el-button>
            <el-button v-if="order.status === 'PENDING_APPROVAL' && isManager" type="success" size="small" @click="handleApprove">通过</el-button>
            <el-button v-if="order.status === 'PENDING_APPROVAL' && isManager" type="warning" size="small" @click="showRejectDialog">驳回</el-button>
            <el-button v-if="['APPROVED'].includes(order.status)" type="primary" size="small" @click="handleComplete">完成</el-button>
            <el-button v-if="['DRAFT', 'PENDING_APPROVAL'].includes(order.status)" type="danger" size="small" @click="handleCancel">取消订单</el-button>
            <el-button @click="$router.back()" size="small">返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="order.status" :map="statusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ order.customerName }}</el-descriptions-item>
        <el-descriptions-item label="销售姓名">{{ order.salesPersonName }}</el-descriptions-item>
        <el-descriptions-item label="所用微信号">
          <span v-if="order.salesWechatAccount">{{ order.salesWechatAccount }}<span v-if="order.salesWechatNickname">({{ order.salesWechatNickname }})</span></span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="折扣金额">¥{{ order.discountAmount }}</el-descriptions-item>
        <el-descriptions-item label="最终金额">
          <strong>¥{{ order.finalAmount }}</strong>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ order.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>订单明细</span>
      </template>
      <el-table :data="order.items || []" border stripe size="small">
        <el-table-column prop="productName" label="产品名称" min-width="160" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100" align="right">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column prop="subtotal" label="小计" width="110" align="right">
          <template #default="{ row }">¥{{ row.subtotal }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>审批记录</span>
      </template>
      <el-timeline>
        <el-timeline-item v-for="log in auditLogs" :key="log.id" :timestamp="log.operatedAt" :type="timelineType(log.action)">
          <div>
            <strong>{{ log.operatorName }}</strong>
            <StatusTag :status="log.action" :map="actionMap" style="margin-left: 8px" />
          </div>
          <div v-if="log.comment" style="color: #606266; margin-top: 4px">{{ log.comment }}</div>
        </el-timeline-item>
      </el-timeline>
      <div v-if="!auditLogs.length" style="color: #909399; text-align: center; padding: 20px">暂无审批记录</div>
    </el-card>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialog.visible" title="驳回订单" width="400px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="驳回原因" prop="comment">
          <el-input v-model="rejectForm.comment" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="rejecting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, getAuditLogs, submitOrder, approveOrder, rejectOrder, cancelOrder, completeOrder } from '../../api/order'
import { useAuthStore } from '../../store/auth'
import StatusTag from '../../components/StatusTag.vue'

const route = useRoute()
const auth = useAuthStore()
const orderId = route.params.id

const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_APPROVAL: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}
const actionMap = {
  SUBMIT: { label: '提交审批', type: 'primary' },
  APPROVE: { label: '通过', type: 'success' },
  REJECT: { label: '驳回', type: 'danger' },
  CANCEL: { label: '取消', type: 'info' },
  COMPLETE: { label: '完成', type: 'success' }
}

const isManager = computed(() => auth.hasAnyRole(['ADMIN', 'SALES_MANAGER']))

const order = ref({ items: [] })
const auditLogs = ref([])
const loading = ref(false)

// Reject
const rejectDialog = reactive({ visible: false })
const rejectForm = reactive({ comment: '' })
const rejectRules = { comment: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }] }
const rejectFormRef = ref(null)
const rejecting = ref(false)

function timelineType(action) {
  const map = { SUBMIT: 'primary', APPROVE: 'success', REJECT: 'danger', CANCEL: 'info', COMPLETE: 'success' }
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
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await submitOrder(orderId)
  ElMessage.success('已提交审批')
  fetchData()
}

async function handleApprove() {
  await approveOrder(orderId)
  ElMessage.success('审批通过')
  fetchData()
}

function showRejectDialog() {
  rejectForm.comment = ''
  rejectDialog.visible = true
}

async function handleReject() {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return
  rejecting.value = true
  try {
    await rejectOrder(orderId, rejectForm)
    ElMessage.success('已驳回')
    rejectDialog.visible = false
    fetchData()
  } finally {
    rejecting.value = false
  }
}

async function handleCancel() {
  await ElMessageBox.confirm('确认取消该订单？', '提示')
  await cancelOrder(orderId)
  ElMessage.success('已取消')
  fetchData()
}

async function handleComplete() {
  await ElMessageBox.confirm('确认完成该订单？', '提示')
  await completeOrder(orderId)
  ElMessage.success('已完成')
  fetchData()
}

onMounted(fetchData)
</script>

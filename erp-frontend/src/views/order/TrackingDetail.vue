<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span>
            <el-button type="default" link @click="$router.back()">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            运单详情 — {{ orderNo }}
            <el-tag v-if="orderStatus" size="small" style="margin-left: 12px">{{ orderStatus }}</el-tag>
          </span>
        </div>
      </template>

      <el-empty v-if="!loading && !trackings.length" description="该订单暂无运单号记录" />
      <el-table v-else :data="trackings" border stripe v-loading="loading">
        <el-table-column prop="trackingNo" label="运单号" width="180" />
        <el-table-column label="发货类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.shipmentType === 'WASH_CARE'" size="small" type="warning">洗护</el-tag>
            <span v-else style="color: #909399">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="SKU明细" min-width="320">
          <template #default="{ row }">
            <el-tag v-for="item in (row.items || [])" :key="item.id" size="small" style="margin-right: 6px; margin-bottom: 3px">
              {{ item.productName || item.productSku }} ×{{ item.quantity }}
            </el-tag>
            <span v-if="!(row.items || []).length" style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="妥投金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.deliveryAmount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="shippingTime" label="发货时间" width="180" />
        <el-table-column prop="createdAt" label="录入时间" width="180" />
        <el-table-column v-if="isAdmin" label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../store/auth'
import { listTracking, getOrder, deleteTracking } from '../../api/order'

const auth = useAuthStore()
const isAdmin = computed(() => auth.hasRole('ADMIN'))

const route = useRoute()
const orderId = route.params.orderId
const orderNo = ref(route.query.orderNo || '')
const orderStatus = ref('')
const trackings = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [tRes, oRes] = await Promise.all([
      listTracking(orderId),
      getOrder(orderId)
    ])
    trackings.value = tRes.data || []
    if (!orderNo.value) orderNo.value = oRes.data?.orderNo || ''
    orderStatus.value = oRes.data?.statusDisplay || ''
    if (!trackings.value.length) {
      ElMessage.info('该订单暂无运单号记录')
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除运单号「${row.trackingNo}」？删除后其 SKU 明细也将一并移除。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteTracking(row.id)
    ElMessage.success('已删除')
    trackings.value = trackings.value.filter(t => t.id !== row.id)
    if (!trackings.value.length) {
      ElMessage.info('该订单暂无运单号记录')
    }
  } catch (err) {
    if (err !== 'cancel' && err?.message) {
      ElMessage.error(err.message)
    }
  }
}
</script>

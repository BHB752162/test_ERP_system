<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span>运单号导入</span>
          <el-button type="primary" size="small" @click="downloadTemplate">
            <el-icon style="margin-right: 4px"><Download /></el-icon>下载模板
          </el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          Excel 列顺序：订单号 | 运单号 | 发货类型 | 发货时间 | 产品SKU | 数量 | 妥投金额（订单号、运单号、产品SKU、数量为必填；发货类型填写"洗护"或不填；发货时间不填则默认系统时间；妥投金额不填默认为0）
        </template>
      </el-alert>

      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :file-list="fileList"
        drag
      >
        <el-icon style="font-size: 48px; color: #c0c4cc"><Upload /></el-icon>
        <div style="color: #606266; margin-top: 8px">将 Excel 文件拖放到此处，或点击选择</div>
        <div style="color: #909399; font-size: 12px; margin-top: 4px">支持 .xlsx / .xls 格式</div>
      </el-upload>
    </el-card>

    <el-card v-if="previewData.length" shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>数据预览（{{ previewData.length }} 行）</span>
          <el-button type="danger" size="small" plain @click="clearPreview">清空</el-button>
        </div>
      </template>
      <el-table :data="previewData" border stripe size="small" max-height="400">
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="trackingNo" label="运单号" width="160" />
        <el-table-column label="发货类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.shipmentType === 'WASH_CARE'" size="small" type="warning">洗护</el-tag>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="shippingTime" label="发货时间" width="160" />
        <el-table-column prop="productSku" label="产品SKU" width="140" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column label="妥投金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.deliveryAmount || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button type="danger" link @click="previewData.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; text-align: right">
        <el-button type="primary" :loading="importing" @click="handleImport">确认导入</el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>导入记录查询</span>
      </template>
      <el-form :model="searchForm" inline @submit.prevent="searchTracking">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="输入订单号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchTracking">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-if="trackingRecords.length" :data="trackingRecords" border stripe size="small">
        <template #empty>
          <el-empty description="未找到运单号记录" />
        </template>
        <el-table-column prop="trackingNo" label="运单号" width="180" />
        <el-table-column label="发货类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.shipmentType === 'WASH_CARE'" size="small" type="warning">洗护</el-tag>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column label="SKU明细" min-width="280">
          <template #default="{ row }">
            <el-tag v-for="item in (row.items || [])" :key="item.id" size="small" style="margin-right: 4px; margin-bottom: 2px">
              {{ item.productSku }} × {{ item.quantity }}
            </el-tag>
            <span v-if="!(row.items || []).length" style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="妥投金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.deliveryAmount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="shippingTime" label="发货时间" width="170" />
      </el-table>
      <el-empty v-else description="请输入订单号查询" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { importTracking, listTracking, listOrders } from '../../api/order'

const fileList = ref([])
const previewData = ref([])
const importing = ref(false)
const trackingRecords = ref([])
const searchForm = reactive({ orderNo: '' })

function handleFileChange(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const wb = XLSX.read(e.target.result, { type: 'array' })
      const sheetName = wb.SheetNames[0]
      const sheet = wb.Sheets[sheetName]
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })

      if (rows.length < 2) {
        ElMessage.warning('Excel 文件为空或只有表头')
        return
      }

      // 列: 订单号 | 运单号 | 发货类型 | 发货时间 | 产品SKU | 数量 | 妥投金额
      const data = []
      const errors = []
      rows.slice(1).forEach((row, idx) => {
        const lineNo = idx + 1
        const orderNo = String(row[0] || '').trim()
        const trackingNo = String(row[1] || '').trim()
        const shipmentTypeRaw = String(row[2] || '').trim()
        const shippingTimeRaw = String(row[3] || '').trim()
        const productSku = String(row[4] || '').trim()
        const quantity = parseInt(row[5])
        const deliveryAmount = parseFloat(row[6])

        if (!orderNo) { errors.push(`第${lineNo}行：订单号不能为空`); return }
        if (!trackingNo) { errors.push(`第${lineNo}行：运单号不能为空`); return }
        if (!productSku) { errors.push(`第${lineNo}行：产品SKU不能为空`); return }
        if (!quantity || quantity <= 0) { errors.push(`第${lineNo}行：数量必须大于0`); return }

        data.push({
          orderNo,
          trackingNo,
          shipmentType: shipmentTypeRaw === '洗护' ? 'WASH_CARE' : (shipmentTypeRaw || null),
          shippingTime: shippingTimeRaw || null,
          productSku,
          quantity,
          deliveryAmount: isNaN(deliveryAmount) ? 0 : deliveryAmount
        })
      })

      if (errors.length) {
        ElMessage.error(errors.join('；'))
      }
      if (!data.length) {
        if (!errors.length) ElMessage.warning('未能解析到有效数据')
        return
      }

      previewData.value = data
      ElMessage.success(`成功解析 ${data.length} 行数据` + (errors.length ? `，${errors.length} 行跳过` : ''))
    } catch {
      ElMessage.error('Excel 文件解析失败')
    }
  }
  reader.readAsArrayBuffer(file.raw)
  fileList.value = [file]
}

function clearPreview() {
  previewData.value = []
  fileList.value = []
}

function downloadTemplate() {
  const wb = XLSX.utils.book_new()
  const headerRow = ['订单号', '运单号', '发货类型', '发货时间', '产品SKU', '数量', '妥投金额']
  const demoRow = ['B250501000001', 'SF1234567890', '洗护', '2025-05-25 15:30:00', 'SKU-001', 2, 100.00]
  const ws = XLSX.utils.aoa_to_sheet([headerRow, demoRow])
  XLSX.utils.book_append_sheet(wb, ws, '运单号导入')
  XLSX.writeFile(wb, '运单号导入模板.xlsx')
}

async function handleImport() {
  if (!previewData.value.length) {
    ElMessage.warning('没有可导入的数据')
    return
  }
  // 前端二次校验
  for (let i = 0; i < previewData.value.length; i++) {
    const row = previewData.value[i]
    const lineNo = i + 1
    if (!row.orderNo) { ElMessage.warning(`第${lineNo}行：订单号不能为空`); return }
    if (!row.trackingNo) { ElMessage.warning(`第${lineNo}行：运单号不能为空`); return }
    if (!row.productSku) { ElMessage.warning(`第${lineNo}行：产品SKU不能为空`); return }
    if (!row.quantity || row.quantity <= 0) { ElMessage.warning(`第${lineNo}行：数量必须大于0`); return }
  }
  importing.value = true
  try {
    await importTracking(previewData.value)
    ElMessage.success('导入成功')
    clearPreview()
  } catch { /* handled by interceptor */ }
  finally {
    importing.value = false
  }
}

async function searchTracking() {
  if (!searchForm.orderNo.trim()) {
    ElMessage.warning('请输入订单号')
    return
  }
  try {
    const res = await listOrders({ page: 1, pageSize: 1, keyword: searchForm.orderNo.trim() })
    const records = res.data?.records || []
    if (!records.length) {
      trackingRecords.value = []
      ElMessage.info('未找到该订单')
      return
    }
    const orderId = records[0].id
    const tRes = await listTracking(orderId)
    trackingRecords.value = tRes.data || []
    if (!trackingRecords.value.length) {
      ElMessage.info('该订单暂无运单号记录')
    }
  } catch { /* handled by interceptor */ }
}
</script>

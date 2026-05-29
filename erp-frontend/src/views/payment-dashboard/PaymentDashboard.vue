<template>
  <div v-loading="loading">
    <!-- 按审批时间统计 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">按审批时间统计</span>
          <div style="display: flex; gap: 8px; align-items: center">
            <el-date-picker
              v-model="approvalDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              style="width: 280px"
            />
            <el-button type="primary" :loading="approvalLoading" @click="fetchApprovalData">查询</el-button>
            <el-button :loading="exportLoading" @click="handleExport">导出Excel</el-button>
          </div>
        </div>
      </template>
      <div ref="approvalChartRef" style="height: 300px">
        <el-empty v-if="!approvalData.length && !approvalLoading" description="请选择日期范围后点击查询" />
      </div>
    </el-card>

    <el-row :gutter="0">
      <el-col :span="24">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>
            <span style="font-weight: 600">今日支付汇总</span>
          </template>
          <div ref="todayChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>
            <span style="font-weight: 600">近7天支付汇总</span>
          </template>
          <div ref="weekChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600">近30天支付汇总</span>
          </template>
          <div ref="monthChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getPaymentSummary, getByApprovalTime, exportByApprovalTime } from '../../api/paymentDashboard'

const loading = ref(false)
const todayChartRef = ref(null)
const weekChartRef = ref(null)
const monthChartRef = ref(null)
const approvalChartRef = ref(null)

let todayChart = null
let weekChart = null
let monthChart = null
let approvalChart = null

const approvalDateRange = ref(null)
const approvalData = ref([])
const approvalLoading = ref(false)
const exportLoading = ref(false)

async function handleExport() {
  let [start, end] = approvalDateRange.value || []
  if (!start || !end) {
    ElMessage.warning('请先选择日期范围')
    return
  }
  exportLoading.value = true
  try {
    const blob = await exportByApprovalTime(start, end)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `payment_export_${start}_to_${end}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { /* handled by interceptor */ }
  finally { exportLoading.value = false }
}

const dateShortcuts = [
  { text: '本周', value: () => {
    const now = new Date()
    const day = now.getDay() || 7
    const monday = new Date(now)
    monday.setDate(now.getDate() - day + 1)
    return [monday, now]
  }},
  { text: '上周', value: () => {
    const now = new Date()
    const day = now.getDay() || 7
    const lastMonday = new Date(now)
    lastMonday.setDate(now.getDate() - day - 6)
    const lastSunday = new Date(now)
    lastSunday.setDate(now.getDate() - day)
    return [lastMonday, lastSunday]
  }},
  { text: '本月', value: () => {
    const now = new Date()
    return [new Date(now.getFullYear(), now.getMonth(), 1), now]
  }},
  { text: '上月', value: () => {
    const now = new Date()
    return [new Date(now.getFullYear(), now.getMonth() - 1, 1), new Date(now.getFullYear(), now.getMonth(), 0)]
  }}
]

function renderChart(chart, data) {
  chart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: ¥{c}' },
    xAxis: {
      type: 'category',
      data: data.map(d => d.channelTypeName),
      axisLabel: { fontSize: 12 }
    },
    yAxis: { type: 'value', name: '金额 (元)' },
    series: [{
      type: 'bar',
      data: data.map(d => d.totalAmount),
      itemStyle: { color: '#409eff' },
      barMaxWidth: 50,
      label: { show: true, position: 'top', fontSize: 11 }
    }],
    dataZoom: data.length > 8 ? [{
      type: 'slider',
      height: 20,
      bottom: 10,
      start: 0,
      end: data.length > 10 ? Math.round(800 / data.length) : 100
    }] : [],
    grid: { top: 20, bottom: data.length > 8 ? 50 : 60, left: 60, right: 20 }
  })
}

function renderEmpty(chart) {
  chart.setOption({
    graphic: { type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#999', fontSize: 14 } }
  })
}

async function fetchApprovalData() {
  let [start, end] = approvalDateRange.value || []
  if (!start || !end) {
    const today = new Date().toISOString().substring(0, 10)
    start = today
    end = today
    approvalDateRange.value = [today, today]
  }
  approvalLoading.value = true
  try {
    const res = await getByApprovalTime(start, end)
    approvalData.value = res.data || []
    await nextTick()
    if (!approvalChart) {
      approvalChart = echarts.init(approvalChartRef.value)
    }
    if (approvalData.value.length) renderChart(approvalChart, approvalData.value)
    else renderEmpty(approvalChart)
  } catch { /* ignored */ }
  finally { approvalLoading.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getPaymentSummary()
    const data = res.data
    await nextTick()

    todayChart = echarts.init(todayChartRef.value)
    weekChart = echarts.init(weekChartRef.value)
    monthChart = echarts.init(monthChartRef.value)

    if (data.today && data.today.length) renderChart(todayChart, data.today)
    else renderEmpty(todayChart)

    if (data.last7Days && data.last7Days.length) renderChart(weekChart, data.last7Days)
    else renderEmpty(weekChart)

    if (data.last30Days && data.last30Days.length) renderChart(monthChart, data.last30Days)
    else renderEmpty(monthChart)
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
})

onBeforeUnmount(() => {
  todayChart?.dispose()
  weekChart?.dispose()
  monthChart?.dispose()
  approvalChart?.dispose()
})
</script>

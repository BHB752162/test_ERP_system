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
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-actions">
        <span></span>
        <el-button type="primary" @click="$router.push('/orders/create')">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 创建订单
        </el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="query.keyword || query.status ? '没有找到匹配的订单' : '暂无订单数据'" />
        </template>
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
            <StatusTag :status="row.status" :map="ORDER_STATUS_MAP" />
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
import { onMounted } from 'vue'
import { listOrders } from '../../api/order'
import { useCrudList } from '../../composables/useCrudList'
import { ORDER_STATUS_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, query, fetchData, search, reset, onPageChange } = useCrudList(listOrders, {
  defaultQuery: { keyword: '', status: '' }
})

onMounted(fetchData)
</script>

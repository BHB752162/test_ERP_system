<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="query.action" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option v-for="(v, k) in ORDER_ACTION_MAP" :key="k" :value="k" :label="v.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="query.orderNo || query.action ? '没有找到匹配的日志' : '暂无审批日志'" />
        </template>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.action" :map="ORDER_ACTION_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="comment" label="意见" min-width="200" />
        <el-table-column prop="operatedAt" label="操作时间" width="160" />
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { listAuditLogs } from '../../api/order'
import { useCrudList } from '../../composables/useCrudList'
import { ORDER_ACTION_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, query, fetchData, search, reset, onPageChange } = useCrudList(listAuditLogs, {
  defaultQuery: { orderNo: '', action: '' }
})

onMounted(fetchData)
</script>

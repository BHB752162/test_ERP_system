<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="客户名称">
          <el-input v-model="query.keyword" placeholder="名称/电话/邮箱" clearable @keyup.enter="search" />
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
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="query.keyword ? '没有找到匹配的客户' : '暂无客户数据'" />
        </template>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="customerName" label="客户名称" min-width="140" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
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
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/customers/${row.id}`)">详情</el-button>
            <el-button type="primary" link @click="$router.push(`/customers/${row.id}/edit`)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
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
import { listCustomers, deleteCustomer } from '../../api/customer'
import { useCrudList, useDeleteAction } from '../../composables/useCrudList'
import { ENABLE_STATUS_MAP, CUSTOMER_LEVEL_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, query, fetchData, search, reset, onPageChange } = useCrudList(listCustomers, { defaultQuery: { keyword: '' } })
const { handleDelete } = useDeleteAction(deleteCustomer, fetchData)

onMounted(fetchData)
</script>

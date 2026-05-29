<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="产品名称">
          <el-input v-model="query.keyword" placeholder="名称/SKU" clearable style="width: 160px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="上下架状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
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
        <el-button type="primary" @click="$router.push('/products/create')">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 新增产品
        </el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty :description="error || (hasActiveFilter ? '没有找到匹配的产品' : '暂无产品数据')" />
        </template>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="productCode" label="SKU" width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="productType" label="类型" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.productType" :map="PRODUCT_TYPE_MAP" />
          </template>
        </el-table-column>
<el-table-column prop="stockQuantity" label="库存" width="80" align="right" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column prop="createdByName" label="创建人" min-width="100" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column prop="updatedByName" label="更新人" min-width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
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
import { computed, onMounted } from 'vue'
import { listProducts, deleteProduct } from '../../api/product'
import { useCrudList, useDeleteAction } from '../../composables/useCrudList'
import { ENABLE_STATUS_MAP, PRODUCT_TYPE_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, error, query, fetchData, search, reset, onPageChange } = useCrudList(listProducts, { defaultQuery: { keyword: '', status: undefined } })
const { handleDelete } = useDeleteAction(deleteProduct, fetchData)

const hasActiveFilter = computed(() => !!(query.keyword || query.status != null))

onMounted(fetchData)
</script>

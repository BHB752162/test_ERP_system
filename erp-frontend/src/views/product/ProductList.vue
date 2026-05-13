<template>
  <div>
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="产品名称">
          <el-input v-model="query.keyword" placeholder="名称/SKU" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="$router.push('/products/create')">新增产品</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="productCode" label="SKU" width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="140" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="price" label="单价" width="100" align="right">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存" width="80" align="right" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="statusMap" />
          </template>
        </el-table-column>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listProducts, deleteProduct } from '../../api/product'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const statusMap = { 0: { label: '下架', type: 'danger' }, 1: { label: '上架', type: 'success' } }

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', page: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await listProducts(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

async function handleDelete(id) {
  await deleteProduct(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

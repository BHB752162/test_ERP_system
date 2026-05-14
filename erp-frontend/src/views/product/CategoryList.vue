<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>产品分类</span>
        <el-button type="primary" size="small" @click="showDialog(null)">新增分类</el-button>
      </div>
    </template>
    <el-table :data="categories" border stripe v-loading="loading" row-key="id" default-expand-all>
      <template #empty>
        <el-empty description="暂无分类数据" />
      </template>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="categoryName" label="分类名称" min-width="180" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="450px" @close="closeDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-tree-select v-model="formData.parentId" :data="categories" :props="{ label: 'categoryName', value: 'id' }" placeholder="不选则为顶级分类" clearable filterable style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listCategories, createCategory, updateCategory, deleteCategory } from '../../api/product'
import { ENABLE_STATUS_MAP } from '../../constants'
import StatusTag from '../../components/StatusTag.vue'

const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const defaultForm = { categoryName: '', parentId: null, sortOrder: 0, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = { categoryName: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

async function fetchData() {
  loading.value = true
  try {
    const res = await listCategories()
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

function showDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  if (row) Object.assign(formData, row)
  else Object.assign(formData, { ...defaultForm })
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, formData)
    } else {
      await createCategory(formData)
    }
    closeDialog()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteCategory(id)
    fetchData()
  } catch { /* handled by interceptor */ }
}

onMounted(fetchData)
</script>

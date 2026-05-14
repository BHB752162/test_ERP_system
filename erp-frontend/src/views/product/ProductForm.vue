<template>
  <el-card shadow="never">
    <template #header>
      <span>{{ isEdit ? '编辑产品' : '新增产品' }}</span>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 600px" v-loading="loading">
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="form.productName" />
      </el-form-item>
      <el-form-item label="SKU编号" prop="productCode">
        <el-input v-model="form.productCode" />
      </el-form-item>
      <el-form-item label="产品分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择" clearable filterable style="width: 100%">
          <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.categoryName" />
        </el-select>
      </el-form-item>
      <el-form-item label="销售单价" prop="price">
        <el-input-number v-model="form.price" :precision="2" :min="0" :min-strictly="true" style="width: 100%" />
      </el-form-item>
      <el-form-item label="成本价" prop="costPrice">
        <el-input-number v-model="form.costPrice" :precision="2" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="库存数量" prop="stockQuantity">
        <el-input-number v-model="form.stockQuantity" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProduct, createProduct, updateProduct, listCategories } from '../../api/product'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)
const categories = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  productName: '', productCode: '', categoryId: null,
  price: 0, costPrice: 0, stockQuantity: 0,
  description: '', status: 1
})

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productCode: [{ required: true, message: '请输入SKU编号', trigger: 'blur' }],
  price: [{ required: true, message: '请输入销售单价', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const [catRes, prodRes] = isEdit.value
      ? await Promise.all([listCategories(), getProduct(route.params.id)])
      : await Promise.all([listCategories()])

    categories.value = catRes.data
    if (isEdit.value) {
      Object.assign(form, prodRes.data)
    }
  } catch { /* handled by interceptor */ }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      await createProduct(form)
      ElMessage.success('创建成功')
    }
    router.push('/products')
  } catch { /* handled by interceptor */ }
  finally {
    submitting.value = false
  }
}
</script>

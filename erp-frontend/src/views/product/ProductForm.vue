<template>
  <el-card shadow="never">
    <template #header>
      <span>{{ isEdit ? '编辑产品' : '新增产品' }}</span>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 600px" v-loading="loading">
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="form.productName" />
      </el-form-item>
      <el-form-item label="产品类型" prop="productType">
        <el-radio-group v-model="form.productType" @change="onTypeChange">
          <el-radio value="SINGLE">单品</el-radio>
          <el-radio value="SET">套装</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.productType === 'SINGLE'" label="所属套装">
        <el-select v-model="form.parentId" placeholder="选择所属套装" clearable filterable style="width: 100%">
          <el-option
            v-for="s in sets"
            :key="s.id"
            :value="s.id"
            :label="s.productName"
            :disabled="s.id === editId"
          />
        </el-select>
      </el-form-item>

      <!-- 套装：管理包含的单品 -->
      <el-form-item v-if="form.productType === 'SET' && isEdit" label="包含单品">
        <div style="width: 100%">
          <div style="display: flex; gap: 8px; margin-bottom: 8px">
            <el-select v-model="selectedChildId" placeholder="选择单品" style="flex: 1" clearable filterable>
              <el-option
                v-for="p in availableSingles"
                :key="p.id"
                :value="p.id"
                :label="p.productName + (p.productCode ? ' (' + p.productCode + ')' : '')"
                :disabled="childIds.includes(p.id)"
              />
            </el-select>
            <el-button type="primary" :disabled="!selectedChildId" @click="addChild">添加</el-button>
          </div>
          <el-tag
            v-for="pid in childIds"
            :key="pid"
            closable
            :disable-transitions="true"
            style="margin: 2px 4px 2px 0"
            @close="removeChild(pid)"
          >
            {{ getChildLabel(pid) }}
          </el-tag>
          <span v-if="childIds.length === 0" style="color: #999; font-size: 13px">暂未添加单品</span>
        </div>
      </el-form-item>

      <el-form-item label="SKU编号" prop="productCode">
        <el-input v-model="form.productCode" />
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
import { getProduct, createProduct, updateProduct, listSets, listChildren, updateChildren, listProducts } from '../../api/product'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)
const isEdit = computed(() => !!route.params.id)
const editId = computed(() => route.params.id ? Number(route.params.id) : null)

const form = reactive({
  productName: '', productType: 'SINGLE', productCode: '',
  price: 0, costPrice: 0, stockQuantity: 0,
  description: '', status: 1, parentId: null
})

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productType: [{ required: true, message: '请选择产品类型', trigger: 'change' }],
  productCode: [{ required: true, message: '请输入SKU编号', trigger: 'blur' }],
  price: [{ required: true, message: '请输入销售单价', trigger: 'blur' }],
}

// 套装下拉列表
const sets = ref([])
// 单品列表（用于套装选择子产品）
const allSingles = ref([])
const childIds = ref([])
const selectedChildId = ref(null)

const childMap = computed(() => {
  const map = {}
  for (const p of allSingles.value) {
    map[p.id] = p
  }
  return map
})

const availableSingles = computed(() => {
  return allSingles.value.filter(p => p.status !== 0 && p.id !== editId.value)
})

function getChildLabel(pid) {
  const p = childMap.value[pid]
  return p ? p.productName + (p.productCode ? ' (' + p.productCode + ')' : '') : `ID:${pid}`
}

async function loadSets() {
  try {
    const res = await listSets()
    sets.value = res.data || []
  } catch { /* ignore */ }
}

async function loadSingles() {
  try {
    const res = await listProducts({ page: 1, pageSize: 999 })
    allSingles.value = (res.data?.records || []).filter(p => p.productType === 'SINGLE')
  } catch { /* ignore */ }
}

async function loadChildren() {
  if (!editId.value) return
  try {
    const res = await listChildren(editId.value)
    childIds.value = (res.data || []).map(p => p.id)
  } catch { /* ignore */ }
}

function onTypeChange(type) {
  if (type === 'SINGLE') {
    form.parentId = null
    loadSets()
  } else {
    form.parentId = null
  }
}

onMounted(async () => {
  try {
    if (isEdit.value) {
      const res = await getProduct(route.params.id)
      Object.assign(form, res.data)
    }
    await loadSets()
    await loadSingles()
    if (isEdit.value && form.productType === 'SET') {
      await loadChildren()
    }
  } catch { /* handled by interceptor */ }
})

function addChild() {
  if (!selectedChildId.value) return
  childIds.value.push(selectedChildId.value)
  selectedChildId.value = null
}

function removeChild(pid) {
  childIds.value = childIds.value.filter(id => id !== pid)
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(route.params.id, form)
      // 如果是套装，保存子产品
      if (form.productType === 'SET') {
        await updateChildren(route.params.id, childIds.value)
      }
      ElMessage.success('更新成功')
    } else {
      const res = await createProduct(form)
      ElMessage.success('创建成功')
      // 如果是套装，创建后保存子产品
      if (form.productType === 'SET') {
        // 创建后获取新ID保存
        const newId = res.data
        if (newId && childIds.value.length > 0) {
          await updateChildren(newId, childIds.value)
        }
      }
    }
    router.push('/products')
  } catch { /* handled by interceptor */ }
  finally {
    submitting.value = false
  }
}
</script>

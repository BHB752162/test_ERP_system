<template>
  <el-card shadow="never">
    <template #header>
      <span>创建订单</span>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px">
      <el-form-item label="选择微信号" prop="salesWechatId">
        <el-select v-model="form.salesWechatId" placeholder="请先选择微信号" filterable style="width: 100%" @change="onWechatChange">
          <el-option v-for="w in myWechats" :key="w.id" :value="w.id" :label="`${w.wechatAccount}(${w.wechatNickname || ''})`" />
        </el-select>
        <div v-if="!myWechats.length" style="color: #909399; font-size: 12px; margin-top: 4px">暂无微信号，请联系管理员配置</div>
      </el-form-item>

      <el-form-item label="选择客户" prop="customerId">
        <el-select v-model="form.customerId" placeholder="选择绑定的客户" filterable style="width: 100%">
          <el-option v-for="c in boundCustomers" :key="c.customerId" :value="c.customerId" :label="c.customerName" />
        </el-select>
        <div v-if="form.salesWechatId && !boundCustomers.length" style="color: #e6a23c; font-size: 12px; margin-top: 4px">该微信号未绑定任何客户，请先在绑定管理中绑定</div>
      </el-form-item>

      <el-form-item label="收件地址">
        <el-select v-model="form.shippingAddressId" placeholder="选择收件地址" clearable filterable style="width: 100%" :disabled="!form.customerId">
          <el-option v-for="a in shippingAddresses" :key="a.id" :value="a.id" :label="`${a.recipientName} ${a.recipientPhone} - ${a.address.substring(0, 30)}${a.address.length > 30 ? '...' : ''}`">
            <span>{{ a.recipientName }}</span>
            <span style="color: #909399; margin-left: 4px">{{ a.recipientPhone }}</span>
            <span style="color: #c0c4cc; margin-left: 8px; font-size: 12px">{{ a.address }}</span>
            <el-tag v-if="a.isDefault" size="small" type="success" style="margin-left: 4px">默认</el-tag>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item label="订单标签">
        <el-select v-model="form.tag" placeholder="选择标签" clearable style="width: 100%">
          <el-option label="无" value="" />
          <el-option label="延迟发货" value="DELAYED" />
          <el-option label="洗护订单" value="WASH_CARE" />
        </el-select>
      </el-form-item>

      <el-form-item label="商场订单号">
        <el-input v-model="form.mallOrderInfo" placeholder="关联商场订单号（选填）" />
      </el-form-item>

      <el-divider>订单明细</el-divider>

      <div v-for="(item, index) in form.items" :key="index" style="display: flex; gap: 10px; align-items: center; margin-bottom: 10px">
        <el-select v-model="item.productId" placeholder="选择产品" filterable style="width: 250px" @change="val => onProductChange(index, val)">
          <el-option v-for="p in products" :key="p.id" :value="p.id" :label="`${p.productName}(${p.productCode}) - ¥${p.price}`" />
        </el-select>
        <el-input-number v-model="item.quantity" :min="1" :max="item.maxStock || 99999" label="数量" style="width: 140px" @change="val => onQtyChange(index)" />
        <span style="width: 100px; text-align: right">小计: ¥{{ item.subtotal }}</span>
        <el-button type="danger" :icon="Delete" circle @click="removeItem(index)" />
      </div>
      <el-button type="primary" plain @click="addItem">+ 添加产品</el-button>

      <el-divider />

      <div style="text-align: right; font-size: 16px; margin-bottom: 16px">
        总金额: <strong>¥{{ totalAmount }}</strong>
      </div>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit('DRAFT')">保存草稿</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmit('SUBMIT')">提交审批</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { createDraft, submitOrder, listWechatsForOrder, getBoundCustomers, listProductsForOrder } from '../../api/order'
import { listShippingAddresses } from '../../api/customer'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const myWechats = ref([])
const boundCustomers = ref([])
const products = ref([])
const shippingAddresses = ref([])

const form = reactive({
  salesWechatId: null,
  customerId: null,
  shippingAddressId: null,
  remark: '',
  tag: '',
  mallOrderInfo: '',
  items: []
})

const rules = {
  salesWechatId: [{ required: true, message: '请选择微信号', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }]
}

const totalAmount = computed(() => {
  return form.items.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

watch(() => form.customerId, async (customerId) => {
  form.shippingAddressId = null
  if (!customerId) { shippingAddresses.value = []; return }
  try {
    const res = await listShippingAddresses(customerId)
    shippingAddresses.value = res.data || []
  } catch { /* handled by interceptor */ }
})

onMounted(async () => {
  try {
    const [wRes, pRes] = await Promise.all([
      listWechatsForOrder(),
      listProductsForOrder()
    ])
    myWechats.value = wRes.data || []
    products.value = pRes.data?.records || pRes.data || []
  } catch { /* handled by interceptor */ }
})

async function onWechatChange(wechatId) {
  form.customerId = null
  if (!wechatId) { boundCustomers.value = []; return }
  try {
    const res = await getBoundCustomers(wechatId)
    boundCustomers.value = res.data || []
  } catch { /* handled by interceptor */ }
}

function addItem() {
  form.items.push({ productId: null, quantity: 1, subtotal: 0, maxStock: 99999 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onProductChange(index, productId) {
  const product = products.value.find(p => p.id === productId)
  if (product) {
    form.items[index].maxStock = product.stockQuantity
    form.items[index].quantity = 1
    const price = parseFloat(product.price)
    form.items[index].subtotal = price
    form.items[index].unitPrice = price
    form.items[index].productName = product.productName
  }
}

function onQtyChange(index) {
  const item = form.items[index]
  const product = products.value.find(p => p.id === item.productId)
  if (product) {
    const price = parseFloat(product.price)
    item.subtotal = price * item.quantity
    item.unitPrice = price
  }
}

async function handleSubmit(action) {
  if (!form.items.length) {
    ElMessage.warning('请至少添加一个产品')
    return
  }
  const missingProduct = form.items.some(item => !item.productId)
  if (missingProduct) {
    ElMessage.warning('请为每个明细选择产品')
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = {
      salesWechatId: form.salesWechatId,
      customerId: form.customerId,
      shippingAddressId: form.shippingAddressId || null,
      remark: form.remark,
      tag: form.tag || '',
      mallOrderInfo: form.mallOrderInfo || '',
      items: form.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice || 0
      }))
    }
    const res = await createDraft(data)
    const orderId = res.data?.id
    if (action === 'SUBMIT' && orderId) {
      await submitOrder(orderId)
      ElMessage.success('订单已提交审批')
    } else {
      ElMessage.success('草稿保存成功')
    }
    router.push('/orders')
  } catch { /* handled by interceptor */ }
  finally {
    submitting.value = false
  }
}
</script>

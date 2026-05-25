<template>
  <el-dialog
    v-model="visible"
    :title="orderId ? '编辑订单' : '创建订单'"
    width="900px"
    top="3vh"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="销售账户" prop="salesAccountId">
        <el-select v-model="form.salesAccountId" placeholder="请先选择销售账户" filterable style="width: 100%" :disabled="!!orderId" @change="onAccountChange">
          <el-option v-for="a in myAccounts" :key="a.id" :value="a.id" :label="`${a.accountName}${a.displayName ? '(' + a.displayName + ')' : ''}`" />
        </el-select>
        <div v-if="!myAccounts.length" style="color: #909399; font-size: 12px; margin-top: 4px">暂无销售账户，请联系管理员配置</div>
      </el-form-item>

      <el-form-item label="选择客户" prop="customerId">
        <el-select v-model="form.customerId" placeholder="选择绑定的客户" filterable style="width: 100%" :disabled="!!orderId">
          <el-option v-for="c in boundCustomers" :key="c.customerId" :value="c.customerId" :label="c.customerName" />
        </el-select>
        <div v-if="form.salesAccountId && !boundCustomers.length" style="color: #e6a23c; font-size: 12px; margin-top: 4px">该销售账户未绑定任何客户，请先在绑定管理中绑定</div>
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

      <el-divider>收款信息</el-divider>

      <div v-for="(pay, index) in form.payments" :key="index" style="display: flex; gap: 10px; align-items: center; margin-bottom: 10px">
        <el-select v-model="pay.paymentChannelTypeId" placeholder="选择渠道" style="width: 220px">
          <el-option v-for="ct in channelTypes" :key="ct.id" :value="ct.id" :label="ct.typeName" />
        </el-select>
        <el-input-number v-model="pay.paymentAmount" :min="0" :precision="2" :step="100" placeholder="金额" style="width: 180px" />
        <span style="width: 80px; text-align: right">¥{{ pay.paymentAmount || 0 }}</span>
        <el-button type="danger" :icon="Delete" circle @click="removePayment(index)" :disabled="form.payments.length <= 1" />
      </div>
      <el-button type="primary" plain @click="addPayment">+ 添加收款渠道</el-button>

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

      <div style="text-align: right; font-size: 14px; color: #909399; margin-bottom: 4px">
        收款合计: <strong>¥{{ totalPayment }}</strong>
      </div>
      <div style="text-align: right; font-size: 16px; margin-bottom: 16px">
        总金额: <strong>¥{{ totalAmount }}</strong>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <template v-if="orderId">
        <el-button type="primary" :loading="submitting" @click="handleSubmit('SAVE')">保存修改</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmit('SUBMIT')">提交审批</el-button>
      </template>
      <template v-else>
        <el-button type="primary" :loading="submitting" @click="handleSubmit('DRAFT')">保存草稿</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmit('SUBMIT')">提交审批</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { createDraft, submitOrder, updateOrder, getOrder, listAccountsForOrder, getBoundCustomers, listProductsForOrder } from '../../api/order'
import { listShippingAddresses } from '../../api/customer'
import { listChannelTypes } from '../../api/channelType'

const visible = defineModel('visible', { type: Boolean, default: false })
const props = defineProps({
  orderId: { type: Number, default: null }
})
const emit = defineEmits(['success'])

const formRef = ref(null)
const submitting = ref(false)
const myAccounts = ref([])
const boundCustomers = ref([])
const products = ref([])
const shippingAddresses = ref([])
const channelTypes = ref([])

const form = reactive({
  salesAccountId: null,
  customerId: null,
  shippingAddressId: null,
  remark: '',
  tag: '',
  mallOrderInfo: '',
  payments: [{ paymentChannelTypeId: null, paymentAmount: null }],
  items: []
})

const rules = {
  salesAccountId: [{ required: true, message: '请选择销售账户', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }]
}

const totalAmount = computed(() => {
  return form.items.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

const totalPayment = computed(() => {
  return form.payments.reduce((sum, p) => sum + (p.paymentAmount || 0), 0)
})

watch(() => form.customerId, async (customerId) => {
  form.shippingAddressId = null
  if (!customerId) { shippingAddresses.value = []; return }
  try {
    const res = await listShippingAddresses(customerId)
    shippingAddresses.value = res.data || []
  } catch { /* handled by interceptor */ }
})

watch(visible, async (v) => {
  if (v) {
    form.salesAccountId = null
    form.customerId = null
    form.shippingAddressId = null
    form.remark = ''
    form.tag = ''
    form.mallOrderInfo = ''
    form.payments = [{ paymentChannelTypeId: null, paymentAmount: null }]
    form.items = []
    boundCustomers.value = []
    shippingAddresses.value = []
    try {
      const [aRes, pRes, ctRes] = await Promise.all([
        listAccountsForOrder(),
        listProductsForOrder(),
        listChannelTypes()
      ])
      myAccounts.value = aRes.data || []
      products.value = pRes.data?.records || pRes.data || []
      channelTypes.value = (ctRes.data || []).filter(ct => ct.status === 1)

      // 编辑模式：加载已有订单数据回填
      if (props.orderId) {
        const orderRes = await getOrder(props.orderId)
        const o = orderRes.data
        form.salesAccountId = o.salesAccountId
        form.customerId = o.customerId
        form.shippingAddressId = o.shippingAddressId || null
        form.remark = o.remark || ''
        form.tag = o.tag || ''
        form.mallOrderInfo = o.mallOrderInfo || ''
        form.payments = (o.payments || []).map(p => ({
          paymentChannelTypeId: p.paymentChannelTypeId,
          paymentAmount: p.paymentAmount
        }))
        if (!form.payments.length) form.payments = [{ paymentChannelTypeId: null, paymentAmount: null }]
        form.items = (o.items || []).map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          subtotal: item.subtotal,
          productName: item.productName,
          maxStock: 99999
        }))
        // 加载该账户的绑定客户和收件地址
        if (o.salesAccountId) {
          const bRes = await getBoundCustomers(o.salesAccountId)
          boundCustomers.value = bRes.data || []
        }
        if (o.customerId) {
          const sRes = await listShippingAddresses(o.customerId)
          shippingAddresses.value = sRes.data || []
        }
      }
    } catch { /* handled by interceptor */ }
  }
})

async function onAccountChange(accountId) {
  form.customerId = null
  if (!accountId) { boundCustomers.value = []; return }
  try {
    const res = await getBoundCustomers(accountId)
    boundCustomers.value = res.data || []
  } catch { /* handled by interceptor */ }
}

function addItem() {
  form.items.push({ productId: null, quantity: 1, subtotal: 0, maxStock: 99999 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function addPayment() {
  form.payments.push({ paymentChannelTypeId: null, paymentAmount: null })
}

function removePayment(index) {
  form.payments.splice(index, 1)
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
  const validPayment = form.payments.every(p => p.paymentChannelTypeId && p.paymentAmount > 0)
  if (!validPayment) {
    ElMessage.warning('请完整填写收款渠道和金额')
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = {
      salesAccountId: form.salesAccountId,
      customerId: form.customerId,
      shippingAddressId: form.shippingAddressId || null,
      remark: form.remark,
      tag: form.tag || '',
      mallOrderInfo: form.mallOrderInfo || '',
      payments: form.payments.map(p => ({
        paymentChannelTypeId: p.paymentChannelTypeId,
        paymentAmount: p.paymentAmount
      })),
      items: form.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice || 0
      }))
    }
    if (props.orderId) {
      // 编辑模式
      await updateOrder(props.orderId, data)
      if (action === 'SUBMIT') {
        await submitOrder(props.orderId)
        ElMessage.success('订单已提交审批')
      } else {
        ElMessage.success('修改已保存')
      }
    } else {
      // 创建模式
      const res = await createDraft(data)
      const newId = res.data?.id
      if (action === 'SUBMIT' && newId) {
        await submitOrder(newId)
        ElMessage.success('订单已提交审批')
      } else {
        ElMessage.success('草稿保存成功')
      }
    }
    visible.value = false
    emit('success')
  } catch { /* handled by interceptor */ }
  finally {
    submitting.value = false
  }
}
</script>

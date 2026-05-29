<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span>{{ isEdit ? '编辑顾客' : '新增顾客' }}</span>
          <div style="display: flex; gap: 8px">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              <el-icon style="margin-right: 4px"><Check /></el-icon>保存
            </el-button>
            <el-button @click="goBack">
              <el-icon style="margin-right: 4px"><ArrowLeft /></el-icon>返回
            </el-button>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" maxlength="20" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加粉时间" prop="addFriendTime">
              <el-date-picker v-model="form.addFriendTime" type="date" placeholder="选择加粉时间（必填）" value-format="YYYY-MM-DD" format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="顾客生日">
              <div style="display: flex; gap: 8px">
                <el-select v-model="birthMonth" placeholder="月" style="width: 120px" clearable>
                  <el-option v-for="m in 12" :key="m" :value="String(m).padStart(2, '0')" :label="m + '月'" />
                </el-select>
                <el-select v-model="birthDay" placeholder="日" style="width: 120px" clearable>
                  <el-option v-for="d in dayOptions" :key="d" :value="String(d).padStart(2, '0')" :label="d + '日'" />
                </el-select>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信号" prop="wechatAccount">
              <el-input v-model="form.wechatAccount" placeholder="顾客微信号（必填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="等级" prop="level">
              <el-select v-model="form.level" style="width: 100%">
                <el-option :value="0" label="普通" />
                <el-option :value="1" label="银卡" />
                <el-option :value="2" label="金卡" />
                <el-option :value="3" label="钻石" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="顾客备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- Sales Account Bindings -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>绑定销售账户</span>
          <el-button size="small" type="primary" @click="showBindDialog">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 绑定销售账户
          </el-button>
        </div>
      </template>
      <el-table :data="boundAccounts" border stripe size="small">
        <template #empty>
          <el-empty description="暂未绑定销售账户" />
        </template>
        <el-table-column prop="salesAccountName" label="账户名称" min-width="140" />
        <el-table-column prop="salesAccountDisplayName" label="显示名称" width="140" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row, $index }">
            <el-popconfirm title="确认解绑？" @confirm="handleUnbind(row, $index)">
              <template #reference>
                <el-button type="danger" link>解绑</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Customer Addresses -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>客户地址</span>
          <el-button size="small" type="primary" @click="showAddressDialog(null, null)">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 添加地址
          </el-button>
        </div>
      </template>
      <el-table :data="addresses" border stripe size="small">
        <template #empty>
          <el-empty description="暂无地址" />
        </template>
        <el-table-column prop="recipientName" label="收件人姓名" width="120" />
        <el-table-column prop="recipientPhone" label="收件人电话" width="140" />
        <el-table-column prop="address" label="收件地址" min-width="200" />
        <el-table-column prop="isDefault" label="默认" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
            <span v-else style="color: #c0c4cc">否</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row, $index }">
            <el-button type="primary" link @click="showAddressDialog(row, $index)">编辑</el-button>
            <el-popconfirm title="确认删除此地址？" @confirm="deleteAddress(row, $index)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Bind Dialog -->
    <el-dialog v-model="bindDialog.visible" title="绑定销售账户" width="500px" destroy-on-close>
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="100px">
        <el-form-item label="销售账户" prop="salesAccountId">
          <el-select v-model="bindForm.salesAccountId" placeholder="请选择销售账户" style="width: 100%" filterable>
            <el-option v-for="a in availableAccounts" :key="a.id" :value="a.id" :label="`${a.displayName || a.accountName}`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitBind">保存</el-button>
      </template>
    </el-dialog>

    <!-- Address Dialog -->
    <el-dialog v-model="addressDialog.visible" :title="addressDialog.editIndex !== null ? '编辑地址' : '添加地址'" width="500px" destroy-on-close>
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="110px">
        <el-form-item label="收件人姓名" prop="recipientName">
          <el-input v-model="addressForm.recipientName" placeholder="请输入收件人姓名" />
        </el-form-item>
        <el-form-item label="收件人电话" prop="recipientPhone">
          <el-input v-model="addressForm.recipientPhone" placeholder="请输入收件人电话" />
        </el-form-item>
        <el-form-item label="收件地址" prop="address">
          <el-input v-model="addressForm.address" type="textarea" :rows="3" placeholder="请输入收件地址" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="addressSubmitting" @click="submitAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getCustomer, createCustomer, updateCustomer,
  listShippingAddresses, createShippingAddress, updateShippingAddress, deleteShippingAddress,
  getBoundAccounts, createBinding, createSelfBinding, unbind, listSalesAccounts, listMyAccounts
} from '../../api/customer'
import { useAuthStore } from '../../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)
const isEdit = computed(() => !!route.params.id)

// ===== Basic Info Form =====
const form = reactive({
  customerName: '', phone: '', addFriendTime: '', birthday: '', wechatAccount: '',
  level: 0, status: 1, remark: ''
})

const rules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^[\d\-+() ]{7,20}$/, message: '请输入有效的联系电话', trigger: 'blur' }
  ],
  addFriendTime: [{ required: true, message: '请选择加粉时间', trigger: 'change' }],
  wechatAccount: [{ required: true, message: '请输入微信号', trigger: 'blur' }]
}

// 生日月日分开选择
const birthMonth = ref('')
const birthDay = ref('')
const MAX_DAYS = { '01': 31, '02': 29, '03': 31, '04': 30, '05': 31, '06': 30, '07': 31, '08': 31, '09': 30, '10': 31, '11': 30, '12': 31 }
const dayOptions = computed(() => {
  const max = (birthMonth.value && MAX_DAYS[birthMonth.value]) || 31
  return Array.from({ length: max }, (_, i) => i + 1)
})

watch(birthMonth, (m) => {
  if (m && birthDay.value) {
    const max = MAX_DAYS[m] || 31
    if (parseInt(birthDay.value) > max) birthDay.value = ''
  }
})

watch([birthMonth, birthDay], ([m, d]) => {
  form.birthday = m && d ? `${m}-${d}` : ''
})

function parseBirthday(bd) {
  if (bd && /^\d{2}-\d{2}$/.test(bd)) {
    const parts = bd.split('-')
    birthMonth.value = parts[0]
    if (parseInt(parts[1]) <= (MAX_DAYS[parts[0]] || 31)) {
      birthDay.value = parts[1]
    }
  }
}

// ===== Sales Account Bindings =====
const boundAccounts = ref([])
const bindDialog = reactive({ visible: false })
const bindForm = reactive({ salesAccountId: null })
const bindRules = { salesAccountId: [{ required: true, message: '请选择销售账户', trigger: 'change' }] }
const bindSubmitting = ref(false)
const bindFormRef = ref(null)
const availableAccounts = ref([])

async function showBindDialog() {
  bindForm.salesAccountId = null
  try {
    const res = auth.hasRole('ADMIN')
      ? await listSalesAccounts()
      : await listMyAccounts()
    // 过滤掉已绑定的账户
    const boundIds = boundAccounts.value.map(b => b.salesAccountId)
    availableAccounts.value = (res.data || []).filter(a => a.status !== 0 && !boundIds.includes(a.id))
  } catch { /* ignore */ }
  bindDialog.visible = true
}

async function submitBind() {
  const valid = await bindFormRef.value.validate().catch(() => false)
  if (!valid) return
  bindSubmitting.value = true
  try {
    if (isEdit.value) {
      // Edit mode: call API immediately
      await createSelfBinding({ salesAccountId: bindForm.salesAccountId, customerId: route.params.id })
      ElMessage.success('绑定成功')
      boundAccounts.value = (await getBoundAccounts(route.params.id)).data || []
    } else {
      // Create mode: save locally, will be created in handleSubmit
      const account = availableAccounts.value.find(a => a.id === bindForm.salesAccountId)
      if (!account) return
      boundAccounts.value.push({
        salesAccountId: account.id,
        salesAccountName: account.accountName,
        salesAccountDisplayName: account.displayName
      })
    }
    bindDialog.visible = false
  } finally {
    bindSubmitting.value = false
  }
}

async function handleUnbind(row, index) {
  if (isEdit.value && row.id) {
    try {
      await unbind(row.id)
      ElMessage.success('已解绑')
    } catch { return }
  }
  boundAccounts.value.splice(index, 1)
}

// ===== Addresses =====
const addresses = ref([])
const addressDialog = reactive({ visible: false, editIndex: null })
const addressForm = reactive({ recipientName: '', recipientPhone: '', address: '', isDefault: 0 })
const addressRules = {
  recipientName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  recipientPhone: [{ required: true, message: '请输入收件人电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入收件地址', trigger: 'blur' }]
}
const addressSubmitting = ref(false)
const addressFormRef = ref(null)

function showAddressDialog(row, index) {
  addressDialog.editIndex = index
  if (row) {
    Object.assign(addressForm, { recipientName: row.recipientName, recipientPhone: row.recipientPhone, address: row.address, isDefault: row.isDefault })
  } else {
    Object.assign(addressForm, { recipientName: '', recipientPhone: '', address: '', isDefault: 0 })
  }
  addressDialog.visible = true
}

async function submitAddress() {
  const valid = await addressFormRef.value.validate().catch(() => false)
  if (!valid) return
  addressSubmitting.value = true
  try {
    const data = { recipientName: addressForm.recipientName, recipientPhone: addressForm.recipientPhone, address: addressForm.address, isDefault: addressForm.isDefault }

    if (isEdit.value) {
      // Edit mode: call API immediately
      const customerId = route.params.id
      if (addressDialog.editIndex !== null && addresses.value[addressDialog.editIndex]?.id) {
        await updateShippingAddress(addresses.value[addressDialog.editIndex].id, data)
      } else {
        await createShippingAddress(customerId, data)
      }
      addresses.value = (await listShippingAddresses(customerId)).data
    } else {
      // Create mode: save locally
      if (addressDialog.editIndex !== null) {
        // 编辑模式下保留原有 id（如果有的话，实际没有）
        addresses.value[addressDialog.editIndex] = { ...data }
      } else {
        addresses.value.push({ ...data })
      }
    }
    addressDialog.visible = false
  } finally {
    addressSubmitting.value = false
  }
}

async function deleteAddress(row, index) {
  if (isEdit.value && row.id) {
    try {
      await deleteShippingAddress(row.id)
      ElMessage.success('删除成功')
    } catch { return }
    const customerId = route.params.id
    addresses.value = (await listShippingAddresses(customerId)).data
  } else {
    addresses.value.splice(index, 1)
  }
}

function goBack() {
  if (route.query.from === 'order') {
    window.close()
  } else {
    router.back()
  }
}

// ===== Save =====
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!isEdit.value) {
    if (boundAccounts.value.length === 0) {
      ElMessage.warning('请至少绑定一个销售账户')
      return
    }
    if (addresses.value.length === 0) {
      ElMessage.warning('请至少添加一个收件地址')
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCustomer(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      // 1. Create customer — API returns new ID
      const newCustomerId = (await createCustomer(form)).data

      // 2. Create bindings
      for (const b of boundAccounts.value) {
        await createSelfBinding({ salesAccountId: b.salesAccountId, customerId: newCustomerId })
      }

      // 3. Create addresses
      for (const a of addresses.value) {
        await createShippingAddress(newCustomerId, {
          recipientName: a.recipientName,
          recipientPhone: a.recipientPhone,
          address: a.address,
          isDefault: a.isDefault
        })
      }
      ElMessage.success('创建成功')
      // 如果是从订单创建页跳转过来的，通知父窗口并关闭
      if (route.query.from === 'order' && window.opener) {
        window.opener.postMessage({
          type: 'customer-created',
          customerId: newCustomerId,
          customerName: form.customerName
        }, window.location.origin)
        window.close()
        return
      }
    }
    router.push('/customers')
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

// ===== Lifecycle =====
onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getCustomer(route.params.id)
    if (res.data.addFriendTime) {
      res.data.addFriendTime = res.data.addFriendTime.substring(0, 10)
    }
    Object.assign(form, res.data)
    parseBirthday(form.birthday)

    const [bRes, aRes] = await Promise.all([
      getBoundAccounts(route.params.id),
      listShippingAddresses(route.params.id)
    ])
    boundAccounts.value = bRes.data || []
    addresses.value = aRes.data || []
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
})
</script>

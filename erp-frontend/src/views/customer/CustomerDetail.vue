<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span>顾客详情</span>
          <el-button size="small" @click="$router.back()">
            <el-icon style="margin-right: 4px"><ArrowLeft /></el-icon> 返回
          </el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="客户名称" min-width="160">{{ customer.customerName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ customer.phone }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          <StatusTag :status="customer.level" :map="CUSTOMER_LEVEL_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="customer.status" :map="ENABLE_STATUS_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="加粉时间">{{ customer.addFriendTime ? customer.addFriendTime.substring(0, 10) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="顾客生日">{{ customer.birthday || '-' }}</el-descriptions-item>
        <el-descriptions-item label="微信号">{{ customer.wechatAccount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="顾客备注" :span="2">{{ customer.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ customer.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ customer.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>绑定销售账户</span>
          <el-button size="small" type="primary" @click="showBindDialog">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 绑定销售账户
          </el-button>
        </div>
      </template>
      <el-table :data="boundAccounts" border stripe size="small" v-loading="bindingLoading">
        <template #empty>
          <el-empty description="暂未绑定销售账户" />
        </template>
        <el-table-column prop="salesAccountName" label="账户名称" min-width="140" />
        <el-table-column prop="salesAccountDisplayName" label="显示名称" width="140" />
        <el-table-column prop="createdAt" label="绑定时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确认解绑？" @confirm="handleUnbind(row.id)">
              <template #reference>
                <el-button type="danger" link>解绑</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>客户地址</span>
          <el-button size="small" type="primary" @click="showAddressDialog(null)">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 添加地址
          </el-button>
        </div>
      </template>
      <el-table :data="addresses" border stripe size="small" v-loading="addressLoading">
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
          <template #default="{ row }">
            <el-button type="primary" link @click="showAddressDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除此地址？" @confirm="deleteAddress(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Bind Account Dialog -->
    <el-dialog v-model="bindDialog.visible" title="绑定销售账户" width="500px" destroy-on-close>
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="100px">
        <el-form-item label="销售账户" prop="salesAccountId">
          <el-select v-model="bindForm.salesAccountId" placeholder="请选择销售账户" style="width: 100%" filterable>
            <el-option
              v-for="a in availableAccounts"
              :key="a.id"
              :value="a.id"
              :label="`${a.displayName || a.accountName}`"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitBind">保存</el-button>
      </template>
    </el-dialog>

    <!-- Address Dialog -->
    <el-dialog v-model="addressDialog.visible" :title="addressDialog.isEdit ? '编辑地址' : '添加地址'" width="500px" destroy-on-close>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomer, listShippingAddresses, createShippingAddress, updateShippingAddress, deleteShippingAddress as delShippingAddress, getBoundAccounts, createBinding, createSelfBinding, unbind, listSalesAccounts, listMyAccounts } from '../../api/customer'
import { CUSTOMER_LEVEL_MAP, ENABLE_STATUS_MAP } from '../../constants'
import { useAuthStore } from '../../store/auth'
import StatusTag from '../../components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const customer = ref({})
const boundAccounts = ref([])
const addresses = ref([])
const bindingLoading = ref(false)
const addressLoading = ref(false)

const customerId = route.params.id

// Bind Account
const bindDialog = reactive({ visible: false })
const bindForm = reactive({ salesAccountId: null })
const bindRules = { salesAccountId: [{ required: true, message: '请选择销售账户', trigger: 'change' }] }
const bindSubmitting = ref(false)
const bindFormRef = ref(null)
const availableAccounts = ref([])

// Address
const addressDialog = reactive({ visible: false, isEdit: false, editId: null })
const addressForm = reactive({ recipientName: '', recipientPhone: '', address: '', isDefault: 0 })
const addressRules = {
  recipientName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  recipientPhone: [{ required: true, message: '请输入收件人电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入收件地址', trigger: 'blur' }]
}
const addressSubmitting = ref(false)
const addressFormRef = ref(null)

async function fetchData() {
  const [cRes, bRes, aRes] = await Promise.all([
    getCustomer(customerId), getBoundAccounts(customerId), listShippingAddresses(customerId)
  ])
  customer.value = cRes.data
  boundAccounts.value = bRes.data || []
  addresses.value = aRes.data
}

async function showBindDialog() {
  bindForm.salesAccountId = null
  try {
    const res = auth.hasRole('ADMIN')
      ? await listSalesAccounts()
      : await listMyAccounts()
    availableAccounts.value = (res.data || []).filter(a => a.status !== 0)
  } catch { /* ignore */ }
  bindDialog.visible = true
}

async function submitBind() {
  const valid = await bindFormRef.value.validate().catch(() => false)
  if (!valid) return
  bindSubmitting.value = true
  try {
    await createSelfBinding({ salesAccountId: bindForm.salesAccountId, customerId })
    ElMessage.success('绑定成功')
    bindDialog.visible = false
    bindingLoading.value = true
    boundAccounts.value = (await getBoundAccounts(customerId)).data || []
    bindingLoading.value = false
  } finally {
    bindSubmitting.value = false
  }
}

async function handleUnbind(id) {
  try {
    await unbind(id)
    ElMessage.success('已解绑')
    boundAccounts.value = boundAccounts.value.filter(b => b.id !== id)
  } catch { /* handled by interceptor */ }
}

function showAddressDialog(row) {
  addressDialog.isEdit = !!row
  addressDialog.editId = row?.id || null
  Object.assign(addressForm, row ? { ...row } : { recipientName: '', recipientPhone: '', address: '', isDefault: 0 })
  addressDialog.visible = true
}

async function submitAddress() {
  const valid = await addressFormRef.value.validate().catch(() => false)
  if (!valid) return
  addressSubmitting.value = true
  try {
    const data = { ...addressForm, customerId }
    if (addressDialog.isEdit) {
      await updateShippingAddress(addressDialog.editId, data)
      ElMessage.success('更新成功')
    } else {
      await createShippingAddress(customerId, data)
      ElMessage.success('添加成功')
    }
    addressDialog.visible = false
    addressLoading.value = true
    addresses.value = (await listShippingAddresses(customerId)).data
    addressLoading.value = false
  } finally {
    addressSubmitting.value = false
  }
}

async function deleteAddress(id) {
  await delShippingAddress(id)
  ElMessage.success('删除成功')
  addressLoading.value = true
  addresses.value = (await listShippingAddresses(customerId)).data
  addressLoading.value = false
}

onMounted(fetchData)
</script>

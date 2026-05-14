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
        <el-descriptions-item label="邮箱">{{ customer.email }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          <StatusTag :status="customer.level" :map="CUSTOMER_LEVEL_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="customer.status" :map="ENABLE_STATUS_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="地址">{{ customer.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ customer.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ customer.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ customer.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>付款渠道</span>
          <el-button size="small" type="primary" @click="showChannelDialog(null)">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 添加渠道
          </el-button>
        </div>
      </template>
      <el-table :data="channels" border stripe size="small" v-loading="channelLoading">
        <template #empty>
          <el-empty description="暂无付款渠道" />
        </template>
        <el-table-column prop="channelType" label="类型" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.channelType" :map="CHANNEL_TYPE_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="channelAccount" label="账号" min-width="140" />
        <el-table-column prop="accountName" label="户名" width="100" />
        <el-table-column prop="bankName" label="开户行" width="160" />
        <el-table-column prop="isDefault" label="默认" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
            <span v-else style="color: #c0c4cc">否</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showChannelDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除此付款渠道？" @confirm="deleteChannel(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="flex-between">
          <span>联系人</span>
          <el-button size="small" type="primary" @click="showContactDialog(null)">
            <el-icon style="margin-right: 4px"><Plus /></el-icon> 添加联系人
          </el-button>
        </div>
      </template>
      <el-table :data="contacts" border stripe size="small" v-loading="contactLoading">
        <template #empty>
          <el-empty description="暂无联系人" />
        </template>
        <el-table-column prop="contactName" label="姓名" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column prop="position" label="职位" width="120" />
        <el-table-column prop="isPrimary" label="主要" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary" type="success" size="small">是</el-tag>
            <span v-else style="color: #c0c4cc">否</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showContactDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除此联系人？" @confirm="deleteContact(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Channel Dialog -->
    <el-dialog v-model="channelDialog.visible" :title="channelDialog.isEdit ? '编辑渠道' : '添加渠道'" width="500px" destroy-on-close>
      <el-form ref="channelFormRef" :model="channelForm" :rules="channelRules" label-width="100px">
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="channelForm.channelType" style="width: 100%" placeholder="请选择渠道类型">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="银行卡" value="BANK_CARD" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="channelAccount">
          <el-input v-model="channelForm.channelAccount" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="户名" prop="accountName">
          <el-input v-model="channelForm.accountName" placeholder="请输入户名" />
        </el-form-item>
        <el-form-item label="开户行" prop="bankName">
          <el-input v-model="channelForm.bankName" placeholder="请输入开户行" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-switch v-model="channelForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="channelForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="channelSubmitting" @click="submitChannel">保存</el-button>
      </template>
    </el-dialog>

    <!-- Contact Dialog -->
    <el-dialog v-model="contactDialog.visible" :title="contactDialog.isEdit ? '编辑联系人' : '添加联系人'" width="500px" destroy-on-close>
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="100px">
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="contactForm.contactName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="contactForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="contactForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="contactForm.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="主要联系人">
          <el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="contactForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="contactSubmitting" @click="submitContact">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomer, listChannels, createChannel, updateChannel, deleteChannel as delChannel, listContacts, createContact, updateContact, deleteContact as delContact } from '../../api/customer'
import { CUSTOMER_LEVEL_MAP, ENABLE_STATUS_MAP, CHANNEL_TYPE_MAP } from '../../constants'
import StatusTag from '../../components/StatusTag.vue'

const route = useRoute()
const router = useRouter()

const customer = ref({})
const channels = ref([])
const contacts = ref([])
const channelLoading = ref(false)
const contactLoading = ref(false)

const customerId = route.params.id

// Channel
const channelDialog = reactive({ visible: false, isEdit: false, editId: null })
const channelForm = reactive({ channelType: '', channelAccount: '', accountName: '', bankName: '', isDefault: 0, status: 1 })
const channelRules = {
  channelType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  channelAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }]
}
const channelSubmitting = ref(false)
const channelFormRef = ref(null)

// Contact
const contactDialog = reactive({ visible: false, isEdit: false, editId: null })
const contactForm = reactive({ contactName: '', phone: '', email: '', position: '', isPrimary: 0, remark: '' })
const contactRules = { contactName: [{ required: true, message: '请输入姓名', trigger: 'blur' }] }
const contactSubmitting = ref(false)
const contactFormRef = ref(null)

async function fetchData() {
  const [cRes, chRes, coRes] = await Promise.all([
    getCustomer(customerId), listChannels(customerId), listContacts(customerId)
  ])
  customer.value = cRes.data
  channels.value = chRes.data
  contacts.value = coRes.data
}

function showChannelDialog(row) {
  channelDialog.isEdit = !!row
  channelDialog.editId = row?.id || null
  Object.assign(channelForm, row ? { ...row } : { channelType: '', channelAccount: '', accountName: '', bankName: '', isDefault: 0, status: 1 })
  channelDialog.visible = true
}

async function submitChannel() {
  const valid = await channelFormRef.value.validate().catch(() => false)
  if (!valid) return
  channelSubmitting.value = true
  try {
    if (channelDialog.isEdit) {
      await updateChannel(customerId, channelDialog.editId, channelForm)
      ElMessage.success('更新成功')
    } else {
      await createChannel(customerId, channelForm)
      ElMessage.success('添加成功')
    }
    channelDialog.visible = false
    channelLoading.value = true
    channels.value = (await listChannels(customerId)).data
    channelLoading.value = false
  } finally {
    channelSubmitting.value = false
  }
}

async function deleteChannel(id) {
  await delChannel(customerId, id)
  ElMessage.success('删除成功')
  channelLoading.value = true
  channels.value = (await listChannels(customerId)).data
  channelLoading.value = false
}

function showContactDialog(row) {
  contactDialog.isEdit = !!row
  contactDialog.editId = row?.id || null
  Object.assign(contactForm, row ? { ...row } : { contactName: '', phone: '', email: '', position: '', isPrimary: 0, remark: '' })
  contactDialog.visible = true
}

async function submitContact() {
  const valid = await contactFormRef.value.validate().catch(() => false)
  if (!valid) return
  contactSubmitting.value = true
  try {
    if (contactDialog.isEdit) {
      await updateContact(customerId, contactDialog.editId, contactForm)
      ElMessage.success('更新成功')
    } else {
      await createContact(customerId, contactForm)
      ElMessage.success('添加成功')
    }
    contactDialog.visible = false
    contactLoading.value = true
    contacts.value = (await listContacts(customerId)).data
    contactLoading.value = false
  } finally {
    contactSubmitting.value = false
  }
}

async function deleteContact(id) {
  await delContact(customerId, id)
  ElMessage.success('删除成功')
  contactLoading.value = true
  contacts.value = (await listContacts(customerId)).data
  contactLoading.value = false
}

onMounted(fetchData)
</script>

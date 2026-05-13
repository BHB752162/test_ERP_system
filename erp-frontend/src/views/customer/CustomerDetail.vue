<template>
  <div>
    <el-card>
      <template #header>
        <span>顾客详情</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="客户名称">{{ customer.customerName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ customer.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ customer.email }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          <StatusTag :status="customer.level" :map="levelMap" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="customer.status" :map="statusMap" />
        </el-descriptions-item>
        <el-descriptions-item label="地址">{{ customer.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ customer.remark }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ customer.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ customer.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>付款渠道</span>
      </template>
      <el-button size="small" type="primary" style="margin-bottom: 10px" @click="showChannelDialog(null)">添加渠道</el-button>
      <el-table :data="channels" border stripe size="small">
        <el-table-column prop="channelType" label="类型" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.channelType" :map="channelTypeMap" />
          </template>
        </el-table-column>
        <el-table-column prop="channelAccount" label="账号" min-width="140" />
        <el-table-column prop="accountName" label="户名" width="100" />
        <el-table-column prop="bankName" label="开户行" width="160" />
        <el-table-column prop="isDefault" label="默认" width="70">
          <template #default="{ row }">{{ row.isDefault ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="statusMap" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showChannelDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="deleteChannel(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>联系人</span>
      </template>
      <el-button size="small" type="primary" style="margin-bottom: 10px" @click="showContactDialog(null)">添加联系人</el-button>
      <el-table :data="contacts" border stripe size="small">
        <el-table-column prop="contactName" label="姓名" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column prop="position" label="职位" width="120" />
        <el-table-column prop="isPrimary" label="主要" width="70">
          <template #default="{ row }">{{ row.isPrimary ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showContactDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="deleteContact(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Channel Dialog -->
    <el-dialog v-model="channelDialog.visible" :title="channelDialog.isEdit ? '编辑渠道' : '添加渠道'" width="500px">
      <el-form ref="channelFormRef" :model="channelForm" :rules="channelRules" label-width="100px">
        <el-form-item label="渠道类型" prop="channelType">
          <el-select v-model="channelForm.channelType" style="width: 100%">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="银行卡" value="BANK_CARD" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="channelAccount">
          <el-input v-model="channelForm.channelAccount" />
        </el-form-item>
        <el-form-item label="户名" prop="accountName">
          <el-input v-model="channelForm.accountName" />
        </el-form-item>
        <el-form-item label="开户行" prop="bankName">
          <el-input v-model="channelForm.bankName" />
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
    <el-dialog v-model="contactDialog.visible" :title="contactDialog.isEdit ? '编辑联系人' : '添加联系人'" width="500px">
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="100px">
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="contactForm.contactName" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="contactForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="contactForm.email" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="contactForm.position" />
        </el-form-item>
        <el-form-item label="主要联系人">
          <el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="contactForm.remark" type="textarea" :rows="2" />
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomer, listChannels, createChannel, updateChannel, deleteChannel as delChannel, listContacts, createContact, updateContact, deleteContact as delContact } from '../../api/customer'
import StatusTag from '../../components/StatusTag.vue'

const route = useRoute()
const levelMap = { 0: { label: '普通', type: 'info' }, 1: { label: '银卡', type: 'success' }, 2: { label: '金卡', type: 'warning' }, 3: { label: '钻石', type: 'danger' } }
const statusMap = { 0: { label: '停用', type: 'danger' }, 1: { label: '启用', type: 'success' } }
const channelTypeMap = { ALIPAY: { label: '支付宝', type: 'primary' }, WECHAT: { label: '微信', type: 'success' }, BANK_CARD: { label: '银行卡', type: 'warning' }, CASH: { label: '现金', type: 'info' }, OTHER: { label: '其他', type: 'info' } }

const customer = ref({})
const channels = ref([])
const contacts = ref([])

const customerId = route.params.id

// Channel
const channelDialog = reactive({ visible: false, isEdit: false, editId: null })
const channelForm = reactive({ channelType: '', channelAccount: '', accountName: '', bankName: '', isDefault: 0, status: 1 })
const channelRules = { channelType: [{ required: true, message: '请选择类型', trigger: 'change' }] }
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
  if (row) Object.assign(channelForm, row)
  else Object.assign(channelForm, { channelType: '', channelAccount: '', accountName: '', bankName: '', isDefault: 0, status: 1 })
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
    channels.value = (await listChannels(customerId)).data
  } finally {
    channelSubmitting.value = false
  }
}

async function deleteChannel(id) {
  await delChannel(customerId, id)
  ElMessage.success('删除成功')
  channels.value = (await listChannels(customerId)).data
}

function showContactDialog(row) {
  contactDialog.isEdit = !!row
  contactDialog.editId = row?.id || null
  if (row) Object.assign(contactForm, row)
  else Object.assign(contactForm, { contactName: '', phone: '', email: '', position: '', isPrimary: 0, remark: '' })
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
    contacts.value = (await listContacts(customerId)).data
  } finally {
    contactSubmitting.value = false
  }
}

async function deleteContact(id) {
  await delContact(customerId, id)
  ElMessage.success('删除成功')
  contacts.value = (await listContacts(customerId)).data
}

onMounted(fetchData)
</script>

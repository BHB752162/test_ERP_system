<template>
  <div>
    <el-card>
      <el-form :model="query" inline>
        <el-form-item label="微信号">
          <el-input v-model="query.keyword" placeholder="微信号/昵称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="showDialog(null)">新增微信号</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="wechatAccount" label="微信号" min-width="130" />
        <el-table-column prop="wechatNickname" label="微信昵称" width="120" />
        <el-table-column prop="salesPersonName" label="所属销售" width="120" />
        <el-table-column prop="qrCode" label="二维码" width="100">
          <template #default="{ row }">
            <el-image v-if="row.qrCode" :src="row.qrCode" style="width: 40px; height: 40px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="statusMap" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
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
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <Pagination :page="query.page" :page-size="query.pageSize" :total="total" @change="onPageChange" />
      </div>
    </el-card>

    <!-- Dialog -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑微信号' : '新增微信号'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="微信号" prop="wechatAccount">
          <el-input v-model="form.wechatAccount" />
        </el-form-item>
        <el-form-item label="微信昵称" prop="wechatNickname">
          <el-input v-model="form.wechatNickname" />
        </el-form-item>
        <el-form-item label="所属销售" prop="salesPersonId">
          <el-select v-model="form.salesPersonId" placeholder="请选择销售" filterable style="width: 100%">
            <el-option v-for="u in salesPersons" :key="u.id" :value="u.id" :label="u.realName || u.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="二维码" prop="qrCode">
          <el-input v-model="form.qrCode" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listWechats, createWechat, updateWechat, deleteWechat } from '../../api/wechat'
import { listUsers } from '../../api/user'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const statusMap = { 0: { label: '禁用', type: 'danger' }, 1: { label: '启用', type: 'success' } }

const list = ref([])
const total = ref(0)
const loading = ref(false)
const salesPersons = ref([])
const submitting = ref(false)
const formRef = ref(null)

const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const dialog = reactive({ visible: false, isEdit: false, editId: null })
const form = reactive({ wechatAccount: '', wechatNickname: '', salesPersonId: null, qrCode: '', status: 1 })
const rules = {
  wechatAccount: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
  salesPersonId: [{ required: true, message: '请选择所属销售', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listWechats(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.page = 1; fetchData() }
function onPageChange(page, pageSize) { query.page = page; query.pageSize = pageSize; fetchData() }

function showDialog(row) {
  dialog.isEdit = !!row
  dialog.editId = row?.id || null
  if (row) Object.assign(form, row)
  else Object.assign(form, { wechatAccount: '', wechatNickname: '', salesPersonId: null, qrCode: '', status: 1 })
  dialog.visible = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialog.isEdit) {
      await updateWechat(dialog.editId, form)
      ElMessage.success('更新成功')
    } else {
      await createWechat(form)
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  await deleteWechat(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(async () => {
  fetchData()
  const res = await listUsers({ page: 1, pageSize: 999 })
  salesPersons.value = res.data.records || []
})
</script>

<template>
  <div>
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="微信号">
          <el-input v-model="query.keyword" placeholder="微信号/昵称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search" :loading="loading">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="table-actions">
        <span></span>
        <el-button type="primary" @click="showDialog(null)">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 新增微信号
        </el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <template #empty>
          <el-empty description="暂无微信号数据" />
        </template>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="wechatAccount" label="微信号" min-width="130" />
        <el-table-column prop="wechatNickname" label="微信昵称" width="120" />
        <el-table-column prop="salesPersonName" label="所属销售" width="120" />
        <el-table-column prop="qrCode" label="二维码" width="100">
          <template #default="{ row }">
            <el-image v-if="row.qrCode" :src="row.qrCode" style="width: 40px; height: 40px" fit="cover" alt="二维码" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column prop="createdByName" label="创建人" min-width="100" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column prop="updatedByName" label="更新人" min-width="100" />
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑微信号' : '新增微信号'" width="500px" @close="closeDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="微信号" prop="wechatAccount">
          <el-input v-model="formData.wechatAccount" />
        </el-form-item>
        <el-form-item label="微信昵称" prop="wechatNickname">
          <el-input v-model="formData.wechatNickname" />
        </el-form-item>
        <el-form-item label="所属销售" prop="salesPersonId">
          <el-select v-model="formData.salesPersonId" placeholder="请选择销售" filterable style="width: 100%">
            <el-option v-for="u in salesPersons" :key="u.id" :value="u.id" :label="u.realName || u.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="二维码" prop="qrCode">
          <el-input v-model="formData.qrCode" placeholder="图片URL" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listWechats, createWechat, updateWechat, deleteWechat } from '../../api/wechat'
import { listUsers } from '../../api/user'
import { useCrudList, useDeleteAction } from '../../composables/useCrudList'
import { ENABLE_STATUS_MAP } from '../../constants'
import Pagination from '../../components/Pagination.vue'
import StatusTag from '../../components/StatusTag.vue'

const { list, total, loading, query, fetchData, search, reset, onPageChange } = useCrudList(listWechats, { defaultQuery: { keyword: '' } })
const { handleDelete } = useDeleteAction(deleteWechat, fetchData)

const salesPersons = ref([])
const submitting = ref(false)
const formRef = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)

const defaultForm = { wechatAccount: '', wechatNickname: '', salesPersonId: null, qrCode: '', status: 1 }
const formData = reactive({ ...defaultForm })
const rules = {
  wechatAccount: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
  salesPersonId: [{ required: true, message: '请选择所属销售', trigger: 'change' }]
}

function showDialog(row) {
  isEdit.value = !!row
  for (const key in formData) {
    delete formData[key]
  }
  Object.assign(formData, row ? { ...row } : { ...defaultForm })
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  if (formRef.value) formRef.value.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateWechat(formData.id, formData)
    } else {
      await createWechat(formData)
    }
    closeDialog()
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  fetchData()
  try {
    const res = await listUsers({ page: 1, pageSize: 200 })
    salesPersons.value = res.data.records || []
  } catch { /* ignore */ }
})
</script>

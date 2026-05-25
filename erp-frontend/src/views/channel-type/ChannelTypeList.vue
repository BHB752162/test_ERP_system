<template>
  <el-card shadow="never">
    <template #header>
      <div class="flex-between">
        <span>付款渠道类型管理</span>
        <el-button type="primary" size="small" @click="showDialog(null)">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 新增类型
        </el-button>
      </div>
    </template>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
    <el-table :data="list" border stripe v-loading="loading" row-key="id">
      <template #empty>
        <el-empty :description="error || '暂无数据'" />
      </template>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="typeCode" label="编码" width="120" />
      <el-table-column prop="typeName" label="名称" min-width="160" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="ENABLE_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="添加时间" width="160" />
      <el-table-column prop="createdByName" label="添加人" min-width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="160" />
      <el-table-column prop="updatedByName" label="更新人" min-width="100" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除？删除后已有渠道数据不受影响" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑渠道类型' : '新增渠道类型'" width="450px" destroy-on-close @close="closeDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="typeCode">
          <el-input v-model="formData.typeCode" :disabled="isEdit" placeholder="如 ALIPAY, WECHAT" />
        </el-form-item>
        <el-form-item label="名称" prop="typeName">
          <el-input v-model="formData.typeName" placeholder="如 支付宝, 微信支付" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
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
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listChannelTypes, createChannelType, updateChannelType, deleteChannelType } from '../../api/channelType'
import { ENABLE_STATUS_MAP } from '../../constants'
import StatusTag from '../../components/StatusTag.vue'

const list = ref([])
const loading = ref(false)
const error = ref('')
const submitting = ref(false)
const formRef = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const defaultForm = { typeCode: '', typeName: '', sortOrder: 0, status: 1 }
const formData = reactive({ ...defaultForm })
const rules = {
  typeCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const res = await listChannelTypes()
    list.value = res.data
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function showDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id || null
  // 彻底清除旧属性，防止残留 id/createdAt 等字段污染新建请求
  for (const key in formData) {
    delete formData[key]
  }
  Object.assign(formData, row ? { ...row } : { ...defaultForm })
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateChannelType(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createChannelType(formData)
      ElMessage.success('创建成功')
    }
    closeDialog()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteChannelType(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* handled by interceptor */ }
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <template #header>
      <span>{{ isEdit ? '编辑顾客' : '新增顾客' }}</span>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px">
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="form.customerName" />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="等级" prop="level">
        <el-select v-model="form.level">
          <el-option :value="0" label="普通" />
          <el-option :value="1" label="银卡" />
          <el-option :value="2" label="金卡" />
          <el-option :value="3" label="钻石" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { getCustomer, createCustomer, updateCustomer } from '../../api/customer'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  customerName: '', phone: '', email: '', address: '',
  level: 0, status: 1, remark: ''
})

const rules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

onMounted(async () => {
  if (isEdit.value) {
    const res = await getCustomer(route.params.id)
    Object.assign(form, res.data)
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCustomer(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      await createCustomer(form)
      ElMessage.success('创建成功')
    }
    router.push('/customers')
  } finally {
    submitting.value = false
  }
}
</script>

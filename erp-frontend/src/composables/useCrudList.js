import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * CRUD 列表页通用组合函数
 * @param {Function} fetchApi - (params) => Promise 获取列表数据的 API 函数
 * @param {Object} options - 可选配置
 * @param {Object} options.defaultQuery - 默认查询条件
 * @param {Function} options.onSuccess - 获取成功后的回调，接收 response
 * @param {Function} options.onError - 获取失败后的回调
 */
export function useCrudList(fetchApi, options = {}) {
  const { defaultQuery = {}, onSuccess, onError } = options

  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const query = reactive({
    keyword: '',
    page: 1,
    pageSize: 10,
    ...defaultQuery
  })

  async function fetchData() {
    loading.value = true
    try {
      const res = await fetchApi(query)
      const data = res.data || res
      list.value = data.records || []
      total.value = data.total || 0
      onSuccess?.(res)
    } catch (err) {
      onError?.(err)
    } finally {
      loading.value = false
    }
  }

  function search() {
    query.page = 1
    fetchData()
  }

  function reset() {
    Object.assign(query, { ...defaultQuery, keyword: '', page: 1, pageSize: 10 })
    fetchData()
  }

  function onPageChange(page, pageSize) {
    query.page = page
    query.pageSize = pageSize
    fetchData()
  }

  return {
    list,
    total,
    loading,
    query,
    fetchData,
    search,
    reset,
    onPageChange
  }
}

/**
 * 对话框表单通用组合函数
 * @param {Function} createApi - (data) => Promise
 * @param {Function} updateApi - (id, data) => Promise
 * @param {Function} onSuccess - 操作成功回调
 */
export function useDialogForm(createApi, updateApi, onSuccess) {
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const editId = ref(null)
  const submitting = ref(false)
  const formRef = ref(null)

  const formData = reactive({})

  function initForm(initValues = {}) {
    Object.assign(formData, initValues)
  }

  function openCreate() {
    isEdit.value = false
    editId.value = null
    if (formRef.value) formRef.value.resetFields()
    dialogVisible.value = true
  }

  function openEdit(row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(formData, { ...row })
    dialogVisible.value = true
  }

  function closeDialog() {
    dialogVisible.value = false
    editId.value = null
  }

  async function handleSubmit() {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateApi(editId.value, { ...formData })
      } else {
        await createApi({ ...formData })
      }
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      closeDialog()
      onSuccess?.()
    } catch {
      // 错误由全局拦截器处理
    } finally {
      submitting.value = false
    }
  }

  return {
    dialogVisible,
    isEdit,
    editId,
    submitting,
    formRef,
    formData,
    initForm,
    openCreate,
    openEdit,
    closeDialog,
    handleSubmit
  }
}

/**
 * 删除操作通用函数
 * @param {Function} deleteApi - (id) => Promise
 * @param {Function} onSuccess - 删除成功回调
 */
export function useDeleteAction(deleteApi, onSuccess) {
  async function handleDelete(id) {
    try {
      await deleteApi(id)
      ElMessage.success('删除成功')
      onSuccess?.()
    } catch {
      // 错误由全局拦截器处理
    }
  }

  return { handleDelete }
}

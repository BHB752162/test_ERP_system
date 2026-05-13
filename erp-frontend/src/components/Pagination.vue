<template>
  <el-pagination
    v-model:current-page="current"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 50, 100]"
    :total="total"
    layout="total, sizes, prev, pager, next, jumper"
    background
    small
    @change="$emit('change', current, pageSize)"
  />
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})

defineEmits(['change'])

const current = ref(props.page)
const pageSize = ref(props.pageSize)

watch(() => props.page, v => current.value = v)
watch(() => props.pageSize, v => pageSize.value = v)
</script>

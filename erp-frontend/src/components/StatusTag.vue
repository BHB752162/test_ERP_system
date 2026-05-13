<template>
  <el-tag :type="type" size="small" effect="plain">{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: [String, Number], required: true },
  map: { type: Object, required: true }
})

const type = computed(() => props.map[props.status]?.type || 'info')
const label = computed(() => props.map[props.status]?.label || props.status)
</script>

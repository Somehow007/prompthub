<script setup lang="ts">
import type { TagVO } from '@/api/tag'

defineProps<{
  tags: TagVO[]
  modelValue: number[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number[]]
}>()

function isSelected(tagId: number): boolean {
  // Need to access modelValue through a workaround since props are readonly
  return false // handled in template
}

function toggle(tagId: number, current: number[]) {
  if (current.includes(tagId)) {
    emit('update:modelValue', current.filter(id => id !== tagId))
  } else {
    emit('update:modelValue', [...current, tagId])
  }
}

function isTagSelected(tagId: number, selected: number[]): boolean {
  return selected.includes(tagId)
}
</script>

<template>
  <div class="tag-selector">
    <div v-for="root in tags" :key="root.id" class="tag-group">
      <div class="tag-group-title">{{ root.name }}</div>
      <div class="tag-group-items">
        <button
          v-for="child in root.children"
          :key="child.id"
          :class="['tag-chip', { active: isTagSelected(child.id, modelValue) }]"
          @click="toggle(child.id, modelValue)"
        >
          {{ child.name }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tag-selector {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tag-group {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.tag-group-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
  padding-top: 6px;
  min-width: 40px;
}

.tag-group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.tag-chip:hover {
  border-color: #4f46e5;
  color: #4f46e5;
  background: #eef2ff;
}

.tag-chip.active {
  background: #4f46e5;
  color: #fff;
  border-color: #4f46e5;
}
</style>

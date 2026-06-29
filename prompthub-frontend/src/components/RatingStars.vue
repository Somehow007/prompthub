<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: number
  readonly?: boolean
  size?: number
}>(), {
  readonly: false,
  size: 18,
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const hoverIndex = ref(0)

const stars = computed(() => {
  const display = hoverIndex.value || props.modelValue
  return Array.from({ length: 5 }, (_, i) => ({
    index: i + 1,
    filled: i < display,
  }))
})

function handleClick(index: number) {
  if (props.readonly) return
  emit('update:modelValue', index)
}

function handleEnter(index: number) {
  if (props.readonly) return
  hoverIndex.value = index
}

function handleLeave() {
  hoverIndex.value = 0
}

function color(index: number): string {
  if (props.readonly) {
    return index <= props.modelValue ? '#f59e0b' : '#d1d5db'
  }
  if (hoverIndex.value > 0) {
    return index <= hoverIndex.value ? '#f59e0b' : '#d1d5db'
  }
  return index <= props.modelValue ? '#f59e0b' : '#d1d5db'
}
</script>

<template>
  <span class="stars" :style="{ fontSize: size + 'px' }">
    <span
      v-for="star in stars"
      :key="star.index"
      class="star"
      :class="{ readonly, clickable: !readonly }"
      :style="{ color: color(star.index), cursor: readonly ? 'default' : 'pointer' }"
      @click="handleClick(star.index)"
      @mouseenter="handleEnter(star.index)"
      @mouseleave="handleLeave"
    >★</span>
  </span>
</template>

<style scoped>
.stars {
  display: inline-flex;
  gap: 2px;
  line-height: 1;
}

.star {
  transition: color 0.15s, transform 0.1s;
}

.star.clickable:hover {
  transform: scale(1.15);
}
</style>

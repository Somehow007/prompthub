<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

const keyword = ref('')
const emit = defineEmits<{
  search: [keyword: string]
}>()

function handleSearch() {
  emit('search', keyword.value.trim())
}

function handleClear() {
  keyword.value = ''
  emit('search', '')
}
</script>

<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      size="large"
      placeholder="搜索提示词模板..."
      :prefix-icon="Search"
      class="search-input"
      @keyup.enter="handleSearch"
      clearable
      @clear="handleClear"
    >
      <template #append>
        <el-button type="primary" @click="handleSearch">
          搜索
        </el-button>
      </template>
    </el-input>
  </div>
</template>

<style scoped>
.search-bar {
  max-width: 640px;
  margin: 0 auto;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px 0 0 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  transition: box-shadow 0.2s;
}

.search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4f46e5 inset;
}

.search-input :deep(.el-button) {
  border-radius: 0 10px 10px 0;
  font-weight: 500;
}
</style>

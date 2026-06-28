<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(async () => {
  // 如果有本地 Token，自动拉取用户信息
  if (userStore.token) {
    await userStore.fetchUserInfo()
  }
})
</script>

<template>
  <router-view />
</template>

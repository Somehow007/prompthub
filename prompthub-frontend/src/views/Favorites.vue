<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getFavorites } from '@/api/favorite'
import type { TemplateVO } from '@/api/template'
import TemplateCard from '@/components/TemplateCard.vue'

const router = useRouter()
const templates = ref<TemplateVO[]>([])
const loading = ref(true)

async function loadFavorites() {
  try {
    templates.value = await getFavorites()
  } catch {
    // 忽略
  } finally {
    loading.value = false
  }
}

function goToDetail(id: number) {
  router.push(`/template/${id}`)
}

onMounted(() => {
  loadFavorites()
})
</script>

<template>
  <div class="page">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <div class="logo-mark">P</div>
          <span class="logo-text">PromptHub</span>
        </router-link>
        <router-link to="/" class="back-link">← 返回广场</router-link>
      </div>
    </header>

    <main class="main">
      <h1 class="page-title">我的收藏</h1>

      <div v-if="loading" class="loading-text">加载中...</div>

      <div v-else-if="templates.length > 0" class="template-grid">
        <TemplateCard
          v-for="t in templates"
          :key="t.id"
          :template="t"
          @click="goToDetail"
        />
      </div>

      <div v-else class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.5">
          <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
        </svg>
        <p>还没有收藏任何模板</p>
        <router-link to="/" class="browse-link">去广场逛逛</router-link>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
}

.header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
}

.header-inner {
  width: 1100px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: #4f46e5;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.back-link {
  font-size: 14px;
  color: #64748b;
  text-decoration: none;
}

.back-link:hover {
  color: #4f46e5;
}

.main {
  width: 1100px;
  max-width: 100%;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 24px;
}

.loading-text {
  text-align: center;
  color: #94a3b8;
  padding: 60px 0;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #94a3b8;
  font-size: 15px;
}

.empty-state p {
  margin: 16px 0;
}

.browse-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
}

.browse-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>

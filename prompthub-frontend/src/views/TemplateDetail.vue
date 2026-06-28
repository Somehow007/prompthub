<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTemplateDetail, type TemplateDetailVO } from '@/api/template'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const template = ref<TemplateDetailVO | null>(null)
const loading = ref(true)

function formatPrice(price: number): string {
  return price > 0 ? `¥${price.toFixed(2)}` : '免费'
}

function statusText(status: number): string {
  if (status === 1) return '上架中'
  if (status === 0) return '已下架'
  return '审核中'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function isOwner(): boolean {
  return userStore.userInfo?.id === template.value?.creatorId
}

function goToEdit() {
  router.push(`/template/${template.value!.id}/edit`)
}

function goToHistory() {
  router.push(`/template/${template.value!.id}/history`)
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    template.value = await getTemplateDetail(id)
  } catch {
    ElMessage.error('模板加载失败')
  } finally {
    loading.value = false
  }
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

    <main v-if="!loading && template" class="main">
      <div class="detail-layout">
        <!-- 左侧内容 -->
        <div class="detail-main">
          <!-- 标题区 -->
          <div class="title-section">
            <h1 class="title">{{ template.title }}</h1>
            <div class="meta">
              <span class="meta-badge" :class="template.price === 0 ? 'free' : 'paid'">
                {{ formatPrice(template.price) }}
              </span>
              <span class="meta-badge status">{{ statusText(template.status) }}</span>
            </div>
          </div>

          <!-- 创作者 -->
          <div class="creator-row">
            <span class="creator-label">创作者：</span>
            <span class="creator-name">{{ template.creatorName }}</span>
          </div>

          <!-- 描述 -->
          <div v-if="template.description" class="section">
            <h2 class="section-title">适用场景</h2>
            <p class="description">{{ template.description }}</p>
          </div>

          <!-- 标签 -->
          <div v-if="template.tags?.length" class="section">
            <h2 class="section-title">标签</h2>
            <div class="tags">
              <span v-for="tag in template.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
            </div>
          </div>

          <!-- Prompt 内容 -->
          <div class="section">
            <h2 class="section-title">
              Prompt 内容
              <span class="version-badge">v{{ template.currentVersion }}</span>
            </h2>
            <div class="prompt-box">
              <pre class="prompt-text">{{ template.promptContent }}</pre>
            </div>
          </div>

          <!-- 变更说明 -->
          <div v-if="template.changeNote" class="section">
            <h2 class="section-title">版本变更说明</h2>
            <p class="change-note">{{ template.changeNote }}</p>
          </div>

          <!-- 操作区 -->
          <div v-if="isOwner()" class="actions">
            <el-button type="primary" @click="goToEdit">编辑模板</el-button>
            <el-button @click="goToHistory">版本历史</el-button>
          </div>
        </div>

        <!-- 右侧统计 -->
        <aside class="detail-sidebar">
          <div class="stats-card">
            <div class="stat-item">
              <span class="stat-value">{{ template.useCount }}</span>
              <span class="stat-label">使用次数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ template.avgRating > 0 ? template.avgRating.toFixed(1) : '-' }}</span>
              <span class="stat-label">平均评分</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ template.favoriteCount }}</span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ template.reviewCount }}</span>
              <span class="stat-label">评价</span>
            </div>
          </div>

          <div class="date-card">
            <div class="date-row">
              <span class="date-label">创建时间</span>
              <span class="date-value">{{ formatDate(template.createdAt) }}</span>
            </div>
            <div class="date-row">
              <span class="date-label">更新时间</span>
              <span class="date-value">{{ formatDate(template.updatedAt) }}</span>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <main v-else-if="!loading" class="main empty">
      <p>模板不存在</p>
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

/* 头部 */
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

/* 主体 */
.main {
  width: 1100px;
  max-width: 100%;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.detail-layout {
  display: flex;
  gap: 40px;
}

.detail-main {
  flex: 1;
  min-width: 0;
}

.empty {
  text-align: center;
  color: #94a3b8;
  padding-top: 80px;
}

/* 标题 */
.title-section {
  margin-bottom: 12px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
  letter-spacing: -0.02em;
}

.meta {
  display: flex;
  gap: 8px;
}

.meta-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 6px;
  font-weight: 600;
}

.meta-badge.free {
  background: #dcfce7;
  color: #16a34a;
}

.meta-badge.paid {
  background: #fef3c7;
  color: #d97706;
}

.meta-badge.status {
  background: #f1f5f9;
  color: #64748b;
}

/* 创作者 */
.creator-row {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 28px;
}

.creator-label {
  color: #94a3b8;
}

.creator-name {
  color: #1e293b;
  font-weight: 500;
}

/* Section */
.section {
  margin-bottom: 28px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 500;
}

.description {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
}

/* 标签 */
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 5px;
  background: #f1f5f9;
  color: #475569;
}

/* Prompt 框 */
.prompt-box {
  background: #1e293b;
  border-radius: 12px;
  padding: 24px;
  overflow-x: auto;
}

.prompt-text {
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
}

.change-note {
  font-size: 14px;
  color: #64748b;
}

/* 操作 */
.actions {
  display: flex;
  gap: 10px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

/* 侧栏 */
.detail-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.stats-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
}

.date-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.date-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 6px 0;
}

.date-row + .date-row {
  border-top: 1px solid #f1f5f9;
}

.date-label {
  color: #94a3b8;
}

.date-value {
  color: #475569;
}

@media (max-width: 768px) {
  .detail-layout {
    flex-direction: column;
  }
  .detail-sidebar {
    width: 100%;
  }
}
</style>

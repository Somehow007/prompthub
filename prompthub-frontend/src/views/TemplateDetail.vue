<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTemplateDetail, recordUse, type TemplateDetailVO } from '@/api/template'
import { purchaseTemplate } from '@/api/order'
import { getTemplateReviews, createReview, deleteReview, type ReviewVO } from '@/api/review'
import { toggleFavorite, checkFavorited } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import RatingStars from '@/components/RatingStars.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const template = ref<TemplateDetailVO | null>(null)
const loading = ref(true)

// 收藏
const favorited = ref(false)

// 评价
const reviews = ref<ReviewVO[]>([])
const reviewRating = ref(5)
const reviewContent = ref('')
const submittingReview = ref(false)

// 购买/使用
const purchasing = ref(false)

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

function isLoggedIn(): boolean {
  return userStore.isLoggedIn()
}

function goToEdit() {
  router.push(`/template/${template.value!.id}/edit`)
}

function goToHistory() {
  router.push(`/template/${template.value!.id}/history`)
}

function goToProfile(userId: number) {
  router.push(`/profile/${userId}`)
}

// ── 收藏 ──

async function loadFavoriteStatus() {
  if (!isLoggedIn() || !template.value) return
  try {
    favorited.value = await checkFavorited(template.value.id)
  } catch { /* 忽略 */ }
}

async function handleToggleFavorite() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    favorited.value = await toggleFavorite(template.value!.id)
    if (template.value) {
      template.value.favoriteCount += favorited.value ? 1 : -1
    }
    ElMessage.success(favorited.value ? '已收藏' : '已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}

// ── 购买/使用 ──

async function handlePurchase() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    return
  }
  if (!template.value) return

  if (template.value.price === 0) {
    // 免费模板：直接"获取"
    await doPurchase()
  } else {
    try {
      await ElMessageBox.confirm(
        `确认支付 ¥${template.value.price.toFixed(2)} 购买此模板？`,
        '确认购买',
        { confirmButtonText: '确认支付', cancelButtonText: '取消' }
      )
      await doPurchase()
    } catch { /* 取消 */ }
  }
}

async function doPurchase() {
  purchasing.value = true
  try {
    await purchaseTemplate(template.value!.id)
    ElMessage.success(template.value!.price === 0 ? '已获取模板' : '购买成功')
    // 刷新用户余额
    if (userStore.userInfo && template.value!.price > 0) {
      await userStore.fetchUserInfo()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    purchasing.value = false
  }
}

// ── 使用 ──

const showUseDialog = ref(false)
const useCopied = ref(false)

async function handleUseTemplate() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await recordUse(template.value!.id)
    if (template.value) {
      template.value.useCount++
    }
    showUseDialog.value = true
    useCopied.value = false
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleCopyPrompt() {
  if (!template.value) return
  try {
    await navigator.clipboard.writeText(template.value.promptContent)
    useCopied.value = true
    ElMessage.success('已复制到剪贴板')
  } catch {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = template.value.promptContent
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    useCopied.value = true
    ElMessage.success('已复制到剪贴板')
  }
}

// ── 删除评价 ──

async function handleDeleteReview(reviewId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条评价吗？', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteReview(reviewId)
    reviews.value = reviews.value.filter(r => r.id !== reviewId)
    if (template.value) {
      template.value.reviewCount = Math.max(0, template.value.reviewCount - 1)
    }
    ElMessage.success('评价已删除')
  } catch { /* 取消或失败 */ }
}

// ── 评价 ──

async function loadReviews() {
  if (!template.value) return
  try {
    reviews.value = await getTemplateReviews(template.value.id)
  } catch { /* 忽略 */ }
}

async function handleSubmitReview() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录')
    return
  }
  submittingReview.value = true
  try {
    const review = await createReview({
      templateId: template.value!.id,
      rating: reviewRating.value,
      content: reviewContent.value || undefined,
    })
    reviews.value.unshift(review)
    reviewContent.value = ''
    reviewRating.value = 5
    // 更新模板评分统计
    if (template.value) {
      template.value.reviewCount++
      // 粗略更新评分显示
    }
    ElMessage.success('评价成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '评价失败')
  } finally {
    submittingReview.value = false
  }
}

// ── 初始化 ──

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    template.value = await getTemplateDetail(id)
    await Promise.all([loadFavoriteStatus(), loadReviews()])
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
            <span class="creator-name clickable" @click="goToProfile(template.creatorId)">
              {{ template.creatorName }}
            </span>
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
          <div class="actions">
            <!-- 创作者操作 -->
            <template v-if="isOwner()">
              <el-button type="primary" @click="goToEdit">编辑模板</el-button>
              <el-button @click="goToHistory">版本历史</el-button>
            </template>

            <!-- 非创作者操作 -->
            <template v-else-if="isLoggedIn()">
              <el-button
                type="primary"
                :loading="purchasing"
                @click="handlePurchase"
              >
                {{ template.price === 0 ? '免费获取' : `购买 ¥${template.price.toFixed(2)}` }}
              </el-button>
              <el-button @click="handleUseTemplate">记录使用</el-button>
              <el-button
                :type="favorited ? 'warning' : 'default'"
                @click="handleToggleFavorite"
              >
                {{ favorited ? '★ 已收藏' : '☆ 收藏' }}
              </el-button>
            </template>

            <!-- 未登录 -->
            <template v-else>
              <router-link to="/login">
                <el-button type="primary">登录后操作</el-button>
              </router-link>
            </template>
          </div>

          <!-- ==================== 评价区域 ==================== -->
          <div class="reviews-section">
            <h2 class="section-title">
              评价 ({{ reviews.length }})
            </h2>

            <!-- 写评价 -->
            <div v-if="isLoggedIn() && !isOwner()" class="review-form">
              <div class="review-form-row">
                <span class="review-form-label">评分：</span>
                <RatingStars v-model="reviewRating" :size="22" />
              </div>
              <div class="review-form-row">
                <el-input
                  v-model="reviewContent"
                  type="textarea"
                  :rows="2"
                  placeholder="写下你的评价（可选）"
                  maxlength="500"
                  show-word-limit
                />
              </div>
              <el-button
                type="primary"
                size="small"
                :loading="submittingReview"
                @click="handleSubmitReview"
              >
                提交评价
              </el-button>
            </div>

            <!-- 评价列表 -->
            <div v-if="reviews.length > 0" class="review-list">
              <div v-for="r in reviews" :key="r.id" class="review-card">
                <div class="review-header">
                  <div class="review-user">
                    <span class="review-username">{{ r.username }}</span>
                    <RatingStars :model-value="r.rating" :size="14" readonly />
                  </div>
                  <div class="review-actions">
                    <span class="review-date">{{ formatDate(r.createdAt) }}</span>
                    <button
                      v-if="userStore.userInfo?.id === r.userId"
                      class="review-delete-btn"
                      @click="handleDeleteReview(r.id)"
                      title="删除评价"
                    >×</button>
                  </div>
                </div>
                <p v-if="r.content" class="review-content">{{ r.content }}</p>
              </div>
            </div>

            <div v-else class="review-empty">
              <p>暂无评价，快来第一个评价吧</p>
            </div>
          </div>
        </div>

        <!-- 右侧统计 -->
        <aside class="detail-sidebar">
          <div class="stats-card">
            <div class="stat-item">
              <span class="stat-value">{{ template.useCount.toLocaleString() }}</span>
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

    <!-- 使用模板弹窗 -->
    <el-dialog v-model="showUseDialog" title="使用 Prompt 模板" width="680px" :close-on-click-modal="false">
      <div class="use-dialog-body">
        <div class="use-dialog-meta">
          <span class="use-dialog-title-text">{{ template?.title }}</span>
          <span v-if="template" class="use-dialog-version">v{{ template.currentVersion }}</span>
        </div>
        <div class="use-dialog-prompt">
          <pre>{{ template?.promptContent }}</pre>
        </div>
        <p class="use-dialog-hint">将 Prompt 复制后粘贴到 AI 工具中使用，将 <code v-pre>{{变量名}}</code> 替换为实际内容</p>
      </div>
      <template #footer>
        <el-button @click="showUseDialog = false">关闭</el-button>
        <el-button type="primary" @click="handleCopyPrompt">
          {{ useCopied ? '✓ 已复制' : '复制 Prompt' }}
        </el-button>
      </template>
    </el-dialog>
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

.creator-name.clickable {
  cursor: pointer;
  border-bottom: 1px dashed #4f46e5;
}

.creator-name.clickable:hover {
  color: #4f46e5;
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

/* 评价区域 */
.reviews-section {
  margin-top: 40px;
  padding-top: 28px;
  border-top: 1px solid #e2e8f0;
}

.review-form {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.review-form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.review-form-label {
  font-size: 14px;
  color: #475569;
  white-space: nowrap;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-username {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.review-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-date {
  font-size: 12px;
  color: #94a3b8;
}

.review-delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.15s;
  line-height: 1;
}

.review-delete-btn:hover {
  background: #fee2e2;
  color: #dc2626;
}

.review-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
}

.review-empty {
  text-align: center;
  padding: 40px 0;
  color: #94a3b8;
  font-size: 14px;
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

/* 使用弹窗 */
.use-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.use-dialog-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.use-dialog-title-text {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.use-dialog-version {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 500;
}

.use-dialog-prompt {
  background: #1e293b;
  border-radius: 10px;
  padding: 20px;
  max-height: 350px;
  overflow-y: auto;
}

.use-dialog-prompt pre {
  color: #e2e8f0;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
}

.use-dialog-hint {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
  line-height: 1.6;
}

.use-dialog-hint code {
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
  color: #64748b;
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

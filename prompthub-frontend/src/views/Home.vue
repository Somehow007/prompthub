<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTemplates, searchTemplates, type TemplateVO, type TemplateQueryDTO } from '@/api/template'
import { getTags, type TagVO } from '@/api/tag'
import SearchBar from '@/components/SearchBar.vue'
import TagSelector from '@/components/TagSelector.vue'
import TemplateCard from '@/components/TemplateCard.vue'

const router = useRouter()
const userStore = useUserStore()

// 状态
const loading = ref(false)
const templates = ref<TemplateVO[]>([])
const tags = ref<TagVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 12

// 筛选参数
const query = reactive<TemplateQueryDTO>({
  page: 1,
  size: pageSize,
  sortBy: 'hot',
})

// 排序选项
const sortOptions = [
  { label: '综合热门', value: 'hot' },
  { label: '最新发布', value: 'newest' },
  { label: '评分最高', value: 'rating' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' },
]

// 加载标签
async function loadTags() {
  try {
    tags.value = await getTags()
  } catch {
    // 忽略
  }
}

// 加载模板
async function loadTemplates() {
  loading.value = true
  try {
    query.page = page.value
    const result = await getTemplates(query)
    templates.value = result.records
    total.value = result.total
  } catch {
    // 忽略
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch(keyword: string) {
  query.keyword = keyword || undefined
  page.value = 1
  loadTemplates()
}

// 标签筛选
function handleTagChange(selectedIds: number[]) {
  query.tagId = selectedIds.length > 0 ? selectedIds[selectedIds.length - 1] : undefined
  page.value = 1
  loadTemplates()
}

// 排序切换
function handleSortChange(sortBy: string) {
  query.sortBy = sortBy
  page.value = 1
  loadTemplates()
}

// 分页
function handlePageChange(p: number) {
  page.value = p
  loadTemplates()
}

// 跳转详情
function goToDetail(id: number) {
  router.push(`/template/${id}`)
}

// 跳转创建
function goToCreate() {
  router.push('/template/create')
}

// 登录/退出
function goToLogin() {
  router.push('/login')
}

function handleLogout() {
  userStore.logout()
  router.go(0)
}

onMounted(() => {
  loadTags()
  loadTemplates()
})
</script>

<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <router-link to="/" class="logo">
            <div class="logo-mark">P</div>
            <span class="logo-text">PromptHub</span>
          </router-link>
          <nav class="nav">
            <router-link to="/" class="nav-link active">发现</router-link>
          </nav>
        </div>
        <div class="header-right">
          <template v-if="userStore.userInfo">
            <el-button @click="goToCreate" type="primary" size="small">
              发布模板
            </el-button>
            <span class="user-name">{{ userStore.userInfo.username }}</span>
            <el-button plain size="small" @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="goToLogin">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="main">
      <!-- Hero 搜索区 -->
      <section class="hero-section">
        <h1 class="hero-title">发现高质量 AI Prompt 模板</h1>
        <p class="hero-sub">连接 AI 创造力与开发者智慧，找到适合你的提示词模板</p>
        <SearchBar @search="handleSearch" />
      </section>

      <!-- 内容区 -->
      <div class="content">
        <!-- 侧栏：标签筛选 -->
        <aside class="sidebar">
          <div class="sidebar-title">分类标签</div>
          <TagSelector
            :tags="tags"
            :model-value="query.tagId ? [query.tagId] : []"
            @update:model-value="handleTagChange"
          />

          <!-- 价格筛选（简单） -->
          <div class="sidebar-title" style="margin-top: 28px">价格区间</div>
          <div class="price-filters">
            <button
              :class="['price-chip', { active: query.minPrice === undefined && query.maxPrice === undefined }]"
              @click="query.minPrice = undefined; query.maxPrice = undefined; page = 1; loadTemplates()"
            >
              全部
            </button>
            <button
              :class="['price-chip', { active: query.minPrice === 0 && query.maxPrice === 0 }]"
              @click="query.minPrice = 0; query.maxPrice = 0; page = 1; loadTemplates()"
            >
              免费
            </button>
            <button
              :class="['price-chip', { active: query.minPrice === 0.01 && query.maxPrice === undefined }]"
              @click="query.minPrice = 0.01; query.maxPrice = undefined; page = 1; loadTemplates()"
            >
              付费
            </button>
          </div>
        </aside>

        <!-- 主内容：模板列表 -->
        <div class="main-content">
          <!-- 排序栏 -->
          <div class="toolbar">
            <span class="result-count">共 {{ total }} 个模板</span>
            <div class="sort-tabs">
              <button
                v-for="opt in sortOptions"
                :key="opt.value"
                :class="['sort-tab', { active: query.sortBy === opt.value }]"
                @click="handleSortChange(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- 模板网格 -->
          <div v-if="templates.length > 0" class="template-grid">
            <TemplateCard
              v-for="t in templates"
              :key="t.id"
              :template="t"
              @click="goToDetail"
            />
          </div>

          <div v-else-if="!loading" class="empty-state">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.5">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/>
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
            </svg>
            <p>暂无模板，快来发布第一个吧</p>
          </div>

          <!-- 分页 -->
          <div v-if="total > pageSize" class="pagination">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="page"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
}

/* ==================== 头部导航 ==================== */
.header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
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

.nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  font-size: 14px;
  padding: 6px 14px;
  border-radius: 6px;
  color: #64748b;
  text-decoration: none;
  transition: all 0.15s;
}

.nav-link:hover {
  color: #1e293b;
  background: #f1f5f9;
}

.nav-link.active {
  color: #4f46e5;
  background: #eef2ff;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 14px;
  color: #475569;
}

/* ==================== Hero ==================== */
.hero-section {
  text-align: center;
  padding: 48px 0 36px;
}

.hero-title {
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
  letter-spacing: -0.02em;
}

.hero-sub {
  font-size: 15px;
  color: #94a3b8;
  margin-bottom: 28px;
}

/* ==================== 内容区 ==================== */
.content {
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px 60px;
  display: flex;
  gap: 32px;
}

/* 侧栏 */
.sidebar {
  width: 200px;
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 12px;
}

.price-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.price-chip {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.price-chip:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.price-chip.active {
  background: #4f46e5;
  color: #fff;
  border-color: #4f46e5;
}

/* 主内容 */
.main-content {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.result-count {
  font-size: 13px;
  color: #94a3b8;
}

.sort-tabs {
  display: flex;
  gap: 2px;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 3px;
}

.sort-tab {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.sort-tab:hover {
  color: #1e293b;
}

.sort-tab.active {
  background: #fff;
  color: #0f172a;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

/* 模板网格 */
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
  margin-top: 16px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 36px;
}

/* 响应式 */
@media (max-width: 900px) {
  .content {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
  }
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .template-grid {
    grid-template-columns: 1fr;
  }
  .toolbar {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  .sort-tabs {
    overflow-x: auto;
    max-width: 100%;
  }
}
</style>

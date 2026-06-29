<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTemplates, type TemplateVO, type TemplateQueryDTO } from '@/api/template'
import { getActivities, createActivity, type ActivityVO } from '@/api/activity'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 权限检查
onMounted(() => {
  if (!userStore.isLoggedIn() || userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('无权限访问')
    router.replace('/')
    return
  }
  loadTemplates()
  loadExistingActivities()
})

// ── 模板列表 ──
const loading = ref(false)
const templates = ref<TemplateVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 15

const query = reactive<TemplateQueryDTO>({
  page: 1,
  size: 15,
  minPrice: 0.01, // 只显示付费模板
  sortBy: 'hot',
})

const sortOptions = [
  { label: '综合热度', value: 'hot' },
  { label: '使用最多', value: 'hot' },
  { label: '评分最高', value: 'rating' },
  { label: '最新发布', value: 'newest' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' },
]

async function loadTemplates() {
  loading.value = true
  try {
    query.page = page.value
    query.size = pageSize
    const result = await getTemplates(query)
    templates.value = result.records
    total.value = result.total
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function handleSearch(keyword: string) {
  query.keyword = keyword || undefined
  page.value = 1
  loadTemplates()
}

function handleSortChange(sortBy: string) {
  query.sortBy = sortBy
  page.value = 1
  loadTemplates()
}

function handlePageChange(p: number) {
  page.value = p
  loadTemplates()
}

// 热度分（与后端公式一致：useCount*0.5 + avgRating*10*0.3 + favoriteCount*0.2）
function hotScore(t: TemplateVO): number {
  return +(t.useCount * 0.5 + t.avgRating * 10 * 0.3 + t.favoriteCount * 0.2).toFixed(1)
}

// ── 已有活动 ──
const existingActivities = ref<ActivityVO[]>([])

async function loadExistingActivities() {
  try {
    existingActivities.value = await getActivities()
  } catch { /* ignore */ }
}

function hasActivity(templateId: number): boolean {
  return existingActivities.value.some(a => a.templateId === templateId && a.status !== 0)
}

// ── 创建活动 ──
const showCreateDialog = ref(false)
const selectedTemplate = ref<TemplateVO | null>(null)
const createForm = reactive({
  startTime: '',
  endTime: '',
  totalQuota: 100,
})
const creating = ref(false)

function openCreateDialog(t: TemplateVO) {
  selectedTemplate.value = t
  createForm.startTime = ''
  createForm.endTime = ''
  createForm.totalQuota = 100
  showCreateDialog.value = true
}

async function handleCreate() {
  if (!selectedTemplate.value) return
  if (!createForm.startTime || !createForm.endTime) {
    ElMessage.warning('请选择活动时间')
    return
  }
  if (new Date(createForm.endTime) <= new Date(createForm.startTime)) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  creating.value = true
  try {
    await createActivity({
      templateId: selectedTemplate.value.id,
      startTime: createForm.startTime,
      endTime: createForm.endTime,
      totalQuota: createForm.totalQuota,
    })
    ElMessage.success(`「${selectedTemplate.value.title}」活动创建成功`)
    showCreateDialog.value = false
    await loadExistingActivities()
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function goBack() {
  router.push('/activity')
}

// 价格格式化
function fmtPrice(p: number): string {
  return p === 0 ? '免费' : '¥' + p.toFixed(2)
}
</script>

<template>
  <div class="manage-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <button class="back-btn" @click="goBack">← 返回活动页</button>
          <span class="header-divider">/</span>
          <span class="header-title">活动管理</span>
        </div>
        <div class="header-right">
          <span class="admin-badge">管理员模式</span>
        </div>
      </div>
    </header>

    <main class="main">
      <!-- 统计概览 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">付费模板</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ existingActivities.filter(a => a.status === 1).length }}</div>
          <div class="stat-label">进行中活动</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ existingActivities.filter(a => a.status === 2).length }}</div>
          <div class="stat-label">未开始活动</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ existingActivities.filter(a => a.status === 0).length }}</div>
          <div class="stat-label">已结束活动</div>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索模板..."
            clearable
            style="width: 260px"
            :prefix-icon="'Search'"
            @input="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <span class="sort-label">排序：</span>
          <el-select
            :model-value="query.sortBy || 'hot'"
            style="width: 160px"
            @change="handleSortChange"
          >
            <el-option
              v-for="opt in sortOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
      </div>

      <!-- 模板表格 -->
      <div class="table-wrap">
        <el-table
          :data="templates"
          v-loading="loading"
          stripe
          style="width: 100%"
          :default-sort="{ prop: 'useCount', order: 'descending' }"
          row-class-name="template-row"
        >
          <el-table-column label="模板" min-width="280">
            <template #default="{ row }">
              <div class="cell-template">
                <span class="cell-title" @click="router.push(`/template/${row.id}`)">{{ row.title }}</span>
                <span class="cell-tags">{{ row.tagNames?.slice(0, 3).join(' · ') }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="创作者" width="120">
            <template #default="{ row }">{{ row.creatorName }}</template>
          </el-table-column>

          <el-table-column label="价格" width="90" sortable prop="price">
            <template #default="{ row }">
              <span :class="row.price === 0 ? 'price-free' : 'price-paid'">{{ fmtPrice(row.price) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="使用量" width="100" sortable prop="useCount">
            <template #default="{ row }">
              {{ row.useCount.toLocaleString() }}
            </template>
          </el-table-column>

          <el-table-column label="评分" width="80" sortable prop="avgRating">
            <template #default="{ row }">
              <span class="rating-cell">⭐ {{ row.avgRating }}</span>
            </template>
          </el-table-column>

          <el-table-column label="热度" width="80" sortable>
            <template #default="{ row }">
              <span class="hot-cell">{{ hotScore(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span v-if="hasActivity(row.id)" class="tag-exists">已有活动</span>
              <span v-else class="tag-none">无活动</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                type="primary"
                :disabled="hasActivity(row.id)"
                @click="openCreateDialog(row)"
              >
                {{ hasActivity(row.id) ? '已有活动' : '创建活动' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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
    </main>

    <!-- 创建活动对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建限时活动"
      width="520px"
      :close-on-click-modal="false"
      class="create-dialog"
    >
      <template v-if="selectedTemplate">
        <!-- 已选模板信息 -->
        <div class="selected-template-card">
          <div class="stc-info">
            <span class="stc-title">{{ selectedTemplate.title }}</span>
            <span class="stc-meta">
              {{ selectedTemplate.creatorName }}
              · ¥{{ selectedTemplate.price.toFixed(2) }}
              · ⭐{{ selectedTemplate.avgRating }}
              · {{ selectedTemplate.useCount }} 次使用
            </span>
          </div>
        </div>

        <el-form label-position="top" class="create-form" style="margin-top: 20px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="开始时间">
                <el-date-picker
                  v-model="createForm.startTime"
                  type="datetime"
                  placeholder="选择开始时间"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束时间">
                <el-date-picker
                  v-model="createForm.endTime"
                  type="datetime"
                  placeholder="选择结束时间"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="限量份数">
            <el-input-number v-model="createForm.totalQuota" :min="1" :max="10000" style="width: 100%" />
            <div class="form-hint">活动开始后用户可免费领取，领完即止</div>
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">
          创建活动
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.manage-page {
  min-height: 100vh;
  background: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
}

/* ==================== Header ==================== */
.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
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
  gap: 10px;
}

.back-btn {
  font-size: 13px;
  color: #4f46e5;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  font-family: inherit;
}

.back-btn:hover {
  background: #eef2ff;
}

.header-divider {
  color: #cbd5e1;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
}

.admin-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 20px;
  background: #fef3c7;
  color: #d97706;
}

/* ==================== Main ==================== */
.main {
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  padding: 28px 24px 60px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 13px;
  color: #94a3b8;
}

/* 表格 */
.table-wrap {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.cell-template {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.cell-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  cursor: pointer;
}

.cell-title:hover {
  color: #4f46e5;
}

.cell-tags {
  font-size: 11px;
  color: #94a3b8;
}

.price-paid {
  font-weight: 600;
  color: #f59e0b;
}

.rating-cell {
  font-weight: 500;
  color: #1e293b;
}

.hot-cell {
  font-weight: 600;
  color: #4f46e5;
}

.tag-exists {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #dbeafe;
  color: #1d4ed8;
  white-space: nowrap;
}

.tag-none {
  font-size: 11px;
  color: #cbd5e1;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ==================== 对话框 ==================== */
.create-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 16px;
}

.create-dialog :deep(.el-dialog__title) {
  font-size: 17px;
  font-weight: 600;
}

.selected-template-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
}

.stc-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stc-title {
  font-size: 16px;
  font-weight: 600;
  color: #166534;
}

.stc-meta {
  font-size: 13px;
  color: #64748b;
}

.form-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
}
</style>

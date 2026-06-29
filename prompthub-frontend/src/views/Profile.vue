<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserProfile, recharge, type UserProfileVO } from '@/api/user'
import { getFavorites } from '@/api/favorite'
import { getOrders, type OrderVO } from '@/api/order'
import type { TemplateVO } from '@/api/template'
import { ElMessage, ElMessageBox } from 'element-plus'
import TemplateCard from '@/components/TemplateCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref<UserProfileVO | null>(null)
const loading = ref(true)
const isSelf = ref(false)
const activeTab = ref('templates')

// 额外数据（仅本人可见）
const favorites = ref<TemplateVO[]>([])
const orders = ref<OrderVO[]>([])

function statusText(status: number): string {
  if (status === 1) return '已完成'
  if (status === 0) return '待支付'
  if (status === 2) return '已取消'
  return '已退款'
}

function formatPrice(price: number): string {
  return price > 0 ? `¥${price.toFixed(2)}` : '免费'
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) +
    ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function levelLabel(level: number): string {
  const labels = ['新手', '入门', '进阶', '资深', '专家', '大师']
  return labels[level] || '新手'
}

function levelColor(level: number): string {
  const colors = ['#94a3b8', '#22c55e', '#3b82f6', '#a855f7', '#f59e0b', '#ef4444']
  return colors[level] || colors[0]
}

async function loadProfile() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    profile.value = await getUserProfile(id)
    isSelf.value = userStore.userInfo?.id === id

    // 如果是本人，加载收藏和订单
    if (isSelf.value) {
      const [favs, ords] = await Promise.allSettled([getFavorites(), getOrders()])
      if (favs.status === 'fulfilled') favorites.value = favs.value
      if (ords.status === 'fulfilled') orders.value = ords.value
    }
  } catch {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

async function handleRecharge() {
  try {
    const { value } = await ElMessageBox.prompt('请输入充值金额', '充值', {
      confirmButtonText: '确认充值',
      cancelButtonText: '取消',
      inputType: 'number',
      inputValidator: (val: string) => {
        const n = Number(val)
        if (!n || n <= 0) return '请输入有效的充值金额'
        if (n > 99999.99) return '单次充值金额不能超过 99999.99 元'
        return true
      },
    })
    if (value) {
      const updatedUser = await recharge(Number(value))
      if (profile.value) {
        profile.value.balance = updatedUser.balance
      }
      if (userStore.userInfo) {
        userStore.userInfo.balance = updatedUser.balance
      }
      ElMessage.success(`充值成功，当前余额 ¥${updatedUser.balance.toFixed(2)}`)
    }
  } catch {
    // 用户取消
  }
}

function goToDetail(id: number) {
  router.push(`/template/${id}`)
}

function goToFavorites() {
  router.push('/favorites')
}

async function handleTabChange(tab: string) {
  activeTab.value = tab
  // 切换到收藏/订单 tab 时刷新数据
  if (tab === 'favorites' && isSelf.value) {
    try { favorites.value = await getFavorites() } catch { /* */ }
  }
  if (tab === 'orders' && isSelf.value) {
    try { orders.value = await getOrders() } catch { /* */ }
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="page">
    <!-- 头部导航 -->
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <div class="logo-mark">P</div>
          <span class="logo-text">PromptHub</span>
        </router-link>
        <router-link to="/" class="back-link">← 返回广场</router-link>
      </div>
    </header>

    <main v-if="!loading && profile" class="main">
      <!-- 用户信息卡片 -->
      <section class="profile-card">
        <div class="profile-left">
          <div class="avatar" :style="{ background: levelColor(profile.creatorLevel) }">
            {{ profile.username.charAt(0).toUpperCase() }}
          </div>
          <div class="profile-info">
            <h1 class="username">{{ profile.username }}</h1>
            <div class="meta-row">
              <span class="level-badge" :style="{ background: levelColor(profile.creatorLevel) + '18', color: levelColor(profile.creatorLevel), borderColor: levelColor(profile.creatorLevel) + '40' }">
                {{ levelLabel(profile.creatorLevel) }}创作者
              </span>
              <span class="meta-text">加入于 {{ formatDate(profile.createdAt) }}</span>
            </div>
            <div v-if="profile.email" class="meta-text email">{{ profile.email }}</div>
          </div>
        </div>
        <div class="profile-right">
          <div class="balance-display">
            <span class="balance-label">{{ isSelf ? '我的余额' : '余额' }}</span>
            <span class="balance-value">¥{{ profile.balance.toFixed(2) }}</span>
          </div>
          <el-button v-if="isSelf" type="primary" size="small" @click="handleRecharge">充值</el-button>
        </div>
      </section>

      <!-- 统计 -->
      <section class="stats-row">
        <div class="stat-item">
          <span class="stat-value">{{ profile.templateCount }}</span>
          <span class="stat-label">发布模板</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ profile.favoriteCount }}</span>
          <span class="stat-label">收藏</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ profile.totalUseCount.toLocaleString() }}</span>
          <span class="stat-label">总使用次数</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">¥{{ profile.totalIncome.toFixed(2) }}</span>
          <span class="stat-label">总收入</span>
        </div>
        <div v-if="isSelf" class="stat-item clickable" @click="activeTab = 'favorites'">
          <span class="stat-value action">查看</span>
          <span class="stat-label">我的收藏 →</span>
        </div>
      </section>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <button
          :class="['tab-btn', { active: activeTab === 'templates' }]"
          @click="handleTabChange('templates')"
        >
          发布的模板
        </button>
        <button
          v-if="isSelf"
          :class="['tab-btn', { active: activeTab === 'favorites' }]"
          @click="handleTabChange('favorites')"
        >
          我的收藏
        </button>
        <button
          v-if="isSelf"
          :class="['tab-btn', { active: activeTab === 'orders' }]"
          @click="handleTabChange('orders')"
        >
          订单记录
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'income' }]"
          @click="handleTabChange('income')"
        >
          收入明细
        </button>
      </div>

      <!-- Tab: 发布的模板 -->
      <div v-if="activeTab === 'templates'" class="tab-content">
        <div v-if="profile.recentTemplates.length > 0" class="template-grid">
          <TemplateCard
            v-for="t in profile.recentTemplates"
            :key="t.id"
            :template="t"
            @click="goToDetail"
          />
        </div>
        <div v-else class="empty-tab">
          <p>还没有发布任何模板</p>
          <router-link v-if="isSelf" to="/template/create" class="create-link">去发布第一个</router-link>
        </div>
      </div>

      <!-- Tab: 我的收藏（仅本人） -->
      <div v-if="activeTab === 'favorites' && isSelf" class="tab-content">
        <div v-if="favorites.length > 0" class="template-grid">
          <TemplateCard
            v-for="t in favorites"
            :key="t.id"
            :template="t"
            @click="goToDetail"
          />
        </div>
        <div v-else class="empty-tab">
          <p>还没有收藏任何模板</p>
          <router-link to="/" class="create-link">去广场逛逛</router-link>
        </div>
      </div>

      <!-- Tab: 订单记录（仅本人） -->
      <div v-if="activeTab === 'orders' && isSelf" class="tab-content">
        <div v-if="orders.length > 0" class="data-table">
          <div class="table-header">
            <span class="col title">模板</span>
            <span class="col amount">金额</span>
            <span class="col status">状态</span>
            <span class="col time">时间</span>
          </div>
          <div v-for="o in orders" :key="o.id" class="table-row">
            <span class="col title order-title" @click="goToDetail(o.templateId)">
              {{ o.templateTitle || '已删除模板' }}
            </span>
            <span class="col amount" :class="{ free: o.amount === 0 }">
              {{ o.amount === 0 ? '免费' : '¥' + o.amount.toFixed(2) }}
            </span>
            <span class="col status">
              <span :class="['status-badge', o.status === 1 ? 'done' : o.status === 2 ? 'cancel' : '']">
                {{ statusText(o.status) }}
              </span>
            </span>
            <span class="col time">{{ formatDateTime(o.createdAt) }}</span>
          </div>
        </div>
        <div v-else class="empty-tab">
          <p>还没有任何订单</p>
        </div>
      </div>

      <!-- Tab: 收入明细 -->
      <div v-if="activeTab === 'income'" class="tab-content">
        <div v-if="profile.incomeRecords.length > 0" class="data-table">
          <div class="table-header">
            <span class="col title">模板</span>
            <span class="col type">类型</span>
            <span class="col amount">金额</span>
            <span class="col time">时间</span>
          </div>
          <div v-for="item in profile.incomeRecords" :key="item.id" class="table-row">
            <span class="col title">{{ item.templateTitle || '已删除模板' }}</span>
            <span class="col type">
              <span :class="['type-badge', item.type === 'sale' ? 'sale' : 'refund']">
                {{ item.type === 'sale' ? '销售' : '退款' }}
              </span>
            </span>
            <span class="col amount income">+¥{{ item.amount.toFixed(2) }}</span>
            <span class="col time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
        </div>
        <div v-else class="empty-tab">
          <p>还没有收入记录</p>
        </div>
      </div>
    </main>

    <main v-else-if="!loading" class="main empty">
      <p>用户不存在</p>
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

.empty {
  text-align: center;
  color: #94a3b8;
  padding-top: 80px;
}

/* 用户信息卡片 */
.profile-card {
  background: #fff;
  border-radius: 14px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.profile-left {
  display: flex;
  gap: 20px;
  align-items: center;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.username {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.02em;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.level-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 6px;
  font-weight: 600;
  border: 1px solid;
}

.meta-text {
  font-size: 13px;
  color: #94a3b8;
}

.email {
  font-size: 14px;
  color: #64748b;
}

.profile-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.balance-display {
  text-align: right;
}

.balance-label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.balance-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
}

/* 统计 */
.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 28px;
}

.stat-item {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.stat-item.clickable {
  cursor: pointer;
  transition: box-shadow 0.15s;
}

.stat-item.clickable:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 0 0 1px #4f46e5;
}

.stat-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.stat-value.action {
  color: #4f46e5;
  font-size: 16px;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
}

/* Tab 切换 */
.tab-bar {
  display: flex;
  gap: 2px;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 24px;
  width: fit-content;
}

.tab-btn {
  font-size: 14px;
  padding: 8px 20px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  white-space: nowrap;
}

.tab-btn:hover {
  color: #1e293b;
}

.tab-btn.active {
  background: #fff;
  color: #0f172a;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

/* Tab 内容 */
.tab-content {
  min-height: 160px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.empty-tab {
  text-align: center;
  padding: 60px 0;
  color: #94a3b8;
  font-size: 14px;
}

.empty-tab p {
  margin-bottom: 12px;
}

.create-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
}

.create-link:hover {
  text-decoration: underline;
}

/* 数据表格 */
.data-table {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
  overflow: hidden;
}

.table-header {
  display: flex;
  padding: 14px 20px;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.table-row {
  display: flex;
  padding: 14px 20px;
  font-size: 14px;
  color: #475569;
  border-bottom: 1px solid #f8fafc;
  align-items: center;
}

.table-row:last-child {
  border-bottom: none;
}

.col.title { flex: 2; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col.type { flex: 1; }
.col.amount { flex: 1; text-align: right; font-weight: 500; }
.col.status { flex: 1; text-align: right; }
.col.time { flex: 1.5; text-align: right; font-size: 13px; color: #94a3b8; }

.col.amount.free { color: #16a34a; }
.col.amount.income { color: #16a34a; }

.order-title {
  cursor: pointer;
  color: #4f46e5;
}

.order-title:hover {
  text-decoration: underline;
}

.status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.status-badge.done {
  background: #dcfce7;
  color: #16a34a;
}

.status-badge.cancel {
  background: #f1f5f9;
  color: #94a3b8;
}

.type-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.type-badge.sale {
  background: #dcfce7;
  color: #16a34a;
}

.type-badge.refund {
  background: #fee2e2;
  color: #dc2626;
}

@media (max-width: 768px) {
  .profile-card {
    flex-direction: column;
    gap: 20px;
  }
  .profile-right {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }
  .stats-row {
    flex-wrap: wrap;
  }
  .stat-item {
    flex: 0 0 calc(50% - 8px);
  }
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .tab-bar {
    overflow-x: auto;
    max-width: 100%;
  }
}

@media (max-width: 480px) {
  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getActivities, claimActivity, type ActivityVO } from '@/api/activity'
import { getPurchasedIds } from '@/api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activities = ref<ActivityVO[]>([])
const claimingId = ref<number | null>(null)
const purchasedIds = ref<Set<number>>(new Set())

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  loading.value = true
  try {
    activities.value = await getActivities()
  } catch { /* ignore */ }
  finally { loading.value = false }

  if (userStore.isLoggedIn()) {
    try {
      const ids = await getPurchasedIds()
      purchasedIds.value = new Set(ids)
    } catch { /* ignore */ }
  }

  timer = setInterval(() => { now.value = Date.now() }, 1000)
})

onUnmounted(() => { if (timer) clearInterval(timer) })

function ownsTemplate(templateId: number) {
  return purchasedIds.value.has(templateId)
}

// ── 辅助 ──

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

function fmtTime(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function countdown(end: string, start: string, status: number): { text: string, urgent: boolean } {
  const e = new Date(end).getTime()
  const s = new Date(start).getTime()
  const n = now.value
  if (status === 2 || n < s) {
    const diff = s - n
    if (diff <= 0) return { text: '即将开始', urgent: false }
    const h = Math.floor(diff / 3600000), m = Math.floor((diff % 3600000) / 60000)
    if (h > 24) return { text: `${Math.floor(h / 24)}天后开始`, urgent: false }
    return { text: `${pad(h)}:${pad(m)}:${pad((diff % 60000) / 1000 | 0)} 后开始`, urgent: false }
  }
  if (status === 1 && n < e) {
    const diff = e - n
    if (diff <= 0) return { text: '即将结束', urgent: true }
    const h = Math.floor(diff / 3600000), m = Math.floor((diff % 3600000) / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    if (diff < 3600000) return { text: `${pad(m)}:${pad(s)}`, urgent: true }
    if (h > 24) return { text: `剩余 ${Math.floor(h / 24)} 天 ${h % 24} 时`, urgent: false }
    return { text: `剩余 ${pad(h)}:${pad(m)}:${pad(s)}`, urgent: h < 2 }
  }
  return { text: '已结束', urgent: false }
}

function pad(n: number): string { return n < 10 ? '0' + n : String(n) }

// ── 分类 ──

const activeActivities = computed(() =>
  activities.value.filter(a => {
    const n = now.value
    return a.status === 1 && new Date(a.startTime).getTime() <= n && new Date(a.endTime).getTime() > n
  })
)
const upcomingActivities = computed(() =>
  activities.value.filter(a =>
    a.status === 2 || (a.status === 1 && new Date(a.startTime).getTime() > now.value)
  )
)
const endedActivities = computed(() =>
  activities.value.filter(a =>
    a.status === 0 || (a.status === 1 && new Date(a.endTime).getTime() <= now.value)
  )
)

// ── 领取 ──

async function handleClaim(activity: ActivityVO) {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  claimingId.value = activity.id
  try {
    await claimActivity(activity.id)
    purchasedIds.value.add(activity.templateId)
    activity.remainingQuota--
    ElMessage.success('领取成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '领取失败')
  } finally {
    claimingId.value = null
  }
}
</script>

<template>
  <div class="pg">
    <!-- Nav -->
    <header class="nav-bar">
      <div class="nav-inner">
        <div class="nav-left">
          <router-link to="/" class="logo">
            <span class="logo-mark">P</span>
            <span class="logo-word">PromptHub</span>
          </router-link>
          <div class="nav-links">
            <router-link to="/" class="nl">发现</router-link>
            <router-link to="/activity" class="nl on">限时活动</router-link>
          </div>
        </div>
        <div class="nav-right">
          <template v-if="userStore.userInfo">
            <router-link v-if="isAdmin" to="/admin/activities" class="admin-entrance">活动管理</router-link>
            <router-link :to="`/profile/${userStore.userInfo.id}`" class="nav-user">{{ userStore.userInfo.username }}</router-link>
          </template>
          <router-link v-else to="/login" class="nav-login">登录</router-link>
        </div>
      </div>
    </header>

    <!-- Content -->
    <div class="body">
      <!-- Page title -->
      <div class="pg-head">
        <h1 class="pg-title">限时活动</h1>
        <p class="pg-desc">优质付费模板限时免费开放，把握机会领取你需要的 Prompt</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="empty">加载中...</div>

      <!-- Empty -->
      <div v-else-if="activities.length === 0" class="empty">暂无活动</div>

      <template v-else>
        <!-- 进行中 -->
        <section v-if="activeActivities.length > 0" class="sec">
          <div class="sec-hd">
            <span class="sec-dot live"></span>
            <h2 class="sec-tt">进行中</h2>
            <span class="sec-n">{{ activeActivities.length }}</span>
          </div>
          <div class="grid">
            <article v-for="a in activeActivities" :key="a.id" class="card card-live">
              <div class="card-top">
                <span class="card-tt" @click="router.push(`/template/${a.templateId}`)">{{ a.templateTitle }}</span>
                <span class="card-by">{{ a.creatorName }}</span>
              </div>
              <div class="card-mid">
                <div class="price-block">
                  <span class="price-old">¥{{ a.templatePrice.toFixed(2) }}</span>
                  <span class="price-free">免费</span>
                </div>
              </div>
              <div class="card-bot">
                <div class="quota-line">
                  <div class="quota-fill" :style="{ width: (a.totalQuota > 0 ? (a.remainingQuota / a.totalQuota * 100) : 0) + '%' }"></div>
                </div>
                <div class="card-meta">
                  <span class="m-quota">{{ a.totalQuota - a.remainingQuota }}/{{ a.totalQuota }} 已领</span>
                  <span class="m-time" :class="{ urgent: countdown(a.endTime, a.startTime, a.status).urgent }">
                    {{ countdown(a.endTime, a.startTime, a.status).text }}
                  </span>
                </div>
              </div>
              <button
                v-if="ownsTemplate(a.templateId)"
                class="btn owned"
                disabled
              >已拥有</button>
              <button
                v-else
                class="btn claim"
                :disabled="a.remainingQuota <= 0 || claimingId === a.id"
                @click="handleClaim(a)"
              >
                <template v-if="claimingId === a.id">领取中...</template>
                <template v-else-if="a.remainingQuota <= 0">已抢光</template>
                <template v-else>免费领取</template>
              </button>
            </article>
          </div>
        </section>

        <!-- 即将开始 -->
        <section v-if="upcomingActivities.length > 0" class="sec">
          <div class="sec-hd">
            <span class="sec-dot soon"></span>
            <h2 class="sec-tt">即将开始</h2>
            <span class="sec-n dim">{{ upcomingActivities.length }}</span>
          </div>
          <div class="grid">
            <article v-for="a in upcomingActivities" :key="a.id" class="card card-soon">
              <div class="card-top">
                <span class="card-tt" @click="router.push(`/template/${a.templateId}`)">{{ a.templateTitle }}</span>
                <span class="card-by">{{ a.creatorName }}</span>
              </div>
              <div class="card-mid">
                <div class="price-block">
                  <span class="price-old">¥{{ a.templatePrice.toFixed(2) }}</span>
                  <span class="price-free">免费</span>
                </div>
              </div>
              <div class="card-bot">
                <div class="card-meta">
                  <span class="m-quota">限量 {{ a.totalQuota }} 份</span>
                  <span class="m-time">{{ countdown(a.endTime, a.startTime, a.status).text }}</span>
                </div>
              </div>
              <button class="btn soon" disabled>尚未开始</button>
            </article>
          </div>
        </section>

        <!-- 已结束 -->
        <section v-if="endedActivities.length > 0" class="sec">
          <div class="sec-hd">
            <span class="sec-dot end"></span>
            <h2 class="sec-tt">已结束</h2>
            <span class="sec-n dim">{{ endedActivities.length }}</span>
          </div>
          <div class="grid">
            <article v-for="a in endedActivities" :key="a.id" class="card card-end">
              <div class="card-top">
                <span class="card-tt" @click="router.push(`/template/${a.templateId}`)">{{ a.templateTitle }}</span>
                <span class="card-by">{{ a.creatorName }}</span>
              </div>
              <div class="card-mid">
                <span class="price-norm">¥{{ a.templatePrice.toFixed(2) }}</span>
              </div>
              <div class="card-bot">
                <span class="m-quota">{{ a.totalQuota }} 份已领完</span>
              </div>
              <button class="btn ended" disabled>已结束</button>
            </article>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
/* ====== Reset & Base ====== */
.pg {
  min-height: 100vh;
  background: #fafbfc;
  color: #1a1a2e;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ====== Nav ====== */
.nav-bar {
  height: 52px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  position: sticky;
  top: 0;
  z-index: 50;
}
.nav-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 28px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-left { display: flex; align-items: center; gap: 28px; }
.logo { display: flex; align-items: center; gap: 7px; text-decoration: none; }
.logo-mark {
  width: 26px; height: 26px; border-radius: 6px;
  background: #2d2d3f; color: #fff;
  font-size: 13px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.logo-word { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.nav-links { display: flex; gap: 2px; }
.nl {
  font-size: 13px; padding: 5px 12px; border-radius: 5px;
  color: #6b7280; text-decoration: none; transition: all .12s;
}
.nl:hover { color: #1a1a2e; background: #f3f4f6; }
.nl.on { color: #2d2d3f; background: #f0f0f5; font-weight: 500; }
.nav-right { display: flex; align-items: center; gap: 14px; }
.nav-user { font-size: 13px; color: #4b5563; text-decoration: none; }
.nav-user:hover { color: #1a1a2e; }
.nav-login {
  font-size: 13px; padding: 5px 14px; border-radius: 6px;
  background: #2d2d3f; color: #fff; text-decoration: none; font-weight: 500;
}
.admin-entrance {
  font-size: 12px; padding: 4px 12px; border-radius: 5px;
  background: #fef3c7; color: #92400e; text-decoration: none; font-weight: 500;
}
.admin-entrance:hover { background: #fde68a; }

/* ====== Body ====== */
.body { max-width: 1100px; margin: 0 auto; padding: 40px 28px 80px; }
.pg-head { margin-bottom: 36px; }
.pg-title { font-size: 26px; font-weight: 700; margin: 0 0 6px; letter-spacing: -0.02em; }
.pg-desc { font-size: 14px; color: #9ca3af; margin: 0; }
.empty { text-align: center; padding: 80px 0; color: #9ca3af; font-size: 14px; }

/* ====== Section ====== */
.sec { margin-bottom: 44px; }
.sec-hd { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
.sec-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.sec-dot.live { background: #22c55e; box-shadow: 0 0 0 4px rgba(34,197,94,.15); }
.sec-dot.soon { background: #f59e0b; box-shadow: 0 0 0 4px rgba(245,158,11,.15); }
.sec-dot.end { background: #cbd5e1; }
.sec-tt { font-size: 17px; font-weight: 600; margin: 0; }
.sec-n { font-size: 12px; padding: 1px 7px; border-radius: 3px; background: #eef2ff; color: #4f46e5; font-weight: 600; }
.sec-n.dim { background: #f3f4f6; color: #9ca3af; }

/* ====== Grid ====== */
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }

/* ====== Card ====== */
.card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebedf0;
  padding: 20px;
  display: flex; flex-direction: column;
  transition: box-shadow .18s, transform .18s;
}
.card-live { border-color: #d1fae5; }
.card-live:hover { box-shadow: 0 4px 20px rgba(0,0,0,.06); }
.card-soon { background: #fafbfc; }
.card-end { background: #fafafa; opacity: .65; }

.card-top { margin-bottom: 14px; }
.card-tt {
  display: block; font-size: 15px; font-weight: 600; color: #1a1a2e;
  cursor: pointer; line-height: 1.4; margin-bottom: 4px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.card-tt:hover { color: #4f46e5; }
.card-by { font-size: 12px; color: #9ca3af; }

.card-mid { margin-bottom: 14px; }
.price-block { display: flex; align-items: baseline; gap: 8px; }
.price-old { font-size: 15px; color: #9ca3af; text-decoration: line-through; }
.price-free { font-size: 20px; font-weight: 700; color: #16a34a; }
.price-norm { font-size: 15px; color: #6b7280; font-weight: 500; }

.card-bot { margin-bottom: 16px; flex: 1; }

.quota-line {
  height: 4px; background: #f3f4f6; border-radius: 2px; margin-bottom: 10px; overflow: hidden;
}
.quota-fill {
  height: 100%; background: #4f46e5; border-radius: 2px; transition: width .4s;
}

.card-meta { display: flex; justify-content: space-between; align-items: center; }
.m-quota { font-size: 11px; color: #9ca3af; }
.m-time { font-size: 12px; color: #6b7280; font-weight: 500; }
.m-time.urgent { color: #ef4444; }

/* ====== Buttons ====== */
.btn {
  width: 100%; padding: 10px 0; border: none; border-radius: 7px;
  font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit;
  transition: all .15s;
}
.btn.claim { background: #2d2d3f; color: #fff; }
.btn.claim:hover:not(:disabled) { background: #1a1a2e; }
.btn.claim:disabled { background: #e5e7eb; color: #9ca3af; cursor: not-allowed; }
.btn.owned { background: #ecfdf5; color: #065f46; cursor: default; }
.btn.soon { background: #f3f4f6; color: #9ca3af; cursor: not-allowed; }
.btn.ended { background: #f3f4f6; color: #cbd5e1; cursor: not-allowed; }

/* ====== Responsive ====== */
@media (max-width: 768px) {
  .grid { grid-template-columns: 1fr; }
  .pg-title { font-size: 22px; }
}
</style>

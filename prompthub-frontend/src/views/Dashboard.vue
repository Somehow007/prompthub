<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPlatformOverview, getHotRanking, type PlatformOverviewVO, type HotTemplateVO } from '@/api/statistics'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const overview = ref<PlatformOverviewVO | null>(null)
const ranking = ref<HotTemplateVO[]>([])
const loading = ref(true)

async function loadData() {
  try {
    const [ov, rank] = await Promise.all([
      getPlatformOverview(),
      getHotRanking(20),
    ])
    overview.value = ov
    ranking.value = rank
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const newUserChartOption = computed(() => {
  if (!overview.value) return {}
  const data = overview.value.newUserTrend
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date.slice(5)), // MM-DD
      axisLabel: { fontSize: 11, color: '#94a3b8' },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { fontSize: 11, color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
    },
    series: [{
      type: 'line',
      data: data.map(d => d.count),
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { color: '#4f46e5', width: 2 },
      itemStyle: { color: '#4f46e5' },
      areaStyle: { color: 'rgba(79, 70, 229, 0.06)' },
    }],
  }
})

function formatPrice(price: number): string {
  return price > 0 ? `¥${price.toFixed(2)}` : '免费'
}

onMounted(() => { loadData() })
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

    <main v-if="!loading && overview" class="main">
      <h1 class="page-title">📊 平台数据总览</h1>

      <!-- 指标卡片 -->
      <section class="cards-row">
        <div class="metric-card">
          <span class="metric-value">{{ overview.totalUsers.toLocaleString() }}</span>
          <span class="metric-label">总用户数</span>
        </div>
        <div class="metric-card">
          <span class="metric-value">{{ overview.totalTemplates.toLocaleString() }}</span>
          <span class="metric-label">总模板数</span>
        </div>
        <div class="metric-card">
          <span class="metric-value">{{ overview.totalOrders.toLocaleString() }}</span>
          <span class="metric-label">总订单数</span>
        </div>
        <div class="metric-card">
          <span class="metric-value">¥{{ overview.totalTransactionAmount.toFixed(2) }}</span>
          <span class="metric-label">总交易额</span>
        </div>
      </section>

      <!-- 图表 + 排行榜 -->
      <div class="split-row">
        <!-- 新增用户趋势 -->
        <section class="panel chart-panel">
          <h2 class="panel-title">近30天新增用户趋势</h2>
          <VChart
            v-if="overview.newUserTrend.length > 0"
            :option="newUserChartOption"
            style="height: 280px;"
          />
          <p v-else class="no-data">暂无数据</p>
        </section>

        <!-- 热门排行 -->
        <section class="panel ranking-panel">
          <h2 class="panel-title">🔥 热门模板 Top 20</h2>
          <div v-if="ranking.length > 0" class="ranking-list">
            <div
              v-for="(item, idx) in ranking"
              :key="item.templateId"
              class="ranking-item"
            >
              <span :class="['rank-num', { gold: idx === 0, silver: idx === 1, bronze: idx === 2 }]">
                {{ idx + 1 }}
              </span>
              <div class="rank-info">
                <router-link :to="`/template/${item.templateId}`" class="rank-title">
                  {{ item.title }}
                </router-link>
                <span class="rank-creator">{{ item.creatorName }}</span>
              </div>
              <div class="rank-stats">
                <span class="rank-score">{{ item.hotScore.toFixed(1) }} 热度</span>
                <span class="rank-price">{{ formatPrice(item.price) }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>

    <main v-else-if="loading" class="main loading-state">
      <p>加载中...</p>
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
  width: 1200px;
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
  width: 1200px;
  max-width: 100%;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.loading-state {
  text-align: center;
  color: #94a3b8;
  padding-top: 80px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 28px;
}

/* 指标卡片 */
.cards-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.metric-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.metric-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.metric-label {
  font-size: 13px;
  color: #94a3b8;
}

/* 分栏 */
.split-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.panel {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
}

.no-data {
  text-align: center;
  color: #94a3b8;
  padding: 40px 0;
  font-size: 14px;
}

/* 排行榜 */
.ranking-list {
  max-height: 540px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-num {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  flex-shrink: 0;
}

.rank-num.gold { background: #fef3c7; color: #d97706; }
.rank-num.silver { background: #f1f5f9; color: #64748b; }
.rank-num.bronze { background: #fed7aa; color: #c2410c; }

.rank-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rank-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-title:hover {
  color: #4f46e5;
}

.rank-creator {
  font-size: 12px;
  color: #94a3b8;
}

.rank-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.rank-score {
  font-size: 13px;
  font-weight: 600;
  color: #4f46e5;
}

.rank-price {
  font-size: 11px;
  color: #94a3b8;
}

@media (max-width: 768px) {
  .cards-row { grid-template-columns: repeat(2, 1fr); }
  .split-row { grid-template-columns: 1fr; }
}
</style>

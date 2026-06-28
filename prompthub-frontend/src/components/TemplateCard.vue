<script setup lang="ts">
import type { TemplateVO } from '@/api/template'

const props = defineProps<{
  template: TemplateVO
}>()

const emit = defineEmits<{
  click: [id: number]
}>()

function formatPrice(price: number): string {
  if (price === 0) return '免费'
  return `¥${price.toFixed(2)}`
}

function formatRating(rating: number): string {
  return rating > 0 ? rating.toFixed(1) : '暂无'
}
</script>

<template>
  <div class="card" @click="emit('click', template.id)">
    <!-- 封面区 -->
    <div class="card-cover">
      <div class="card-cover-placeholder">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5">
          <path d="M9.5 14.5 3 21" stroke-linecap="round"/>
          <path d="M5.5 8.5 10 14l2-3 4.5 6H3l2.5-8.5Z"/>
          <circle cx="16.5" cy="5.5" r="1.5"/>
        </svg>
      </div>
      <span v-if="template.price === 0" class="card-badge free">免费</span>
      <span v-else class="card-badge paid">¥{{ template.price.toFixed(2) }}</span>
    </div>

    <!-- 信息区 -->
    <div class="card-body">
      <h3 class="card-title">{{ template.title }}</h3>
      <p v-if="template.description" class="card-desc">{{ template.description }}</p>

      <!-- 标签 -->
      <div v-if="template.tagNames?.length" class="card-tags">
        <span v-for="name in template.tagNames.slice(0, 3)" :key="name" class="card-tag">{{ name }}</span>
        <span v-if="template.tagNames.length > 3" class="card-tag-more">+{{ template.tagNames.length - 3 }}</span>
      </div>

      <!-- 统计 + 创作者 -->
      <div class="card-footer">
        <div class="card-stats">
          <span class="stat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20V10m0 0-4 4m4-4 4 4"/><path d="M5 17v2m14-2v2"/></svg>
            {{ template.useCount }}
          </span>
          <span class="stat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 0 0 .95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 0 0-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 0 0-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 0 0-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 0 0 .951-.69l1.519-4.674Z"/></svg>
            {{ formatRating(template.avgRating) }}
          </span>
        </div>
        <div class="card-creator">
          <span class="creator-name">{{ template.creatorName }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08), 0 0 0 1px #cbd5e1;
}

/* 封面 */
.card-cover {
  position: relative;
  height: 120px;
  background: linear-gradient(135deg, #f0f4ff 0%, #f8f9ff 50%, #eef2ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-cover-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.card-badge.free {
  background: #dcfce7;
  color: #16a34a;
}

.card-badge.paid {
  background: #fef3c7;
  color: #d97706;
}

/* 内容 */
.card-body {
  padding: 14px 16px 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.4;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 标签 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.card-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
}

.card-tag-more {
  font-size: 11px;
  color: #94a3b8;
}

/* 底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #94a3b8;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.creator-name {
  font-size: 12px;
  color: #64748b;
}
</style>

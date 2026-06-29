import request from './request'
import type { TemplateVO } from './template'

// ── API ──

/** 切换收藏状态 */
export function toggleFavorite(templateId: number): Promise<boolean> {
  return request.post(`/favorites/${templateId}`)
}

/** 检查是否已收藏 */
export function checkFavorited(templateId: number): Promise<boolean> {
  return request.get(`/favorites/check/${templateId}`)
}

/** 我的收藏列表 */
export function getFavorites(): Promise<TemplateVO[]> {
  return request.get('/favorites')
}

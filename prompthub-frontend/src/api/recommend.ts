import request from './request'
import type { TemplateVO } from './template'

/** 获取个性化推荐 */
export function getRecommend(limit = 10): Promise<TemplateVO[]> {
  return request.get('/recommend', { params: { limit } })
}

import request from './request'

// ── DTO ──

export interface CreateReviewDTO {
  templateId: number
  rating: number
  content?: string
}

// ── VO ──

export interface ReviewVO {
  id: number
  userId: number
  username: string
  avatarUrl: string | null
  templateId: number
  rating: number
  content: string
  createdAt: string
}

// ── API ──

/** 创建评价 */
export function createReview(data: CreateReviewDTO): Promise<ReviewVO> {
  return request.post('/reviews', data)
}

/** 模板评价列表 */
export function getTemplateReviews(templateId: number): Promise<ReviewVO[]> {
  return request.get(`/templates/${templateId}/reviews`)
}

/** 删除自己的评价 */
export function deleteReview(reviewId: number): Promise<void> {
  return request.delete(`/reviews/${reviewId}`)
}

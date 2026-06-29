import request from './request'

// ── Types ──

export interface ActivityVO {
  id: number
  templateId: number
  templateTitle: string
  creatorName: string
  templatePrice: number
  avgRating: number
  startTime: string
  endTime: string
  totalQuota: number
  remainingQuota: number
  status: number // 1-进行中 0-已结束 2-未开始
  createdAt: string
}

// ── API ──

/** 获取活动列表 */
export function getActivities(): Promise<ActivityVO[]> {
  return request.get('/activities')
}

/** 领取活动模板 */
export function claimActivity(id: number): Promise<string> {
  return request.post(`/activities/${id}/claim`)
}

/** 创建活动（管理员） */
export interface CreateActivityDTO {
  templateId: number
  startTime: string
  endTime: string
  totalQuota: number
}

export function createActivity(dto: CreateActivityDTO): Promise<ActivityVO> {
  return request.post('/admin/activities', dto)
}

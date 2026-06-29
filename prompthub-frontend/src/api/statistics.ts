import request from './request'

// ── VO ──

export interface HotTemplateVO {
  templateId: number
  title: string
  creatorName: string
  price: number
  useCount: number
  avgRating: number
  favoriteCount: number
  hotScore: number
}

export interface UsageTrendVO {
  date: string
  count: number
}

export interface IncomeTrendVO {
  month: string
  amount: number
}

export interface CreatorDashboardVO {
  totalTemplates: number
  totalUseCount: number
  totalIncome: number
  avgRating: number
  usageTrend: UsageTrendVO[]
  incomeTrend: IncomeTrendVO[]
}

export interface DailyNewUsersVO {
  date: string
  count: number
}

export interface PlatformOverviewVO {
  totalUsers: number
  totalTemplates: number
  totalTransactionAmount: number
  totalOrders: number
  newUserTrend: DailyNewUsersVO[]
}

// ── API ──

/** 热门排行 */
export function getHotRanking(limit: number = 20): Promise<HotTemplateVO[]> {
  return request.get('/statistics/ranking', { params: { limit } })
}

/** 模板使用趋势（近30天） */
export function getTemplateTrend(templateId: number): Promise<UsageTrendVO[]> {
  return request.get(`/statistics/trend/${templateId}`)
}

/** 创作者数据看板 */
export function getCreatorDashboard(): Promise<CreatorDashboardVO> {
  return request.get('/statistics/creator')
}

/** 平台总览 */
export function getPlatformOverview(): Promise<PlatformOverviewVO> {
  return request.get('/statistics/platform')
}

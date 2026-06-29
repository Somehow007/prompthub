import request from './request'
import type { TemplateVO } from './template'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  email?: string
  phone?: string
}

export interface UserInfo {
  id: number
  username: string
  email: string
  phone: string
  avatarUrl: string
  creatorLevel: number
  balance: number
  role: string
  createdAt: string
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export interface IncomeRecordVO {
  id: number
  templateId: number
  templateTitle: string
  orderId: number
  amount: number
  type: string
  createdAt: string
}

export interface UserProfileVO {
  id: number
  username: string
  email: string
  avatarUrl: string | null
  creatorLevel: number
  balance: number
  createdAt: string
  templateCount: number
  favoriteCount: number
  totalUseCount: number
  totalIncome: number
  recentTemplates: TemplateVO[]
  incomeRecords: IncomeRecordVO[]
  purchasedTemplates: TemplateVO[]
}

/** 登录 */
export function login(params: LoginParams): Promise<LoginResult> {
  return request.post('/user/login', params)
}

/** 注册 */
export function register(params: RegisterParams): Promise<UserInfo> {
  return request.post('/user/register', params)
}

/** 获取当前登录用户信息 */
export function getCurrentUser(): Promise<UserInfo> {
  return request.get('/user/info')
}

/** 充值 */
export function recharge(amount: number): Promise<UserInfo> {
  return request.post('/user/recharge', { amount })
}

/** 获取用户主页 */
export function getUserProfile(userId: number): Promise<UserProfileVO> {
  return request.get(`/user/profile/${userId}`)
}

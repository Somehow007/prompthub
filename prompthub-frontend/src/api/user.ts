import request from './request'

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

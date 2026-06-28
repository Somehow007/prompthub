import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, getCurrentUser } from '@/api/user'
import type { UserInfo, LoginParams, RegisterParams } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  /** 登录 */
  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.token
    userInfo.value = res.user
    localStorage.setItem('token', res.token)
    return res
  }

  /** 注册 */
  async function register(params: RegisterParams) {
    const res = await registerApi(params)
    return res
  }

  /** 拉取当前用户信息 */
  async function fetchUserInfo() {
    if (!token.value) return
    try {
      userInfo.value = await getCurrentUser()
    } catch {
      // Token 失效，清除本地状态
      logout()
    }
  }

  /** 登出 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  /** 是否已登录 */
  const isLoggedIn = () => !!token.value

  return { token, userInfo, login, register, fetchUserInfo, logout, isLoggedIn }
})

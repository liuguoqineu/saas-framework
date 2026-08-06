import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

/**
 * 用户状态管理
 * 存储当前登录用户信息
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  /** 是否为超级管理员 */
  const isSuperAdmin = computed(() => userInfo.value?.tenantId === 0)

  /** 用户权限列表 */
  const permissions = computed(() => userInfo.value?.permissions || [])

  /** 登录 */
  async function login(username, password) {
    const res = await authApi.login({ username, password })
    token.value = res.data.token
    userInfo.value = res.data.userInfo

    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))

    return res.data
  }

  /** 获取用户信息 */
  async function fetchUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = res.data
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  /** 登出 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isSuperAdmin, permissions, login, fetchUserInfo, logout }
})

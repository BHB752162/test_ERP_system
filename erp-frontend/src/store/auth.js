import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo } from '../api/auth'

const STORAGE_KEY = 'erp_auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem(STORAGE_KEY)
  }

  async function login(credentials) {
    const res = await loginApi(credentials)
    setToken(res.data.token)
    userInfo.value = res.data
    return res.data
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
      return res.data
    } catch (err) {
      // 仅当 401 时才清除登录态，网络抖动等临时错误不登出
      if (err?.response?.status === 401) {
        clearAuth()
      }
      return null
    }
  }

  function hasRole(roleCode) {
    return userInfo.value?.roleCode === roleCode
  }

  function hasAnyRole(roles) {
    return roles.includes(userInfo.value?.roleCode)
  }

  return { token, userInfo, setToken, clearAuth, login, fetchUserInfo, hasRole, hasAnyRole }
}, {
  persist: {
    key: STORAGE_KEY,
    storage: localStorage,
    paths: ['userInfo', 'token']
  }
})

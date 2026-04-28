import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import userApi from '@/api/user'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(readStoredUser())

  const isLoggedIn = computed(() => !!token.value)

  function login(data) {
    token.value = data.token
    userInfo.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.user))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  function updateUserInfo(patch) {
    userInfo.value = { ...(userInfo.value || {}), ...patch }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  async function refreshFromServer() {
    if (!token.value) return
    const user = await userApi.getCurrentUserDetail()
    if (user) {
      userInfo.value = user
      localStorage.setItem('userInfo', JSON.stringify(user))
    }
  }

  return { token, userInfo, isLoggedIn, login, logout, updateUserInfo, refreshFromServer }
})

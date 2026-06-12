import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

/** 登录态：持久化 token 与用户信息到 localStorage。 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('medshop_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('medshop_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role ?? null)
  const roleName = computed(() => {
    const map = { 0: '消费者', 1: '执业药师', 2: '客服', 3: '运营' }
    return map[user.value?.role] ?? '用户'
  })
  const isConsumer = computed(() => role.value === 0)
  const isPharmacist = computed(() => role.value === 1)
  const isOperator = computed(() => role.value === 3)

  function persist() {
    localStorage.setItem('medshop_token', token.value)
    localStorage.setItem('medshop_user', JSON.stringify(user.value))
  }

  async function login(phone, password) {
    const data = await authApi.login(phone, password)
    token.value = data.token
    user.value = { userId: data.userId, role: data.role, phone }
    persist()
  }

  async function register(phone, password, nickname) {
    const data = await authApi.register(phone, password, nickname)
    token.value = data.token
    user.value = { userId: data.userId, role: data.role, phone }
    persist()
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('medshop_token')
    localStorage.removeItem('medshop_user')
  }

  return { token, user, isLoggedIn, role, roleName, isConsumer, isPharmacist, isOperator, login, register, logout }
})

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// 把 authApi 替换为受控桩，避免真实 HTTP 请求
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    setPrivacy: vi.fn()
  }
}))

import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

/**
 * 登录态 store 单元测试：登录/注册写入并持久化 token+user、登出清理、
 * 角色派生属性（roleName / isConsumer / isPharmacist / isOperator）。
 */
describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('初始未登录：token 为空，isLoggedIn 为 false，role 为 null', () => {
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.role).toBe(null)
    expect(auth.roleName).toBe('用户')
  })

  it('登录成功：保存 token/user 并持久化到 localStorage', async () => {
    authApi.login.mockResolvedValue({ token: 'TKN', userId: 1, role: 0 })
    const auth = useAuthStore()

    await auth.login('13800000000', '123456')

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.token).toBe('TKN')
    expect(auth.user).toMatchObject({ userId: 1, role: 0, phone: '13800000000' })
    expect(localStorage.getItem('medshop_token')).toBe('TKN')
    expect(JSON.parse(localStorage.getItem('medshop_user')).userId).toBe(1)
  })

  it('注册成功：同样写入登录态并持久化', async () => {
    authApi.register.mockResolvedValue({ token: 'NEW', userId: 9, role: 0 })
    const auth = useAuthStore()

    await auth.register('13812345678', 'abcdef', '小明')

    expect(authApi.register).toHaveBeenCalledWith('13812345678', 'abcdef', '小明')
    expect(auth.token).toBe('NEW')
    expect(auth.user.userId).toBe(9)
  })

  it('登出：清空 token/user 并移除 localStorage', async () => {
    authApi.login.mockResolvedValue({ token: 'TKN', userId: 1, role: 0 })
    const auth = useAuthStore()
    await auth.login('13800000000', '123456')

    auth.logout()

    expect(auth.isLoggedIn).toBe(false)
    expect(auth.user).toBe(null)
    expect(localStorage.getItem('medshop_token')).toBe(null)
    expect(localStorage.getItem('medshop_user')).toBe(null)
  })

  it('角色派生：消费者(0)', async () => {
    authApi.login.mockResolvedValue({ token: 'T', userId: 1, role: 0 })
    const auth = useAuthStore()
    await auth.login('13800000000', 'x')
    expect(auth.roleName).toBe('消费者')
    expect(auth.isConsumer).toBe(true)
    expect(auth.isPharmacist).toBe(false)
    expect(auth.isOperator).toBe(false)
  })

  it('角色派生：执业药师(1)', async () => {
    authApi.login.mockResolvedValue({ token: 'T', userId: 2, role: 1 })
    const auth = useAuthStore()
    await auth.login('13800000001', 'x')
    expect(auth.roleName).toBe('执业药师')
    expect(auth.isPharmacist).toBe(true)
    expect(auth.isConsumer).toBe(false)
  })

  it('角色派生：运营(3)', async () => {
    authApi.login.mockResolvedValue({ token: 'T', userId: 3, role: 3 })
    const auth = useAuthStore()
    await auth.login('13800000099', 'x')
    expect(auth.roleName).toBe('运营')
    expect(auth.isOperator).toBe(true)
  })

  it('从 localStorage 恢复登录态', () => {
    localStorage.setItem('medshop_token', 'SAVED')
    localStorage.setItem('medshop_user', JSON.stringify({ userId: 5, role: 1, phone: '138' }))
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.role).toBe(1)
    expect(auth.isPharmacist).toBe(true)
  })
})

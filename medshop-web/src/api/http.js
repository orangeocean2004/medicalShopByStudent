import axios from 'axios'

/**
 * axios 实例：统一 baseURL、注入 JWT、解包后端 {code,message,data} 结构。
 * 后端约定 code=0 为成功，非 0 抛业务错误；401 触发登出跳登录。
 */
const http = axios.create({
  baseURL: '/api/v1',
  timeout: 20000
})

// 请求拦截：带上 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('medshop_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：解包 + 统一错误
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      // 业务错误：抛出可读 message
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('medshop_token')
      localStorage.removeItem('medshop_user')
      // 避免在登录页重复跳转
      if (!location.hash.includes('/login')) {
        location.hash = '#/login'
      }
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    const msg = error.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

export default http

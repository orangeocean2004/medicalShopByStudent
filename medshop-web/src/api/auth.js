import http from './http'

/** 认证相关接口（P3）。 */
export const authApi = {
  login: (phone, password) => http.post('/auth/login', { phone, password }),
  register: (phone, password, nickname) =>
    http.post('/auth/register', { phone, password, nickname }),
  // 隐私授权（API-10）
  setPrivacy: (scope, status) => http.put('/privacy/authorization', { scope, status })
}

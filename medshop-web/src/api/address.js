import http from './http'

/** 收货地址接口（任意登录用户管理自己的地址）。 */
export const addressApi = {
  list: () => http.get('/addresses'),
  create: (addr) => http.post('/addresses', addr),
  update: (id, addr) => http.put(`/addresses/${id}`, addr),
  remove: (id) => http.delete(`/addresses/${id}`),
  setDefault: (id) => http.put(`/addresses/${id}/default`)
}

import http from './http'

/** 运营管理后台接口（角色 3 运营）。 */
export const adminApi = {
  // 药品管理
  listDrugs: () => http.get('/admin/drugs'),
  addDrug: (drug) => http.post('/admin/drugs', drug),
  updateDrug: (id, drug) => http.put(`/admin/drugs/${id}`, drug),
  setStock: (id, stock) => http.put(`/admin/drugs/${id}/stock`, { stock }),
  // 订单管理
  listOrders: () => http.get('/admin/orders'),
  setOrderStatus: (id, status) => http.put(`/admin/orders/${id}/status`, { status }),
  // 网站统计
  stats: () => http.get('/admin/stats'),
  // 人员管理
  listUsers: () => http.get('/admin/users'),
  createStaff: (payload) => http.post('/admin/users', payload)
}

/** 药师工作台接口（角色 1 执业药师）。 */
export const pharmacistApi = {
  listTickets: () => http.get('/handoff/tickets'),
  // status: 1 接单/处理中  2 已完成
  updateTicket: (id, status) => http.put(`/handoff/tickets/${id}`, { status }),
  // 在线药师状态（运营/药师可查），返回 { online, count }
  onlineStatus: () => http.get('/handoff/online'),
  // 个人工作量统计
  stats: () => http.get('/handoff/stats')
}

/** 个人中心接口（任意登录用户）。 */
export const userApi = {
  me: () => http.get('/users/me'),
  updateNickname: (nickname) => http.put('/users/me', { nickname }),
  changePassword: (oldPassword, newPassword) =>
    http.put('/users/me/password', { oldPassword, newPassword })
}

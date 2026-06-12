import http from './http'

/** 药品与订单接口（M1）。 */
export const drugApi = {
  search: (keyword, page = 1, size = 12) =>
    http.get('/drugs', { params: { keyword, page, size } }),
  detail: (id) => http.get(`/drugs/${id}`)
}

export const orderApi = {
  create: (addressId, items) => http.post('/orders', { addressId, items }),
  detail: (id) => http.get(`/orders/${id}`),
  // 我的订单列表
  myOrders: () => http.get('/orders')
}

/** 处方接口（M4 处方辅助审核）。 */
export const prescriptionApi = {
  // 上传处方：drugId 关联处方药，imageUrl 为 base64 data URL
  upload: (drugId, imageUrl) => http.post('/prescriptions', { drugId, imageUrl }),
  // 我的处方及审核状态
  mine: () => http.get('/prescriptions/mine'),
  // 待审处方列表（药师）
  pending: () => http.get('/prescriptions/pending'),
  // 审核：status 1通过 2驳回
  review: (id, status, comment) => http.put(`/prescriptions/${id}/review`, { status, comment })
}

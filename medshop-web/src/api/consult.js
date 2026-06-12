import http from './http'

/** 智能药师咨询接口（M2 / AI）。 */
export const consultApi = {
  // 创建会话，返回 { consultationId }
  create: (symptom) => http.post('/consultations', { symptom }),
  // 发送一条咨询消息，返回 { reply, aiSource, confidence, needHandoff }
  send: (consultationId, content) =>
    http.post(`/consultations/${consultationId}/messages`, { content }),
  // 转人工，返回 { ticketId, status, pharmacistId }
  handoff: (consultationId, reason) =>
    http.post(`/consultations/${consultationId}/handoff`, { reason }),
  // 拉取会话全部消息（人工模式下用户/药师轮询），返回 [{ id, senderType, content, createdAt }]
  messages: (consultationId) => http.get(`/consultations/${consultationId}/messages`),
  // 药师在已转人工会话中回复用户
  reply: (consultationId, content) =>
    http.post(`/consultations/${consultationId}/reply`, { content }),
  // 用户结束人工咨询，切回智能药师
  resume: (consultationId) => http.post(`/consultations/${consultationId}/resume`),
  // 在线药师状态，返回 { online: boolean, count: number }
  online: () => http.get('/handoff/online')
}

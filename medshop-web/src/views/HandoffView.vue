<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { pharmacistApi } from '@/api/admin'
import { consultApi } from '@/api/consult'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const tickets = ref([])
const loading = ref(true)
const acting = ref({})
const expanded = ref(null)

// 工单内对话（仅 sourceType=1 药师咨询工单）
const chatMsgs = ref([])
const chatInput = ref('')
const chatSending = ref(false)
const chatScroller = ref(null)
let chatTimer = null
let chatConsultId = null

const STATUS = {
  0: { label: '待接单', cls: 'wait' },
  1: { label: '处理中', cls: 'doing' },
  2: { label: '已完成', cls: 'done' }
}

const pending = computed(() => tickets.value.filter((t) => t.status !== 2).length)

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    tickets.value = await pharmacistApi.listTickets()
  } catch (e) {
    if (!silent) toast.error(e.message)
  } finally {
    loading.value = false
  }
}

let pollTimer = null

onMounted(() => {
  load()
  // 每 30 秒静默刷新工单；轮询同时刷新本药师活跃时间，保持「在线」。
  pollTimer = setInterval(() => load(true), 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (chatTimer) clearInterval(chatTimer)
})

function parseContext(ctx) {
  if (!ctx) return []
  try {
    const arr = JSON.parse(ctx)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function toggle(t) {
  const willOpen = expanded.value !== t.id
  expanded.value = willOpen ? t.id : null
  stopChat()
  if (willOpen && t.sourceType === 1 && t.sourceId) {
    startChat(t.sourceId)
  }
}

async function chatScrollDown() {
  await nextTick()
  if (chatScroller.value) chatScroller.value.scrollTop = chatScroller.value.scrollHeight
}

async function pollChat() {
  if (!chatConsultId) return
  try {
    const list = await consultApi.messages(chatConsultId)
    const prevLen = chatMsgs.value.length
    chatMsgs.value = list
    if (list.length !== prevLen) chatScrollDown()
  } catch {
    // 静默
  }
}

function startChat(consultId) {
  chatConsultId = consultId
  chatMsgs.value = []
  pollChat()
  chatTimer = setInterval(pollChat, 3000)
}

function stopChat() {
  if (chatTimer) { clearInterval(chatTimer); chatTimer = null }
  chatConsultId = null
  chatMsgs.value = []
  chatInput.value = ''
}

async function sendReply() {
  const content = chatInput.value.trim()
  if (!content || chatSending.value || !chatConsultId) return
  chatSending.value = true
  try {
    await consultApi.reply(chatConsultId, content)
    chatInput.value = ''
    await pollChat()
  } catch (e) {
    toast.error(e.message)
  } finally {
    chatSending.value = false
  }
}

function senderLabel(type) {
  return type === 0 ? '用户' : type === 2 ? '我（药师）' : '智能药师'
}

async function act(t, status) {
  acting.value[t.id] = true
  try {
    await pharmacistApi.updateTicket(t.id, status)
    t.status = status
    toast.success(status === 1 ? `已接单 #${t.id}` : `已完成 #${t.id}`)
  } catch (e) {
    toast.error(e.message)
  } finally {
    acting.value[t.id] = false
  }
}
</script>

<template>
  <div class="wb container">
    <header class="wb-head rise">
      <div>
        <p class="overline">HANDOFF TICKETS</p>
        <h1>转人工工单</h1>
        <p class="wb-lead">智能药师转来的人工咨询工单。接单后可在工单内与用户实时对话，问题解决后标记完成。</p>
      </div>
      <div class="wb-stat" :class="{ active: pending }">
        <span class="stat-num">{{ pending }}</span>
        <span class="stat-label">待处理</span>
      </div>
    </header>

    <div v-if="loading" class="state">正在加载工单…</div>

    <div v-else-if="!tickets.length" class="empty rise">
      <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M9 12l2 2 4-4"/><circle cx="12" cy="12" r="9"/></svg>
      <p>暂无转人工工单</p>
    </div>

    <div v-else class="list">
      <div
        v-for="(t, i) in tickets"
        :key="t.id"
        class="ticket rise"
        :style="{ animationDelay: `${Math.min(i * 60, 360)}ms` }"
        :class="{ open: expanded === t.id }"
      >
        <div class="t-bar" @click="toggle(t)">
          <div class="t-left">
            <span class="t-id">#{{ t.id }}</span>
            <span class="t-status" :class="STATUS[t.status]?.cls">{{ STATUS[t.status]?.label }}</span>
            <span class="t-reason">{{ t.reason || '用户请求人工' }}</span>
          </div>
          <div class="t-right">
            <span class="t-user">{{ t.userPhone || ('用户 ' + t.userId) }}</span>
            <span class="t-time">{{ t.createdAt }}</span>
            <svg class="t-chev" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
        </div>

        <transition name="expand">
          <div v-if="expanded === t.id" class="t-body">
            <!-- 实时对话（药师咨询工单 sourceType=1）-->
            <template v-if="t.sourceType === 1">
              <p class="ctx-label">与用户的对话（实时）</p>
              <div class="live-chat" ref="chatScroller">
                <div v-if="!chatMsgs.length" class="chat-empty">加载对话中…</div>
                <div
                  v-for="m in chatMsgs"
                  :key="m.id"
                  class="lc-msg"
                  :class="m.senderType === 0 ? 'from-user' : (m.senderType === 2 ? 'from-me' : 'from-ai')"
                >
                  <span class="lc-who">{{ senderLabel(m.senderType) }}</span>
                  <p class="lc-text">{{ m.content }}</p>
                </div>
              </div>

              <div class="reply-bar">
                <textarea
                  v-model="chatInput"
                  rows="1"
                  placeholder="回复用户…（Enter 发送）"
                  @keydown.enter.exact.prevent="sendReply"
                ></textarea>
                <button class="reply-send" :disabled="!chatInput.trim() || chatSending" @click="sendReply">发送</button>
              </div>

              <div class="t-actions">
                <button v-if="t.status === 0" class="act primary" :disabled="acting[t.id]" @click="act(t, 1)">接单处理</button>
                <button v-if="t.status === 1" class="act done" :disabled="acting[t.id]" @click="act(t, 2)">标记完成</button>
                <span v-if="t.status === 2" class="resolved">✓ 该工单已完成</span>
              </div>
            </template>

            <!-- 其他来源工单：仅展示携带上下文 -->
            <template v-else>
              <p class="ctx-label">对话上下文</p>
              <div v-if="parseContext(t.context).length" class="ctx">
                <div
                  v-for="(m, mi) in parseContext(t.context)"
                  :key="mi"
                  class="msg"
                  :class="(m.role === 'user' || m.sender === 0) ? 'user' : 'ai'"
                >
                  <span class="msg-who">{{ (m.role === 'user' || m.sender === 0) ? '用户' : '智能药师' }}</span>
                  <p class="msg-text">{{ m.content }}</p>
                </div>
              </div>
              <p v-else class="ctx-empty">（无携带的对话记录）</p>

              <div class="t-actions">
                <button v-if="t.status === 0" class="act primary" :disabled="acting[t.id]" @click="act(t, 1)">接单处理</button>
                <button v-if="t.status === 1" class="act done" :disabled="acting[t.id]" @click="act(t, 2)">标记完成</button>
                <span v-if="t.status === 2" class="resolved">✓ 该工单已完成</span>
              </div>
            </template>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wb { padding-top: var(--sp-6); max-width: 880px; }
.wb-head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--sp-5); margin-bottom: var(--sp-6); }
.wb-head h1 { font-size: 36px; margin-top: 6px; }
.wb-lead { color: var(--ink-soft); font-size: 15px; margin-top: var(--sp-3); max-width: 560px; line-height: 1.7; }
.wb-stat {
  flex-shrink: 0; display: flex; flex-direction: column; align-items: center;
  padding: 14px 26px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-md);
}
.wb-stat.active { border-color: var(--clay); background: var(--clay-soft); }
.stat-num { font-family: var(--font-display); font-size: 32px; font-weight: 600; color: var(--ink-faint); }
.wb-stat.active .stat-num { color: var(--clay); }
.stat-label { font-size: 12px; color: var(--ink-faint); margin-top: 2px; }

/* 工单内实时对话 */
.live-chat {
  max-height: 320px; overflow-y: auto;
  display: flex; flex-direction: column; gap: var(--sp-3);
  padding: var(--sp-4); margin-bottom: var(--sp-3);
  background: var(--paper-2); border: 1px solid var(--line); border-radius: var(--r-md);
}
.chat-empty { text-align: center; color: var(--ink-faint); font-size: 13px; padding: var(--sp-4); }
.lc-msg { max-width: 80%; }
.lc-msg.from-user { align-self: flex-start; }
.lc-msg.from-me { align-self: flex-end; text-align: right; }
.lc-msg.from-ai { align-self: flex-start; opacity: 0.85; }
.lc-who { font-size: 11px; font-weight: 700; color: var(--ink-faint); }
.lc-text {
  margin-top: 3px; padding: 9px 13px; border-radius: var(--r-md); font-size: 14px; line-height: 1.55;
  white-space: pre-wrap;
}
.lc-msg.from-user .lc-text { background: var(--paper); border: 1px solid var(--line); border-bottom-left-radius: var(--r-sm); }
.lc-msg.from-me .lc-text { background: var(--pine); color: var(--paper); border-bottom-right-radius: var(--r-sm); }
.lc-msg.from-ai .lc-text { background: var(--paper); border: 1px dashed var(--line-strong); }

.reply-bar { display: flex; gap: var(--sp-3); align-items: flex-end; margin-bottom: var(--sp-4); }
.reply-bar textarea {
  flex: 1; resize: none; padding: 11px 14px; font-size: 14px; font-family: inherit;
  border: 1px solid var(--line); border-radius: var(--r-md); background: var(--paper); line-height: 1.5;
}
.reply-bar textarea:focus { outline: none; border-color: var(--pine); box-shadow: 0 0 0 3px var(--pine-soft); }
.reply-send {
  flex-shrink: 0; padding: 11px 22px; font-size: 14px; font-weight: 600;
  background: var(--pine); color: var(--paper); border-radius: var(--r-md);
  transition: background var(--t-fast), opacity var(--t-fast);
}
.reply-send:hover:not(:disabled) { background: var(--pine-deep); }
.reply-send:disabled { opacity: 0.5; cursor: not-allowed; }

.state { padding: var(--sp-9); text-align: center; color: var(--ink-soft); font-family: var(--font-display); font-size: 19px; }
.empty {
  display: flex; flex-direction: column; align-items: center; gap: var(--sp-4);
  padding: var(--sp-9); background: var(--surface); border: 1px solid var(--line);
  border-radius: var(--r-lg); color: var(--ink-faint);
}

.list { display: flex; flex-direction: column; gap: var(--sp-3); }
.ticket {
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  overflow: hidden; transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.ticket.open { border-color: var(--pine); box-shadow: var(--shadow-md); }

.t-bar { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-4); padding: var(--sp-4) var(--sp-5); cursor: pointer; }
.t-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.t-id { font-family: var(--font-display); font-size: 18px; font-weight: 600; }
.t-status { font-size: 11.5px; font-weight: 700; padding: 3px 10px; border-radius: var(--r-pill); flex-shrink: 0; }
.t-status.wait { background: var(--clay-soft); color: #8f3e23; }
.t-status.doing { background: #fbf0d8; color: #8a6516; }
.t-status.done { background: var(--pine-soft); color: var(--pine); }
.t-reason { font-size: 14px; color: var(--ink-soft); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.t-right { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.t-user { font-family: var(--font-mono); font-size: 13px; color: var(--ink); font-weight: 600; }
.t-time { font-size: 12.5px; color: var(--ink-faint); }
.t-chev { color: var(--ink-faint); transition: transform var(--t-mid) var(--ease); }
.ticket.open .t-chev { transform: rotate(180deg); }

.t-body { padding: 0 var(--sp-5) var(--sp-5); border-top: 1px solid var(--line); }
.ctx-label { font-size: 11px; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--ink-faint); margin: var(--sp-4) 0 var(--sp-3); }
.ctx { display: flex; flex-direction: column; gap: var(--sp-3); }
.msg { max-width: 78%; }
.msg.user { align-self: flex-end; text-align: right; }
.msg.ai { align-self: flex-start; }
.msg-who { font-size: 11px; font-weight: 700; color: var(--ink-faint); }
.msg-text {
  margin-top: 4px; padding: 10px 14px; border-radius: var(--r-md); font-size: 14px; line-height: 1.6;
  white-space: pre-wrap;
}
.msg.user .msg-text { background: var(--pine); color: var(--paper); border-bottom-right-radius: var(--r-sm); }
.msg.ai .msg-text { background: var(--paper-2); color: var(--ink); border-bottom-left-radius: var(--r-sm); }
.ctx-empty { color: var(--ink-faint); font-size: 14px; padding: var(--sp-3) 0; }

.t-actions { margin-top: var(--sp-5); display: flex; gap: var(--sp-3); align-items: center; }
.act { padding: 10px 22px; font-size: 14px; font-weight: 600; border-radius: var(--r-pill); transition: background var(--t-fast), opacity var(--t-fast); }
.act:disabled { opacity: 0.5; cursor: wait; }
.act.primary { background: var(--clay); color: #fff; }
.act.primary:hover:not(:disabled) { background: #b3552f; }
.act.done { background: var(--pine); color: var(--paper); }
.act.done:hover:not(:disabled) { background: var(--pine-deep); }
.resolved { color: var(--success); font-weight: 600; font-size: 14px; }

.expand-enter-active, .expand-leave-active { transition: all var(--t-mid) var(--ease); overflow: hidden; }
.expand-enter-from, .expand-leave-to { opacity: 0; max-height: 0; }
.expand-enter-to, .expand-leave-from { max-height: 800px; }

@media (max-width: 640px) {
  .wb-head { flex-direction: column; }
  .t-time, .t-reason { display: none; }
  .msg { max-width: 90%; }
}
</style>

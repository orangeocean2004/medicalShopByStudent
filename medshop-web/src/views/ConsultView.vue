<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { consultApi } from '@/api/consult'
import { useToastStore } from '@/stores/toast'
import ConfidenceMeter from '@/components/ui/ConfidenceMeter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const toast = useToastStore()

const consultationId = ref(null)
const messages = ref([]) // { role: 'user'|'ai'|'system'|'pharmacist', text, aiSource?, confidence?, needHandoff? }
const input = ref('')
const sending = ref(false)
const handing = ref(false)
const handed = ref(false)
const scroller = ref(null)

// 人工模式：转人工后 AI 退出，3 秒轮询拉药师回复
let chatTimer = null
let lastMsgId = 0   // 已渲染的最大服务端消息ID，用于增量拉取药师消息

// 药师在线状态（轮询）
const pharmacistOnline = ref(false)
const onlineCount = ref(0)
const onlineLoaded = ref(false)
let onlineTimer = null

const suggestions = [
  '成人感冒发烧头痛吃什么药？',
  '布洛芬饭前吃还是饭后吃？',
  '孕妇可以吃布洛芬吗？',
  '孩子半夜发烧38.5℃怎么办？'
]

async function refreshOnline() {
  try {
    const res = await consultApi.online()
    pharmacistOnline.value = !!res.online
    onlineCount.value = res.count || 0
  } catch {
    pharmacistOnline.value = false
    onlineCount.value = 0
  } finally {
    onlineLoaded.value = true
  }
}

async function ensureSession() {
  if (consultationId.value) return
  const res = await consultApi.create('在线咨询')
  consultationId.value = res.consultationId
}

async function scrollDown() {
  await nextTick()
  if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
}

async function send(text) {
  const content = (text ?? input.value).trim()
  if (!content || sending.value) return
  input.value = ''
  messages.value.push({ role: 'user', text: content })
  scrollDown()

  // 人工模式：直接把消息发给后端，不显示 AI 思考气泡，等药师轮询回复
  if (handed.value) {
    sending.value = true
    try {
      await consultApi.send(consultationId.value, content)
    } catch (e) {
      toast.error(e.message)
    } finally {
      sending.value = false
      scrollDown()
    }
    return
  }

  sending.value = true
  // 占位「正在思考」气泡
  messages.value.push({ role: 'ai', pending: true, text: '' })
  scrollDown()

  try {
    await ensureSession()
    const res = await consultApi.send(consultationId.value, content)
    // 替换占位气泡
    const idx = messages.value.findIndex((m) => m.pending)
    messages.value[idx] = {
      role: 'ai',
      text: res.reply,
      aiSource: res.aiSource,
      confidence: res.confidence,
      needHandoff: res.needHandoff
    }
  } catch (e) {
    const idx = messages.value.findIndex((m) => m.pending)
    if (idx >= 0) messages.value.splice(idx, 1)
    toast.error(e.message)
  } finally {
    sending.value = false
    scrollDown()
  }
}

// 人工模式轮询：拉取药师新回复（senderType=2）并追加到对话
async function pollPharmacistMessages() {
  if (!consultationId.value) return
  try {
    const list = await consultApi.messages(consultationId.value)
    let appended = false
    for (const m of list) {
      if (m.id > lastMsgId) lastMsgId = m.id
      // 只追加药师消息（用户自己的消息已在本地即时渲染）
      if (m.senderType === 2 && !messages.value.some((x) => x.serverId === m.id)) {
        messages.value.push({ role: 'pharmacist', text: m.content, serverId: m.id, time: m.createdAt })
        appended = true
      }
    }
    if (appended) scrollDown()
  } catch {
    // 轮询失败静默
  }
}

function startChatPolling() {
  if (chatTimer) return
  pollPharmacistMessages()
  chatTimer = setInterval(pollPharmacistMessages, 3000)
}

// 结束人工咨询，切回智能药师
const resuming = ref(false)
async function resumeAi() {
  if (!consultationId.value || resuming.value) return
  resuming.value = true
  try {
    await consultApi.resume(consultationId.value)
    handed.value = false
    if (chatTimer) { clearInterval(chatTimer); chatTimer = null }
    messages.value.push({
      role: 'system',
      text: '已结束人工咨询，智能药师已返回，可以继续向我提问。'
    })
    scrollDown()
  } catch (e) {
    toast.error(e.message)
  } finally {
    resuming.value = false
  }
}

async function handoff() {
  if (!consultationId.value || handing.value || handed.value) return
  // 前端先按在线状态拦一道，避免无谓请求；后端仍会做权威判定
  if (!pharmacistOnline.value) {
    toast.info('当前没有在线药师，请稍后再试')
    return
  }
  handing.value = true
  try {
    const res = await consultApi.handoff(consultationId.value, '用户请求人工确认')
    handed.value = true
    // 记录当前已有消息的最大ID，避免把转人工前的历史当成「药师新消息」重复渲染
    try {
      const existing = await consultApi.messages(consultationId.value)
      lastMsgId = existing.reduce((mx, m) => Math.max(mx, m.id), 0)
    } catch { /* 忽略 */ }
    messages.value.push({
      role: 'system',
      text: res.pharmacistId
        ? `已转接执业药师（工单 #${res.ticketId}）。智能助手已退出，你现在可以直接和药师对话，TA 会尽快回复。`
        : `已创建人工工单 #${res.ticketId}，当前药师繁忙，将尽快为你安排。`
    })
    toast.success('已转接人工药师')
    startChatPolling()
    scrollDown()
  } catch (e) {
    // 后端在转接瞬间判定无在线药师时也会走到这里
    toast.error(e.message)
    refreshOnline()
  } finally {
    handing.value = false
  }
}

onMounted(async () => {
  messages.value.push({
    role: 'ai',
    text: '你好，我是松和堂的智能药师。可以描述你的症状或想了解的药品，我会给出有依据的用药建议；遇到拿不准的情况，我会主动建议你转接人工药师。',
    intro: true
  })
  // 预先建立会话，使「转接人工」按钮在用户尚未提问时也可用
  try {
    await ensureSession()
  } catch {
    // 后端未就绪时静默失败，首次发送消息时会重试建会话
  }
  // 在线状态轮询：立即查一次，之后每 30 秒刷新
  refreshOnline()
  onlineTimer = setInterval(refreshOnline, 30000)
})

onUnmounted(() => {
  if (onlineTimer) clearInterval(onlineTimer)
  if (chatTimer) clearInterval(chatTimer)
})
</script>

<template>
  <div class="consult container">
    <!-- 顶部信息 -->
    <header class="c-head rise">
      <div class="ch-left">
        <span class="ch-avatar">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 3a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V7a4 4 0 0 1 4-4z"/><path d="M5 21c0-3.5 3.1-6 7-6s7 2.5 7 6" stroke-linecap="round"/><circle cx="18" cy="6" r="2.5" fill="currentColor" stroke="none" opacity="0.3"/></svg>
        </span>
        <div>
          <h1>智能药师</h1>
          <p class="ch-sub">
            <span class="dot" :class="{ off: !pharmacistOnline }"></span>
            <template v-if="!onlineLoaded">检测药师状态…</template>
            <template v-else-if="pharmacistOnline">人工药师在线{{ onlineCount > 1 ? `（${onlineCount}人）` : '' }} · 可随时转接</template>
            <template v-else>人工药师当前离线 · 智能药师仍可解答</template>
          </p>
        </div>
      </div>
      <BaseButton
        v-if="handed"
        variant="line"
        size="sm"
        :loading="resuming"
        @click="resumeAi"
      >
        返回智能药师
      </BaseButton>
      <BaseButton
        v-else
        variant="line"
        size="sm"
        :loading="handing"
        :disabled="!consultationId || !pharmacistOnline"
        @click="handoff"
      >
        {{ pharmacistOnline ? '转接人工药师' : '药师离线' }}
      </BaseButton>
    </header>

    <!-- 对话区 -->
    <div class="chat rise" style="animation-delay: 80ms">
      <div class="messages" ref="scroller">
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="msg"
          :class="m.role"
        >
          <!-- 系统提示（转人工）-->
          <div v-if="m.role === 'system'" class="sys">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3a9 9 0 1 0 9 9" stroke-linecap="round"/><path d="m9 12 2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/></svg>
            {{ m.text }}
          </div>

          <!-- 普通气泡 -->
          <template v-else>
            <div v-if="m.role === 'ai'" class="bubble-avatar" aria-hidden="true">药</div>
            <div v-else-if="m.role === 'pharmacist'" class="bubble-avatar human" aria-hidden="true">师</div>
            <div class="bubble">
              <!-- 思考中 -->
              <div v-if="m.pending" class="typing">
                <span></span><span></span><span></span>
              </div>
              <template v-else>
                <span v-if="m.role === 'pharmacist'" class="pharm-tag">执业药师</span>
                <p class="bubble-text">{{ m.text }}</p>

                <!-- AI 元信息：来源 + 置信度 + 转人工建议 -->
                <div v-if="m.role === 'ai' && !m.intro && m.confidence !== undefined" class="ai-meta">
                  <div class="meta-left">
                    <div v-if="m.aiSource" class="source">
                      <span class="src-label">知识来源</span>
                      <span class="src-val">{{ m.aiSource }}</span>
                    </div>
                    <div v-else class="source">
                      <span class="src-label">知识来源</span>
                      <span class="src-val faint">未命中知识库</span>
                    </div>
                  </div>
                  <ConfidenceMeter :value="Number(m.confidence)" />
                </div>

                <!-- 转人工建议条 -->
                <div v-if="m.needHandoff" class="handoff-hint">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 9v4m0 3.5v.2M10.3 3.9 2.6 17.3A2 2 0 0 0 4.3 20h15.4a2 2 0 0 0 1.7-2.7L13.7 3.9a2 2 0 0 0-3.4 0z" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  <span>该问题置信度较低或涉及风险，建议转接人工药师确认。</span>
                  <button v-if="!handed" :disabled="!pharmacistOnline || handing" @click="handoff">
                    {{ pharmacistOnline ? '立即转接' : '药师离线' }}
                  </button>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>

      <!-- 引导问题 -->
      <div v-if="messages.length <= 1" class="suggest">
        <span class="suggest-label">你可以这样问</span>
        <div class="suggest-list">
          <button v-for="s in suggestions" :key="s" @click="send(s)">{{ s }}</button>
        </div>
      </div>

      <!-- 输入栏 -->
      <div class="composer">
        <textarea
          v-model="input"
          rows="1"
          :placeholder="handed ? '与执业药师对话…（Enter 发送）' : '描述症状或想了解的药品…（Enter 发送）'"
          @keydown.enter.exact.prevent="send()"
        ></textarea>
        <button class="send" :disabled="!input.trim() || sending" @click="send()">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 12 20 4l-3 8 3 8L4 12z" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
      </div>
    </div>

    <p class="disclaimer">
      智能药师建议仅供参考，不能替代执业医师诊断。症状持续或加重请及时就医。
    </p>
  </div>
</template>

<style scoped>
.consult { padding-top: var(--sp-6); max-width: 880px; }

/* 头部 */
.c-head {
  display: flex; align-items: center; justify-content: space-between; gap: var(--sp-4);
  padding-bottom: var(--sp-4);
}
.ch-left { display: flex; align-items: center; gap: 14px; }
.ch-avatar {
  width: 54px; height: 54px; border-radius: 16px;
  display: grid; place-items: center;
  background: linear-gradient(150deg, var(--pine), var(--pine-deep));
  color: var(--paper);
}
.c-head h1 { font-size: 26px; }
.ch-sub { display: flex; align-items: center; gap: 7px; font-size: 13px; color: var(--ink-soft); margin-top: 3px; }
.ch-sub .dot { width: 7px; height: 7px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 3px rgba(63,125,82,0.18); }
.ch-sub .dot.off { background: var(--ink-faint); box-shadow: 0 0 0 3px rgba(120,120,120,0.14); }

/* 对话容器 */
.chat {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  display: flex; flex-direction: column;
}
.messages {
  padding: var(--sp-5);
  min-height: 380px;
  max-height: 56vh;
  overflow-y: auto;
  display: flex; flex-direction: column; gap: var(--sp-5);
  background-image: radial-gradient(var(--paper-2) 1px, transparent 1px);
  background-size: 22px 22px;
}

/* 消息行 */
.msg { display: flex; gap: 12px; max-width: 88%; }
.msg.user { align-self: flex-end; flex-direction: row-reverse; }
.msg.system { align-self: center; max-width: 92%; }

.bubble-avatar {
  width: 36px; height: 36px; border-radius: 11px; flex-shrink: 0;
  display: grid; place-items: center;
  background: var(--pine); color: var(--paper);
  font-family: var(--font-display); font-weight: 600; font-size: 16px;
}
.bubble {
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 15px; line-height: 1.65;
}
.msg.ai .bubble { background: var(--paper); border: 1px solid var(--line); border-top-left-radius: 4px; }
.msg.user .bubble { background: var(--pine); color: var(--paper); border-top-right-radius: 4px; }
.msg.pharmacist .bubble { background: var(--clay-soft); border: 1px solid var(--clay); border-top-left-radius: 4px; }
.bubble-avatar.human { background: var(--clay); }
.pharm-tag {
  display: inline-block; margin-bottom: 6px;
  font-size: 11px; font-weight: 700; letter-spacing: 0.04em;
  color: #8f3e23;
}
.bubble-text { white-space: pre-wrap; }

/* 系统提示 */
.sys {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px;
  background: var(--pine-soft); color: var(--pine);
  border-radius: var(--r-pill);
  font-size: 13.5px; font-weight: 500;
}

/* 思考动画 */
.typing { display: flex; gap: 5px; padding: 4px 2px; }
.typing span { width: 8px; height: 8px; border-radius: 50%; background: var(--sage); animation: bounce 1.2s infinite ease-in-out; }
.typing span:nth-child(2) { animation-delay: 0.15s; }
.typing span:nth-child(3) { animation-delay: 0.3s; }
@keyframes bounce { 0%, 60%, 100% { transform: translateY(0); opacity: 0.5; } 30% { transform: translateY(-6px); opacity: 1; } }

/* AI 元信息 */
.ai-meta {
  display: flex; align-items: center; justify-content: space-between; gap: var(--sp-4);
  margin-top: var(--sp-4); padding-top: var(--sp-4);
  border-top: 1px dashed var(--line);
}
.source { display: flex; flex-direction: column; gap: 4px; }
.src-label { font-size: 11px; font-weight: 700; letter-spacing: 0.08em; color: var(--ink-faint); text-transform: uppercase; }
.src-val { font-size: 13.5px; font-weight: 600; color: var(--pine); line-height: 1.4; }
.src-val.faint { color: var(--ink-faint); font-weight: 500; }

/* 转人工建议 */
.handoff-hint {
  display: flex; align-items: center; gap: 9px; flex-wrap: wrap;
  margin-top: var(--sp-3); padding: 11px 14px;
  background: var(--clay-soft); border-radius: var(--r-md);
  font-size: 13px; color: #8f3e23; line-height: 1.5;
}
.handoff-hint svg { flex-shrink: 0; }
.handoff-hint button {
  margin-left: auto;
  padding: 6px 14px;
  background: var(--clay); color: #fff;
  border-radius: var(--r-pill);
  font-size: 12.5px; font-weight: 700;
  transition: background var(--t-fast);
}
.handoff-hint button:hover:not(:disabled) { background: #b1542f; }
.handoff-hint button:disabled { background: var(--line-strong); cursor: not-allowed; }

/* 引导问题 */
.suggest { padding: 0 var(--sp-5) var(--sp-4); }
.suggest-label { font-size: 12px; font-weight: 700; color: var(--ink-faint); letter-spacing: 0.06em; }
.suggest-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.suggest-list button {
  padding: 9px 15px; font-size: 13.5px; font-weight: 500;
  background: var(--paper); border: 1px solid var(--line);
  border-radius: var(--r-pill); color: var(--ink-soft);
  transition: all var(--t-fast) var(--ease);
}
.suggest-list button:hover { border-color: var(--pine); color: var(--pine); background: var(--pine-soft); }

/* 输入栏 */
.composer {
  display: flex; align-items: flex-end; gap: 10px;
  padding: var(--sp-4) var(--sp-5);
  border-top: 1px solid var(--line);
  background: var(--surface);
}
.composer textarea {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 12px 16px;
  font-size: 15px; line-height: 1.5;
  resize: none; max-height: 120px;
  background: var(--paper);
  transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.composer textarea:focus { outline: none; border-color: var(--pine); box-shadow: 0 0 0 3px var(--pine-soft); background: #fff; }
.send {
  display: grid; place-items: center;
  width: 46px; height: 46px; flex-shrink: 0;
  background: var(--pine); color: var(--paper);
  border-radius: 13px;
  transition: background var(--t-fast), transform var(--t-fast);
}
.send:hover:not(:disabled) { background: var(--pine-deep); transform: translateY(-1px); }
.send:disabled { background: var(--line-strong); cursor: not-allowed; }

.disclaimer { text-align: center; font-size: 12.5px; color: var(--ink-faint); margin-top: var(--sp-4); line-height: 1.5; }

@media (max-width: 640px) {
  .msg { max-width: 96%; }
  .ai-meta { flex-direction: column; align-items: flex-start; }
}
</style>

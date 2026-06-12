<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { prescriptionApi } from '@/api/shop'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

const rxList = ref([])
const rxLoading = ref(true)
const rxActing = ref({})
const rxExpanded = ref(null)
const rxComment = ref({}) // 每张处方的驳回备注输入

const rxPending = computed(() => rxList.value.length)

async function loadRx(silent = false) {
  if (!silent) rxLoading.value = true
  try {
    rxList.value = await prescriptionApi.pending()
  } catch (e) {
    if (!silent) toast.error(e.message)
  } finally {
    rxLoading.value = false
  }
}

let pollTimer = null

onMounted(() => {
  loadRx()
  // 每 30 秒静默刷新待审处方；轮询同时刷新本药师活跃时间，保持「在线」。
  pollTimer = setInterval(() => loadRx(true), 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

function rxToggle(p) {
  rxExpanded.value = rxExpanded.value === p.id ? null : p.id
}

async function review(p, status) {
  // 驳回时要求填写原因
  if (status === 2 && !(rxComment.value[p.id] || '').trim()) {
    toast.error('请填写驳回原因')
    return
  }
  rxActing.value[p.id] = true
  try {
    await prescriptionApi.review(p.id, status, rxComment.value[p.id] || '')
    toast.success(status === 1 ? `已通过处方 #${p.id}` : `已驳回处方 #${p.id}`)
    rxList.value = rxList.value.filter((x) => x.id !== p.id)
    rxExpanded.value = null
  } catch (e) {
    toast.error(e.message)
  } finally {
    rxActing.value[p.id] = false
  }
}
</script>

<template>
  <div class="wb container">
    <header class="wb-head rise">
      <div>
        <p class="overline">PRESCRIPTION REVIEW</p>
        <h1>处方审核</h1>
        <p class="wb-lead">消费者购买处方药需上传处方，由你核验真伪与有效性。通过后用户才能下单，驳回需注明原因。</p>
      </div>
      <div class="wb-stat" :class="{ active: rxPending }">
        <span class="stat-num">{{ rxPending }}</span>
        <span class="stat-label">待审核</span>
      </div>
    </header>

    <div v-if="rxLoading" class="state">正在加载待审处方…</div>

    <div v-else-if="!rxList.length" class="empty rise">
      <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M9 12l2 2 4-4"/><circle cx="12" cy="12" r="9"/></svg>
      <p>暂无待审处方</p>
    </div>

    <div v-else class="list">
      <div
        v-for="(p, i) in rxList"
        :key="p.id"
        class="ticket rise"
        :style="{ animationDelay: `${Math.min(i * 60, 360)}ms` }"
        :class="{ open: rxExpanded === p.id }"
      >
        <div class="t-bar" @click="rxToggle(p)">
          <div class="t-left">
            <span class="t-id">#{{ p.id }}</span>
            <span class="t-status wait">待审核</span>
            <span class="t-reason">购买「{{ p.drugName || ('药品 ' + p.drugId) }}」</span>
          </div>
          <div class="t-right">
            <span class="t-user">{{ p.userPhone || ('用户 ' + p.userId) }}</span>
            <span class="t-time">{{ p.createdAt }}</span>
            <svg class="t-chev" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
        </div>

        <transition name="expand">
          <div v-if="rxExpanded === p.id" class="t-body">
            <p class="ctx-label">处方图片</p>
            <a :href="p.imageUrl" target="_blank" class="rx-img-wrap">
              <img :src="p.imageUrl" alt="处方图片" class="rx-img" />
            </a>

            <label class="rx-comment-field">
              <span>审核备注（驳回时必填）</span>
              <input v-model="rxComment[p.id]" type="text" placeholder="如：处方信息不清晰 / 已过期" />
            </label>

            <div class="t-actions">
              <button class="act done" :disabled="rxActing[p.id]" @click="review(p, 1)">通过</button>
              <button class="act reject" :disabled="rxActing[p.id]" @click="review(p, 2)">驳回</button>
            </div>
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

/* 处方审核 */
.rx-img-wrap { display: block; margin-bottom: var(--sp-4); border-radius: var(--r-md); overflow: hidden; border: 1px solid var(--line); }
.rx-img { display: block; width: 100%; max-height: 360px; object-fit: contain; background: var(--paper-2); }
.rx-comment-field { display: block; margin-bottom: var(--sp-4); }
.rx-comment-field span { display: block; font-size: 12.5px; font-weight: 600; color: var(--ink-soft); margin-bottom: 6px; }
.rx-comment-field input {
  width: 100%; padding: 10px 13px; font-size: 14px;
  border: 1px solid var(--line); border-radius: var(--r-md); background: var(--paper);
}
.rx-comment-field input:focus { outline: none; border-color: var(--pine); box-shadow: 0 0 0 3px var(--pine-soft); }
.act.reject { background: var(--clay); color: #fff; }
.act.reject:hover:not(:disabled) { background: #b3552f; }

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
.t-reason { font-size: 14px; color: var(--ink-soft); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.t-right { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.t-user { font-family: var(--font-mono); font-size: 13px; color: var(--ink); font-weight: 600; }
.t-time { font-size: 12.5px; color: var(--ink-faint); }
.t-chev { color: var(--ink-faint); transition: transform var(--t-mid) var(--ease); }
.ticket.open .t-chev { transform: rotate(180deg); }

.t-body { padding: 0 var(--sp-5) var(--sp-5); border-top: 1px solid var(--line); }
.ctx-label { font-size: 11px; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--ink-faint); margin: var(--sp-4) 0 var(--sp-3); }

.t-actions { margin-top: var(--sp-5); display: flex; gap: var(--sp-3); align-items: center; }
.act { padding: 10px 22px; font-size: 14px; font-weight: 600; border-radius: var(--r-pill); transition: background var(--t-fast), opacity var(--t-fast); }
.act:disabled { opacity: 0.5; cursor: wait; }
.act.done { background: var(--pine); color: var(--paper); }
.act.done:hover:not(:disabled) { background: var(--pine-deep); }

.expand-enter-active, .expand-leave-active { transition: all var(--t-mid) var(--ease); overflow: hidden; }
.expand-enter-from, .expand-leave-to { opacity: 0; max-height: 0; }
.expand-enter-to, .expand-leave-from { max-height: 800px; }

@media (max-width: 640px) {
  .wb-head { flex-direction: column; }
  .t-time, .t-reason { display: none; }
}
</style>

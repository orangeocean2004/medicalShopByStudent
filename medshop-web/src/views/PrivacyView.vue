<script setup>
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

// C-3：隐私授权可随时撤回。本地维护开关态，调用后端记录。
const scopes = ref([
  {
    key: 'healthProfile',
    title: '健康档案',
    desc: '允许保存并使用你的过敏史、慢性病等健康信息，让用药建议更贴合你的情况。',
    enabled: true,
    loading: false
  },
  {
    key: 'recommendation',
    title: '个性化推荐',
    desc: '基于你的浏览与购买记录推荐可能需要的药品与健康内容。',
    enabled: true,
    loading: false
  }
])

async function toggle(scope) {
  const next = scope.enabled ? 0 : 1
  scope.loading = true
  try {
    await authApi.setPrivacy(scope.key, next)
    scope.enabled = !scope.enabled
    toast.success(scope.enabled ? `已开启「${scope.title}」` : `已撤回「${scope.title}」`)
  } catch (e) {
    toast.error(e.message)
  } finally {
    scope.loading = false
  }
}
</script>

<template>
  <div class="privacy container">
    <header class="p-head rise">
      <p class="overline">PRIVACY & CONSENT</p>
      <h1>隐私与授权</h1>
      <p class="p-lead">
        你的数据由你做主。每一项授权都可以随时开启或撤回，撤回后我们将停止相应的数据使用。
      </p>
    </header>

    <div class="cards">
      <div
        v-for="(s, i) in scopes"
        :key="s.key"
        class="p-card rise"
        :style="{ animationDelay: `${i * 80}ms` }"
        :class="{ off: !s.enabled }"
      >
        <div class="pc-icon">
          <svg v-if="s.key === 'healthProfile'" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 21s-7-4.5-9.5-9C1 9 2.5 5 6 5c2 0 3.2 1.2 4 2.3C10.8 6.2 12 5 14 5c3.5 0 5 4 3.5 7-2.5 4.5-5.5 9-5.5 9z"/></svg>
          <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1"/><circle cx="12" cy="12" r="3.5"/></svg>
        </div>

        <div class="pc-body">
          <div class="pc-top">
            <h3>{{ s.title }}</h3>
            <span class="pc-status" :class="{ on: s.enabled }">
              {{ s.enabled ? '已授权' : '已撤回' }}
            </span>
          </div>
          <p class="pc-desc">{{ s.desc }}</p>
        </div>

        <button
          class="switch"
          :class="{ on: s.enabled, busy: s.loading }"
          @click="toggle(s)"
          :disabled="s.loading"
          :aria-pressed="s.enabled"
        >
          <span class="knob"></span>
        </button>
      </div>
    </div>

    <div class="note rise" style="animation-delay: 200ms">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8v.2" stroke-linecap="round"/></svg>
      <p>
        依据约束 C-3，授权状态变更会被完整记录用于合规审计。撤回授权不影响你已经完成的订单与历史咨询。
      </p>
    </div>
  </div>
</template>

<style scoped>
.privacy { padding-top: var(--sp-7); max-width: 760px; }
.p-head { margin-bottom: var(--sp-6); }
.p-head h1 { font-size: 40px; margin-top: 6px; }
.p-lead { color: var(--ink-soft); font-size: 16px; margin-top: var(--sp-3); line-height: 1.7; max-width: 560px; }

.cards { display: flex; flex-direction: column; gap: var(--sp-4); }
.p-card {
  display: flex; align-items: center; gap: var(--sp-4);
  padding: var(--sp-5);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  transition: border-color var(--t-mid), opacity var(--t-mid);
}
.p-card.off { opacity: 0.7; }
.pc-icon {
  width: 52px; height: 52px; flex-shrink: 0;
  display: grid; place-items: center;
  border-radius: 14px;
  background: var(--pine-soft); color: var(--pine);
}
.p-card.off .pc-icon { background: var(--paper-2); color: var(--ink-faint); }
.pc-body { flex: 1; }
.pc-top { display: flex; align-items: center; gap: 10px; }
.pc-top h3 { font-size: 19px; }
.pc-status {
  font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: var(--r-pill);
  background: var(--paper-2); color: var(--ink-faint);
}
.pc-status.on { background: var(--pine-soft); color: var(--pine); }
.pc-desc { color: var(--ink-soft); font-size: 14px; margin-top: 6px; line-height: 1.6; }

/* 开关 */
.switch {
  position: relative; flex-shrink: 0;
  width: 52px; height: 30px;
  border-radius: var(--r-pill);
  background: var(--line-strong);
  transition: background var(--t-mid) var(--ease);
}
.switch.on { background: var(--pine); }
.switch.busy { opacity: 0.6; cursor: wait; }
.knob {
  position: absolute; top: 3px; left: 3px;
  width: 24px; height: 24px; border-radius: 50%;
  background: #fff;
  box-shadow: var(--shadow-sm);
  transition: transform var(--t-mid) var(--ease);
}
.switch.on .knob { transform: translateX(22px); }

.note {
  display: flex; align-items: flex-start; gap: 11px;
  margin-top: var(--sp-6); padding: var(--sp-4) var(--sp-5);
  background: var(--paper-2);
  border-radius: var(--r-md);
  font-size: 13.5px; color: var(--ink-soft); line-height: 1.6;
}
.note svg { flex-shrink: 0; margin-top: 2px; color: var(--sage); }

@media (max-width: 640px) {
  .p-head h1 { font-size: 30px; }
}
</style>

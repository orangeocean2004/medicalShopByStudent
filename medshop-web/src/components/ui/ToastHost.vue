<script setup>
import { useToastStore } from '@/stores/toast'
const toast = useToastStore()
</script>

<template>
  <Teleport to="body">
    <div class="toast-host">
      <transition-group name="toast">
        <div v-for="t in toast.list" :key="t.id" class="toast" :class="t.type">
          <span class="ic" aria-hidden="true">
            <svg v-if="t.type === 'success'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2"><path d="m5 13 4 4L19 7" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <svg v-else-if="t.type === 'error'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 8v5M12 16.5v.5" stroke-linecap="round"/><circle cx="12" cy="12" r="9"/></svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 11v6M12 7.5v.5" stroke-linecap="round"/><circle cx="12" cy="12" r="9"/></svg>
          </span>
          <span class="msg">{{ t.message }}</span>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: calc(var(--header-h) + 14px);
  left: 50%; transform: translateX(-50%);
  z-index: 200;
  display: flex; flex-direction: column; gap: 10px;
  pointer-events: none;
}
.toast {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-left-width: 4px;
  border-radius: var(--r-md);
  box-shadow: var(--shadow-lg);
  font-size: 14.5px; font-weight: 500;
  min-width: 240px; max-width: 90vw;
}
.ic { display: grid; place-items: center; }
.toast.success { border-left-color: var(--success); }
.toast.success .ic { color: var(--success); }
.toast.error { border-left-color: var(--danger); }
.toast.error .ic { color: var(--danger); }
.toast.info { border-left-color: var(--pine); }
.toast.info .ic { color: var(--pine); }

.toast-enter-active { transition: all var(--t-mid) var(--ease-out); }
.toast-leave-active { transition: all var(--t-fast) var(--ease); position: absolute; }
.toast-enter-from { opacity: 0; transform: translateY(-12px) scale(0.96); }
.toast-leave-to { opacity: 0; transform: scale(0.96); }
</style>

<script setup>
// 处方/非处方标识。isRx: 1 处方药(Rx) / 0 非处方(OTC)
defineProps({ isRx: { type: [Number, String], default: 0 } })
</script>

<template>
  <span class="badge" :class="Number(isRx) === 1 ? 'rx' : 'otc'">
    <span class="dot"></span>
    {{ Number(isRx) === 1 ? '处方药 Rx' : '非处方 OTC' }}
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 11px 4px 9px;
  font-size: 12px; font-weight: 700;
  letter-spacing: 0.02em;
  border-radius: var(--r-pill);
  border: 1px solid transparent;
}
.dot { width: 6px; height: 6px; border-radius: 50%; }
.otc { background: var(--pine-soft); color: var(--pine); border-color: #cfe0d5; }
.otc .dot { background: var(--pine); }
.rx { background: var(--clay-soft); color: #a8482a; border-color: #e8c9bb; }
.rx .dot { background: var(--clay); }
</style>

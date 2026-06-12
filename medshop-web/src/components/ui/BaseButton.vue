<script setup>
defineProps({
  variant: { type: String, default: 'primary' }, // primary | ghost | clay | line
  size: { type: String, default: 'md' },          // sm | md | lg
  block: Boolean,
  loading: Boolean,
  disabled: Boolean,
  type: { type: String, default: 'button' }
})

const emit = defineEmits(['click'])
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`v-${variant}`, `s-${size}`, { block, loading }]"
    :disabled="disabled || loading"
    @click="emit('click', $event)"
  >
    <span v-if="loading" class="spinner" aria-hidden="true"></span>
    <span class="btn-label"><slot /></span>
  </button>
</template>

<style scoped>
.btn {
  position: relative;
  display: inline-flex; align-items: center; justify-content: center;
  gap: 8px;
  font-family: var(--font-body);
  font-weight: 600;
  border-radius: var(--r-pill);
  transition: transform var(--t-fast) var(--ease),
              background var(--t-fast) var(--ease),
              box-shadow var(--t-fast) var(--ease),
              color var(--t-fast) var(--ease),
              border-color var(--t-fast) var(--ease);
  white-space: nowrap;
}
.btn:active:not(:disabled) { transform: translateY(1px) scale(0.99); }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.block { width: 100%; }

/* sizes */
.s-sm { padding: 7px 16px; font-size: 13.5px; }
.s-md { padding: 11px 24px; font-size: 15px; }
.s-lg { padding: 15px 34px; font-size: 16.5px; }

/* variants */
.v-primary { background: var(--pine); color: var(--paper); box-shadow: var(--shadow-sm); }
.v-primary:hover:not(:disabled) { background: var(--pine-deep); box-shadow: var(--shadow-md); }

.v-clay { background: var(--clay); color: #fff; box-shadow: var(--shadow-sm); }
.v-clay:hover:not(:disabled) { background: #b1542f; box-shadow: var(--shadow-md); }

.v-ghost { background: var(--pine-soft); color: var(--pine); }
.v-ghost:hover:not(:disabled) { background: #d7e4db; }

.v-line { background: transparent; color: var(--ink); border: 1px solid var(--line-strong); }
.v-line:hover:not(:disabled) { border-color: var(--pine); color: var(--pine); }

.loading .btn-label { opacity: 0.85; }
.spinner {
  width: 15px; height: 15px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>

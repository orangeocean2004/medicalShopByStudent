<script setup>
import { useRouter } from 'vue-router'
import RxBadge from '@/components/ui/RxBadge.vue'

const props = defineProps({
  drug: { type: Object, required: true },
  index: { type: Number, default: 0 }
})
const emit = defineEmits(['add'])
const router = useRouter()

function open() {
  router.push({ name: 'drug-detail', params: { id: props.drug.id } })
}
</script>

<template>
  <article
    class="card rise"
    :style="{ animationDelay: `${index * 60}ms` }"
    @click="open"
  >
    <div class="card-top">
      <RxBadge :is-rx="drug.isRx" />
      <span class="cat">{{ drug.category }}</span>
    </div>

    <h3 class="name">{{ drug.name }}</h3>
    <p class="indication">{{ drug.indication }}</p>

    <div class="card-foot">
      <div class="price">
        <span class="cur">¥</span>{{ drug.price }}
      </div>
      <button
        class="add"
        :disabled="drug.stock === 0"
        @click.stop="emit('add', drug)"
        :title="drug.stock === 0 ? '已售罄' : '加入购物车'"
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14" stroke-linecap="round"/></svg>
      </button>
    </div>

    <span v-if="drug.stock === 0" class="sold">售罄</span>
  </article>
</template>

<style scoped>
.card {
  position: relative;
  display: flex; flex-direction: column;
  padding: var(--sp-5);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  cursor: pointer;
  transition: transform var(--t-mid) var(--ease-out),
              box-shadow var(--t-mid) var(--ease-out),
              border-color var(--t-mid) var(--ease);
  overflow: hidden;
}
.card::before {
  /* 左上角的细弧装饰，hover 时显现，增加层次 */
  content: '';
  position: absolute; inset: 0;
  pointer-events: none;
  background: radial-gradient(circle at 100% 0%, var(--pine-soft), transparent 38%);
  opacity: 0;
  transition: opacity var(--t-mid) var(--ease);
}
.card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-md);
  border-color: #cfe0d5;
}
.card:hover::before { opacity: 1; }

.card-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; position: relative; }
.cat { font-size: 12px; color: var(--ink-faint); font-weight: 500; }

.name {
  font-size: 19px; margin-top: var(--sp-4);
  position: relative;
}
.indication {
  margin-top: 8px;
  font-size: 13.5px; color: var(--ink-soft);
  line-height: 1.55;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
  position: relative;
}

.card-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: var(--sp-5);
  padding-top: var(--sp-4);
  border-top: 1px dashed var(--line);
  position: relative;
}
.price { font-family: var(--font-display); font-size: 26px; font-weight: 600; color: var(--ink); }
.cur { font-size: 15px; color: var(--clay); margin-right: 1px; }

.add {
  display: grid; place-items: center;
  width: 40px; height: 40px;
  border-radius: 50%;
  background: var(--pine); color: var(--paper);
  transition: transform var(--t-fast) var(--ease), background var(--t-fast);
}
.add:hover:not(:disabled) { background: var(--pine-deep); transform: rotate(90deg); }
.add:disabled { background: var(--line-strong); cursor: not-allowed; }

.sold {
  position: absolute; top: 14px; right: -34px;
  background: var(--ink); color: var(--paper);
  font-size: 11px; font-weight: 700;
  padding: 4px 36px;
  transform: rotate(45deg);
}
</style>

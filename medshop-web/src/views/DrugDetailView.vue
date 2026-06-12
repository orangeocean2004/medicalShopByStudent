<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { drugApi } from '@/api/shop'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import RxBadge from '@/components/ui/RxBadge.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()

const drug = ref(null)
const loading = ref(true)
const qty = ref(1)

async function load() {
  loading.value = true
  try {
    drug.value = await drugApi.detail(route.params.id)
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function addToCart() {
  cart.add(drug.value, qty.value)
  toast.success(`已加入 ${qty.value} 件「${drug.value.name}」`)
}

function buyNow() {
  cart.add(drug.value, qty.value)
  router.push({ name: 'cart' })
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="detail container">
    <button class="back" @click="router.back()">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5m6 6-6-6 6-6" stroke-linecap="round" stroke-linejoin="round"/></svg>
      返回
    </button>

    <div v-if="loading" class="loading">正在抓药…</div>

    <div v-else-if="drug" class="detail-grid">
      <!-- 左：视觉面板 -->
      <div class="visual rise">
        <div class="visual-inner" :class="drug.isRx === 1 ? 'rx' : 'otc'">
          <span class="v-cat">{{ drug.category }}</span>
          <svg class="v-icon" viewBox="0 0 24 24" width="80" height="80" fill="none" stroke="currentColor" stroke-width="1.1">
            <path d="M16 3c5 4 8 8 8 13a8 8 0 0 1-16 0c0-5 3-9 8-13z" transform="translate(-4 0)"/>
          </svg>
          <span class="v-name">{{ drug.name }}</span>
        </div>
      </div>

      <!-- 右：信息 -->
      <div class="info rise" style="animation-delay: 100ms">
        <div class="info-head">
          <RxBadge :is-rx="drug.isRx" />
          <span class="stock" :class="{ low: drug.stock < 10 }">
            {{ drug.stock > 0 ? `库存 ${drug.stock}` : '暂时缺货' }}
          </span>
        </div>

        <h1 class="d-name">{{ drug.name }}</h1>
        <div class="d-price"><span>¥</span>{{ drug.price }}</div>

        <!-- 处方药提示 -->
        <div v-if="drug.isRx === 1" class="rx-note">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 9v4m0 3.5v.2M10.3 3.9 2.6 17.3A2 2 0 0 0 4.3 20h15.4a2 2 0 0 0 1.7-2.7L13.7 3.9a2 2 0 0 0-3.4 0z" stroke-linecap="round" stroke-linejoin="round"/></svg>
          此为<strong>处方药</strong>，下单前需上传处方并通过药师审核。
          <a class="rx-link" @click="router.push({ name: 'prescriptions', query: { drugId: drug.id } })">去上传处方 →</a>
        </div>

        <!-- 用药信息 -->
        <dl class="specs">
          <div class="spec">
            <dt>适应症</dt>
            <dd>{{ drug.indication || '—' }}</dd>
          </div>
          <div class="spec">
            <dt>用法用量</dt>
            <dd>{{ drug.dosage || '—' }}</dd>
          </div>
          <div class="spec">
            <dt>禁忌</dt>
            <dd>{{ drug.contraindication || '—' }}</dd>
          </div>
        </dl>

        <!-- 购买操作 -->
        <div class="buy">
          <div class="stepper">
            <button @click="qty > 1 && qty--" :disabled="qty <= 1">−</button>
            <span>{{ qty }}</span>
            <button @click="qty < drug.stock && qty++" :disabled="qty >= drug.stock">+</button>
          </div>
          <BaseButton variant="line" :disabled="drug.stock === 0" @click="addToCart">加入购物车</BaseButton>
          <BaseButton :disabled="drug.stock === 0" @click="buyNow">立即购买</BaseButton>
        </div>
        <p v-if="drug.isRx === 1" class="rx-tip">处方药需审核通过后方可结算；未通过时下单会被拦截并提示。</p>

        <button class="ask" @click="router.push({ name: 'consult' })">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M8 10.5h8M8 14h5M21 11.5a8.5 8.5 0 0 1-12.2 7.6L3 20.5l1.5-5.3A8.5 8.5 0 1 1 21 11.5z" stroke-linecap="round" stroke-linejoin="round"/></svg>
          关于这款药，问问智能药师
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail { padding-top: var(--sp-6); }
.back {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 14.5px; font-weight: 600; color: var(--ink-soft);
  padding: 8px 0; margin-bottom: var(--sp-4);
  transition: gap var(--t-fast), color var(--t-fast);
}
.back:hover { color: var(--pine); gap: 10px; }
.loading { padding: var(--sp-9); text-align: center; color: var(--ink-soft); font-family: var(--font-display); font-size: 20px; }

.detail-grid { display: grid; grid-template-columns: 0.85fr 1.15fr; gap: var(--sp-7); align-items: start; }

/* 视觉面板 */
.visual { position: sticky; top: calc(var(--header-h) + 24px); }
.visual-inner {
  aspect-ratio: 4 / 5;
  border-radius: var(--r-lg);
  display: flex; flex-direction: column; justify-content: space-between;
  padding: var(--sp-6);
  color: var(--paper);
  position: relative; overflow: hidden;
}
.visual-inner.otc { background: linear-gradient(155deg, var(--pine), var(--pine-deep)); }
.visual-inner.rx { background: linear-gradient(155deg, var(--clay), #a8482a); }
.visual-inner::after {
  content: ''; position: absolute; right: -40px; bottom: -40px;
  width: 220px; height: 220px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.18);
  pointer-events: none;
}
.v-cat { font-size: 13px; letter-spacing: 0.16em; text-transform: uppercase; opacity: 0.8; font-weight: 600; }
.v-icon { opacity: 0.92; align-self: center; }
.v-name { font-family: var(--font-display); font-size: 26px; font-weight: 600; }

/* 信息 */
.info-head { display: flex; align-items: center; gap: var(--sp-3); }
.stock { font-size: 13px; font-weight: 600; color: var(--success); }
.stock.low { color: var(--clay); }
.d-name { font-size: clamp(30px, 4vw, 44px); margin-top: var(--sp-3); }
.d-price { font-family: var(--font-display); font-size: 40px; font-weight: 600; color: var(--ink); margin-top: var(--sp-2); }
.d-price span { font-size: 22px; color: var(--clay); margin-right: 2px; }

.rx-note {
  display: flex; align-items: flex-start; gap: 10px;
  margin-top: var(--sp-5);
  padding: var(--sp-4);
  background: var(--clay-soft);
  border-radius: var(--r-md);
  font-size: 14px; color: #8f3e23; line-height: 1.55;
}
.rx-note svg { flex-shrink: 0; margin-top: 2px; }
.rx-note strong { font-weight: 700; }
.rx-link { color: var(--clay); font-weight: 700; cursor: pointer; white-space: nowrap; }
.rx-link:hover { text-decoration: underline; }
.rx-tip { margin-top: 10px; font-size: 12.5px; color: var(--ink-faint); }

.specs { margin-top: var(--sp-6); display: flex; flex-direction: column; }
.spec { display: grid; grid-template-columns: 96px 1fr; gap: var(--sp-4); padding: var(--sp-4) 0; border-top: 1px solid var(--line); }
.spec dt { font-size: 13px; font-weight: 700; color: var(--ink-faint); letter-spacing: 0.05em; padding-top: 2px; }
.spec dd { font-size: 15px; color: var(--ink); line-height: 1.65; }

.buy { display: flex; align-items: center; gap: var(--sp-3); margin-top: var(--sp-6); flex-wrap: wrap; }
.stepper {
  display: inline-flex; align-items: center;
  border: 1px solid var(--line-strong); border-radius: var(--r-pill);
  overflow: hidden;
}
.stepper button { width: 40px; height: 44px; font-size: 20px; color: var(--ink); transition: background var(--t-fast); }
.stepper button:hover:not(:disabled) { background: var(--pine-soft); color: var(--pine); }
.stepper button:disabled { opacity: 0.35; cursor: not-allowed; }
.stepper span { min-width: 38px; text-align: center; font-weight: 700; font-size: 16px; }

.ask {
  display: inline-flex; align-items: center; gap: 9px;
  margin-top: var(--sp-5); padding: 12px 18px;
  width: 100%;
  background: var(--pine-soft); color: var(--pine);
  border-radius: var(--r-md);
  font-size: 14.5px; font-weight: 600;
  transition: background var(--t-fast);
}
.ask:hover { background: #d7e4db; }

@media (max-width: 880px) {
  .detail-grid { grid-template-columns: 1fr; gap: var(--sp-5); }
  .visual { position: static; max-width: 320px; }
}
</style>

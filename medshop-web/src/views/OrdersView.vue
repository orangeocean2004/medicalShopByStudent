<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api/shop'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const toast = useToastStore()

const orders = ref([])
const loading = ref(true)

// 0待支付 1已支付 2配送中 3已完成 4已取消
const STATUS = {
  0: { label: '待支付', cls: 'pending' },
  1: { label: '已支付', cls: 'paid' },
  2: { label: '配送中', cls: 'shipping' },
  3: { label: '已完成', cls: 'done' },
  4: { label: '已取消', cls: 'cancelled' }
}

onMounted(async () => {
  try {
    orders.value = await orderApi.myOrders()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="orders container">
    <header class="o-head rise">
      <p class="overline">MY ORDERS</p>
      <h1>我的订单</h1>
      <p class="o-lead">这里汇总你在松和堂的全部购药记录，点击任意订单可查看物流与明细。</p>
    </header>

    <div v-if="loading" class="state">正在加载订单…</div>

    <div v-else-if="!orders.length" class="empty rise">
      <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M3 4h2l2.4 12.3a1 1 0 0 0 1 .8h8.7a1 1 0 0 0 1-.8L21 8H6" stroke-linecap="round" stroke-linejoin="round"/><circle cx="9.5" cy="20" r="1.3"/><circle cx="17.5" cy="20" r="1.3"/></svg>
      <p>还没有订单</p>
      <button class="go-shop" @click="router.push({ name: 'drugs', query: { q: '感冒' } })">去逛逛药品</button>
    </div>

    <div v-else class="list">
      <button
        v-for="(o, i) in orders"
        :key="o.orderId"
        class="o-card rise"
        :style="{ animationDelay: `${Math.min(i * 60, 360)}ms` }"
        @click="router.push({ name: 'order-detail', params: { id: o.orderId } })"
      >
        <div class="oc-main">
          <div class="oc-top">
            <span class="oc-no">订单 #{{ o.orderId }}</span>
            <span class="oc-status" :class="STATUS[o.status]?.cls">{{ STATUS[o.status]?.label }}</span>
          </div>
          <span class="oc-time">{{ o.createdAt }}</span>
        </div>
        <div class="oc-right">
          <div class="oc-amount">
            <span class="oc-cur">¥</span>{{ o.totalAmount }}
          </div>
          <span v-if="o.logisticsNo" class="oc-logi">物流 {{ o.logisticsNo }}</span>
          <svg class="oc-arrow" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
      </button>
    </div>
  </div>
</template>

<style scoped>
.orders { padding-top: var(--sp-7); max-width: 820px; }
.o-head { margin-bottom: var(--sp-6); }
.o-head h1 { font-size: 38px; margin-top: 6px; }
.o-lead { color: var(--ink-soft); font-size: 15.5px; margin-top: var(--sp-3); }

.state { padding: var(--sp-9); text-align: center; color: var(--ink-soft); font-family: var(--font-display); font-size: 19px; }

.empty {
  display: flex; flex-direction: column; align-items: center; gap: var(--sp-4);
  padding: var(--sp-9) var(--sp-5);
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  color: var(--ink-faint);
}
.empty p { font-size: 16px; }
.go-shop {
  padding: 10px 22px; background: var(--pine); color: var(--paper);
  border-radius: var(--r-pill); font-weight: 600; font-size: 14px;
  transition: background var(--t-fast);
}
.go-shop:hover { background: var(--pine-deep); }

.list { display: flex; flex-direction: column; gap: var(--sp-3); }
.o-card {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; text-align: left;
  padding: var(--sp-4) var(--sp-5);
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  transition: border-color var(--t-fast), transform var(--t-fast), box-shadow var(--t-fast);
}
.o-card:hover { border-color: var(--pine); transform: translateY(-2px); box-shadow: var(--shadow-md); }

.oc-top { display: flex; align-items: center; gap: 12px; }
.oc-no { font-family: var(--font-display); font-size: 18px; font-weight: 600; }
.oc-status { font-size: 12px; font-weight: 700; padding: 3px 11px; border-radius: var(--r-pill); }
.oc-status.pending { background: var(--clay-soft); color: #8f3e23; }
.oc-status.paid { background: #e7eef6; color: #345b86; }
.oc-status.shipping { background: #fbf0d8; color: #8a6516; }
.oc-status.done { background: var(--pine-soft); color: var(--pine); }
.oc-status.cancelled { background: var(--paper-2); color: var(--ink-faint); }
.oc-time { display: block; font-size: 13px; color: var(--ink-faint); margin-top: 7px; }

.oc-right { display: flex; align-items: center; gap: var(--sp-4); }
.oc-amount { font-family: var(--font-display); font-size: 24px; font-weight: 600; color: var(--pine); }
.oc-cur { font-size: 15px; margin-right: 1px; }
.oc-logi { font-family: var(--font-mono); font-size: 12px; color: var(--ink-faint); letter-spacing: 0.03em; }
.oc-arrow { color: var(--ink-faint); transition: transform var(--t-fast); }
.o-card:hover .oc-arrow { transform: translateX(3px); color: var(--pine); }

@media (max-width: 600px) {
  .o-head h1 { font-size: 30px; }
  .oc-logi { display: none; }
}
</style>

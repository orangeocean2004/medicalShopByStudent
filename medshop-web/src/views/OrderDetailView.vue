<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api/shop'
import { useToastStore } from '@/stores/toast'
import BaseButton from '@/components/ui/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const order = ref(null)
const loading = ref(true)

// 订单状态：0待支付 1已支付 2已发货 3已完成 4已取消
const STATUS = {
  0: { label: '待支付', step: 0 },
  1: { label: '已支付', step: 1 },
  2: { label: '已发货', step: 2 },
  3: { label: '已完成', step: 3 },
  4: { label: '已取消', step: -1 }
}
const steps = ['下单', '支付', '发货', '完成']
const curStep = computed(() => STATUS[order.value?.status]?.step ?? 0)
const statusLabel = computed(() => STATUS[order.value?.status]?.label ?? '未知')

onMounted(async () => {
  try {
    order.value = await orderApi.detail(route.params.id)
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="order container">
    <div v-if="loading" class="loading">正在查询订单…</div>

    <div v-else-if="order">
      <!-- 成功横幅 -->
      <div class="banner rise">
        <div class="b-check">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="2.4"><path d="m4 13 5 5L20 7" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <div class="b-text">
          <h1>下单成功</h1>
          <p>订单号 <strong>#{{ order.orderId }}</strong> · 当前状态 <span class="st">{{ statusLabel }}</span></p>
        </div>
        <div class="b-amount">
          <span class="ba-label">应付金额</span>
          <span class="ba-num">¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <!-- 状态时间线 -->
      <div class="timeline rise" style="animation-delay: 80ms">
        <div
          v-for="(s, i) in steps"
          :key="s"
          class="t-step"
          :class="{ done: i <= curStep, active: i === curStep }"
        >
          <div class="t-dot">
            <svg v-if="i < curStep" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3"><path d="m5 13 4 4L19 7" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <span class="t-label">{{ s }}</span>
          <div v-if="i < steps.length - 1" class="t-line"></div>
        </div>
      </div>

      <!-- 物流 -->
      <div class="logi rise" style="animation-delay: 140ms">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 7h11v9H3zM14 10h4l3 3v3h-7" stroke-linecap="round" stroke-linejoin="round"/><circle cx="7" cy="18" r="1.6"/><circle cx="17.5" cy="18" r="1.6"/></svg>
        <span class="logi-label">物流单号</span>
        <span class="logi-no">{{ order.logisticsNo || '待发货后生成' }}</span>
      </div>

      <!-- 明细 -->
      <div class="detail-card rise" style="animation-delay: 200ms">
        <h3>商品明细</h3>
        <table v-if="order.items && order.items.length">
          <thead>
            <tr><th>药品</th><th>单价</th><th>数量</th><th class="r">小计</th></tr>
          </thead>
          <tbody>
            <tr v-for="it in order.items" :key="it.id">
              <td class="td-name">{{ it.drugName || ('药品 #' + it.drugId) }}</td>
              <td>¥{{ it.unitPrice }}</td>
              <td>×{{ it.quantity }}</td>
              <td class="r">¥{{ (it.unitPrice * it.quantity).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="no-items">明细加载中或暂无数据</p>

        <div class="dc-total">
          <span>合计</span>
          <span class="dct-num">¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <div class="actions">
        <BaseButton variant="line" @click="router.push({ name: 'drugs', query: { q: '感冒' } })">继续购药</BaseButton>
        <BaseButton @click="router.push({ name: 'home' })">返回首页</BaseButton>
      </div>
    </div>

    <div v-else class="loading">未找到订单</div>
  </div>
</template>

<style scoped>
.order { padding-top: var(--sp-7); max-width: 820px; }
.loading { padding: var(--sp-9); text-align: center; color: var(--ink-soft); font-family: var(--font-display); font-size: 20px; }

.banner {
  display: flex; align-items: center; gap: var(--sp-4);
  padding: var(--sp-5) var(--sp-6);
  background: linear-gradient(150deg, var(--pine), var(--pine-deep));
  border-radius: var(--r-lg);
  color: var(--paper);
  position: relative; overflow: hidden;
}
.banner::after { content: ''; position: absolute; right: -50px; top: -50px; width: 200px; height: 200px; border-radius: 50%; background: radial-gradient(circle, rgba(200,100,60,0.4), transparent 70%); pointer-events: none; }
.b-check { display: grid; place-items: center; width: 60px; height: 60px; border-radius: 50%; background: rgba(255,255,255,0.16); color: #fff; flex-shrink: 0; }
.b-text { position: relative; }
.b-text h1 { color: var(--paper); font-size: 28px; }
.b-text p { color: rgba(244,240,232,0.82); font-size: 14.5px; margin-top: 4px; }
.b-text strong { color: #fff; }
.st { color: #e9b48a; font-weight: 600; }
.b-amount { margin-left: auto; text-align: right; position: relative; }
.ba-label { display: block; font-size: 12px; color: rgba(244,240,232,0.7); }
.ba-num { font-family: var(--font-display); font-size: 30px; font-weight: 600; color: #fff; }

/* 时间线 */
.timeline { display: flex; justify-content: space-between; margin: var(--sp-6) 0; padding: 0 var(--sp-3); }
.t-step { position: relative; flex: 1; display: flex; flex-direction: column; align-items: center; }
.t-dot {
  width: 38px; height: 38px; border-radius: 50%;
  display: grid; place-items: center;
  background: var(--surface); border: 2px solid var(--line-strong);
  color: var(--ink-faint); font-weight: 700; font-size: 14px;
  z-index: 2; transition: all var(--t-mid) var(--ease);
}
.t-label { margin-top: 8px; font-size: 13.5px; font-weight: 600; color: var(--ink-faint); transition: color var(--t-mid); }
.t-line { position: absolute; top: 19px; left: 50%; width: 100%; height: 2px; background: var(--line-strong); z-index: 1; transition: background var(--t-mid); }
.t-step.done .t-dot { background: var(--pine); border-color: var(--pine); color: #fff; }
.t-step.done .t-label { color: var(--pine); }
.t-step.done .t-line { background: var(--pine); }
.t-step.active .t-dot { box-shadow: 0 0 0 5px var(--pine-soft); }

/* 物流 */
.logi {
  display: flex; align-items: center; gap: 10px;
  padding: var(--sp-4) var(--sp-5);
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-md);
  margin-bottom: var(--sp-4);
}
.logi svg { color: var(--pine); }
.logi-label { font-size: 13px; color: var(--ink-faint); font-weight: 600; }
.logi-no { font-family: var(--font-mono); font-weight: 600; margin-left: auto; letter-spacing: 0.04em; }

/* 明细 */
.detail-card { padding: var(--sp-5); background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg); }
.detail-card h3 { font-size: 20px; margin-bottom: var(--sp-4); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 12px; font-weight: 700; color: var(--ink-faint); letter-spacing: 0.06em; text-transform: uppercase; padding-bottom: 10px; border-bottom: 1px solid var(--line); }
td { padding: 14px 0; font-size: 15px; border-bottom: 1px solid var(--line); }
.td-name { font-weight: 600; }
.r { text-align: right; }
.no-items { color: var(--ink-soft); padding: var(--sp-4) 0; }
.dc-total { display: flex; justify-content: space-between; align-items: baseline; padding-top: var(--sp-4); }
.dc-total > span:first-child { font-weight: 600; }
.dct-num { font-family: var(--font-display); font-size: 26px; font-weight: 600; color: var(--pine); }

.actions { display: flex; gap: var(--sp-3); justify-content: flex-end; margin-top: var(--sp-6); }

@media (max-width: 640px) {
  .banner { flex-wrap: wrap; }
  .b-amount { margin-left: 0; text-align: left; }
}
</style>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { orderApi } from '@/api/shop'
import { addressApi } from '@/api/address'
import RxBadge from '@/components/ui/RxBadge.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()

// 收货地址：真实拉取，默认选中默认地址
const addresses = ref([])
const addrLoading = ref(true)
const selectedId = ref(null)
const showPicker = ref(false)
const submitting = ref(false)

const selectedAddr = computed(() =>
  addresses.value.find((a) => a.id === selectedId.value) || null
)

function maskPhone(p) {
  return p && p.length === 11 ? p.slice(0, 3) + '****' + p.slice(7) : p
}

async function loadAddresses() {
  addrLoading.value = true
  try {
    addresses.value = await addressApi.list()
    const def = addresses.value.find((a) => a.isDefault === 1)
    selectedId.value = def ? def.id : (addresses.value[0]?.id ?? null)
  } catch (e) {
    toast.error(e.message)
  } finally {
    addrLoading.value = false
  }
}

function pick(a) {
  selectedId.value = a.id
  showPicker.value = false
}

async function checkout() {
  if (!cart.items.length) return
  if (!selectedId.value) {
    toast.error('请先添加收货地址')
    router.push({ name: 'profile' })
    return
  }
  submitting.value = true
  try {
    const items = cart.items.map((i) => ({ drugId: i.id, quantity: i.quantity }))
    const res = await orderApi.create(selectedId.value, items)
    toast.success('下单成功')
    cart.clear()
    router.push({ name: 'order-detail', params: { id: res.orderId } })
  } catch (e) {
    // 处方药未通过审核会被后端拦截，提示并引导去上传处方
    toast.error(e.message)
    if (/处方/.test(e.message)) {
      router.push({ name: 'prescriptions' })
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadAddresses)
</script>

<template>
  <div class="cart container">
    <div class="head">
      <p class="overline">YOUR BAG</p>
      <h1>购物车</h1>
    </div>

    <!-- 空车 -->
    <div v-if="!cart.items.length" class="empty">
      <div class="e-art">
        <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M3 4h2l2.4 12.3a1 1 0 0 0 1 .8h8.7a1 1 0 0 0 1-.8L21 8H6" stroke-linecap="round" stroke-linejoin="round"/><circle cx="9.5" cy="20" r="1.4"/><circle cx="17.5" cy="20" r="1.4"/></svg>
      </div>
      <h3>购物车还是空的</h3>
      <p>去挑些需要的药品吧。</p>
      <BaseButton @click="router.push({ name: 'drugs', query: { q: '感冒' } })">浏览药品</BaseButton>
    </div>

    <!-- 有商品 -->
    <div v-else class="layout">
      <!-- 商品列表 -->
      <div class="items">
        <div v-for="it in cart.items" :key="it.id" class="item">
          <div class="i-thumb" :class="it.isRx === 1 ? 'rx' : 'otc'">
            {{ it.name[0] }}
          </div>
          <div class="i-main">
            <div class="i-top">
              <h4>{{ it.name }}</h4>
              <RxBadge :is-rx="it.isRx" />
            </div>
            <span class="i-unit">单价 ¥{{ it.price }}</span>
          </div>
          <div class="stepper">
            <button @click="cart.setQuantity(it.id, it.quantity - 1)">−</button>
            <span>{{ it.quantity }}</span>
            <button @click="cart.setQuantity(it.id, it.quantity + 1)">+</button>
          </div>
          <div class="i-sum">¥{{ (it.price * it.quantity).toFixed(2) }}</div>
          <button class="i-del" @click="cart.remove(it.id)" title="移除">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M5 7h14M10 7V5h4v2m-7 0 1 13h8l1-13" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
      </div>

      <!-- 结算栏 -->
      <aside class="summary">
        <h3>订单摘要</h3>

        <!-- 收货地址 -->
        <div class="addr">
          <div class="addr-top">
            <span class="addr-h">配送至</span>
            <button
              v-if="addresses.length"
              class="addr-switch"
              @click="showPicker = !showPicker"
            >{{ showPicker ? '收起' : '切换地址' }}</button>
          </div>

          <div v-if="addrLoading" class="addr-loading">加载地址中…</div>

          <!-- 无地址 -->
          <div v-else-if="!addresses.length" class="addr-none">
            <p>还没有收货地址</p>
            <button class="addr-add-link" @click="router.push({ name: 'profile' })">去个人中心添加 →</button>
          </div>

          <!-- 已选地址 -->
          <template v-else-if="!showPicker && selectedAddr">
            <p class="addr-name">{{ selectedAddr.receiver }} · {{ maskPhone(selectedAddr.phone) }}</p>
            <p class="addr-detail">{{ selectedAddr.region }} {{ selectedAddr.detail }}</p>
          </template>

          <!-- 地址选择列表 -->
          <ul v-else-if="showPicker" class="addr-pick">
            <li
              v-for="a in addresses"
              :key="a.id"
              class="pick-item"
              :class="{ on: a.id === selectedId }"
              @click="pick(a)"
            >
              <div>
                <span class="pick-name">{{ a.receiver }} · {{ maskPhone(a.phone) }}</span>
                <span v-if="a.isDefault === 1" class="pick-def">默认</span>
                <p class="pick-detail">{{ a.region }} {{ a.detail }}</p>
              </div>
              <svg v-if="a.id === selectedId" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="m5 12 5 5 9-9" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </li>
          </ul>
        </div>

        <div class="rows">
          <div class="row"><span>商品件数</span><span>{{ cart.count }} 件</span></div>
          <div class="row"><span>配送费</span><span class="free">免运费</span></div>
        </div>

        <div class="total">
          <span>合计</span>
          <span class="total-num">¥{{ cart.total.toFixed(2) }}</span>
        </div>

        <!-- 处方药提醒 -->
        <div v-if="cart.hasRx" class="rx-warn">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 9v4m0 3.5v.2M10.3 3.9 2.6 17.3A2 2 0 0 0 4.3 20h15.4a2 2 0 0 0 1.7-2.7L13.7 3.9a2 2 0 0 0-3.4 0z" stroke-linecap="round" stroke-linejoin="round"/></svg>
          含处方药，结算时将校验处方。未上传或未通过审核将无法下单，
          <a class="rx-warn-link" @click="router.push({ name: 'prescriptions' })">去上传处方</a>。
        </div>

        <BaseButton size="lg" block :loading="submitting" :disabled="!addresses.length" @click="checkout">
          结算下单
        </BaseButton>
        <p class="tip">{{ addresses.length ? '下单后将生成待支付订单' : '请先添加收货地址' }}</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.cart { padding-top: var(--sp-7); }
.head { margin-bottom: var(--sp-6); }
.head h1 { font-size: 38px; margin-top: 6px; }

.empty { text-align: center; padding: var(--sp-9) var(--sp-5); }
.e-art { display: inline-grid; place-items: center; width: 84px; height: 84px; border-radius: 50%; background: var(--pine-soft); color: var(--pine); margin-bottom: var(--sp-4); }
.empty h3 { font-size: 24px; }
.empty p { color: var(--ink-soft); margin: 10px 0 var(--sp-5); }

.layout { display: grid; grid-template-columns: 1fr 360px; gap: var(--sp-6); align-items: start; }

/* 商品行 */
.items { display: flex; flex-direction: column; gap: var(--sp-3); }
.item {
  display: grid;
  grid-template-columns: 64px 1fr auto auto auto;
  align-items: center; gap: var(--sp-4);
  padding: var(--sp-4);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.item:hover { border-color: #cfe0d5; box-shadow: var(--shadow-sm); }
.i-thumb {
  width: 64px; height: 64px; border-radius: var(--r-md);
  display: grid; place-items: center;
  font-family: var(--font-display); font-size: 28px; font-weight: 600; color: var(--paper);
}
.i-thumb.otc { background: linear-gradient(150deg, var(--pine), var(--pine-deep)); }
.i-thumb.rx { background: linear-gradient(150deg, var(--clay), #a8482a); }
.i-top { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.i-top h4 { font-size: 17px; }
.i-unit { font-size: 13px; color: var(--ink-faint); }

.stepper { display: inline-flex; align-items: center; border: 1px solid var(--line-strong); border-radius: var(--r-pill); overflow: hidden; }
.stepper button { width: 32px; height: 36px; font-size: 18px; transition: background var(--t-fast); }
.stepper button:hover { background: var(--pine-soft); color: var(--pine); }
.stepper span { min-width: 32px; text-align: center; font-weight: 700; }

.i-sum { font-family: var(--font-display); font-size: 20px; font-weight: 600; min-width: 76px; text-align: right; }
.i-del { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 8px; color: var(--ink-faint); transition: all var(--t-fast); }
.i-del:hover { color: var(--danger); background: var(--clay-soft); }

/* 结算 */
.summary {
  position: sticky; top: calc(var(--header-h) + 24px);
  padding: var(--sp-5);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-md);
}
.summary h3 { font-size: 22px; }
.addr { margin: var(--sp-4) 0; padding: var(--sp-4); background: var(--paper); border-radius: var(--r-md); border: 1px dashed var(--line-strong); }
.addr-top { display: flex; align-items: center; justify-content: space-between; }
.addr-h { font-size: 12px; font-weight: 700; color: var(--clay); letter-spacing: 0.06em; }
.addr-switch { font-size: 12.5px; font-weight: 600; color: var(--pine); transition: opacity var(--t-fast); }
.addr-switch:hover { opacity: 0.7; }
.addr-name { font-weight: 600; margin-top: 6px; }
.addr-detail { font-size: 13.5px; color: var(--ink-soft); margin-top: 3px; line-height: 1.5; }
.addr-loading { font-size: 13px; color: var(--ink-faint); margin-top: 8px; }
.addr-none { margin-top: 8px; }
.addr-none p { font-size: 13.5px; color: var(--ink-soft); }
.addr-add-link { margin-top: 6px; font-size: 13px; font-weight: 700; color: var(--clay); }
.addr-add-link:hover { text-decoration: underline; }

.addr-pick { display: flex; flex-direction: column; gap: 8px; margin-top: 10px; }
.pick-item {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 10px 12px; border: 1px solid var(--line); border-radius: var(--r-md);
  cursor: pointer; transition: border-color var(--t-fast), background var(--t-fast); color: var(--pine);
}
.pick-item:hover { border-color: var(--pine); }
.pick-item.on { border-color: var(--pine); background: var(--pine-soft); }
.pick-name { font-weight: 600; font-size: 14px; color: var(--ink); }
.pick-def { margin-left: 8px; font-size: 10.5px; font-weight: 700; color: var(--pine); border: 1px solid var(--pine); padding: 0 6px; border-radius: var(--r-pill); }
.pick-detail { font-size: 12.5px; color: var(--ink-soft); margin-top: 3px; line-height: 1.45; }

.rows { display: flex; flex-direction: column; gap: 10px; padding: var(--sp-4) 0; border-top: 1px solid var(--line); }
.row { display: flex; justify-content: space-between; font-size: 14.5px; color: var(--ink-soft); }
.free { color: var(--success); font-weight: 600; }

.total { display: flex; align-items: baseline; justify-content: space-between; padding: var(--sp-4) 0; border-top: 1px solid var(--line); margin-bottom: var(--sp-4); }
.total > span:first-child { font-weight: 600; }
.total-num { font-family: var(--font-display); font-size: 32px; font-weight: 600; color: var(--pine); }

.rx-warn {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 11px 13px; margin-bottom: var(--sp-4);
  background: var(--clay-soft); border-radius: var(--r-md);
  font-size: 12.5px; color: #8f3e23; line-height: 1.5;
}
.rx-warn svg { flex-shrink: 0; margin-top: 1px; }
.rx-warn-link { color: var(--clay); font-weight: 700; cursor: pointer; }
.rx-warn-link:hover { text-decoration: underline; }
.tip { text-align: center; font-size: 12.5px; color: var(--ink-faint); margin-top: 12px; }

@media (max-width: 880px) {
  .layout { grid-template-columns: 1fr; }
  .summary { position: static; }
  .item { grid-template-columns: 52px 1fr auto; grid-template-areas: 'thumb main del' 'step step sum'; row-gap: 12px; }
  .i-thumb { grid-area: thumb; width: 52px; height: 52px; }
  .i-main { grid-area: main; }
  .i-del { grid-area: del; }
  .stepper { grid-area: step; }
  .i-sum { grid-area: sum; }
}
</style>

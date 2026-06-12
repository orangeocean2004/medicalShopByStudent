<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { adminApi, pharmacistApi } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import BaseButton from '@/components/ui/BaseButton.vue'

const toast = useToastStore()

const tab = ref('drugs') // drugs | orders

// ---------- 在线药师监测（轮询）----------
const pharmacistOnline = ref(false)
const pharmacistCount = ref(0)
let onlineTimer = null

async function refreshPharmacistOnline() {
  try {
    const res = await pharmacistApi.onlineStatus()
    pharmacistOnline.value = !!res.online
    pharmacistCount.value = res.count || 0
  } catch {
    pharmacistOnline.value = false
    pharmacistCount.value = 0
  }
}

// ---------- 药品 ----------
const drugs = ref([])
const drugsLoading = ref(true)
const editingStock = ref({}) // id -> 临时库存值
const savingStock = ref({})

async function loadDrugs() {
  drugsLoading.value = true
  try {
    drugs.value = await adminApi.listDrugs()
  } catch (e) {
    toast.error(e.message)
  } finally {
    drugsLoading.value = false
  }
}

async function saveStock(d) {
  const val = Number(editingStock.value[d.id])
  if (!Number.isInteger(val) || val < 0) {
    toast.error('请输入合法的库存数量')
    return
  }
  savingStock.value[d.id] = true
  try {
    await adminApi.setStock(d.id, val)
    d.stock = val
    delete editingStock.value[d.id]
    toast.success(`「${d.name}」库存已更新为 ${val}`)
  } catch (e) {
    toast.error(e.message)
  } finally {
    savingStock.value[d.id] = false
  }
}

const lowStockCount = computed(() => drugs.value.filter((d) => d.stock <= 60).length)

// ---------- 新增药品 ----------
const showAdd = ref(false)
const form = ref(blankForm())
const adding = ref(false)
function blankForm() {
  return { name: '', category: '', isRx: 0, price: null, stock: 0, indication: '', dosage: '', contraindication: '' }
}
async function submitAdd() {
  if (!form.value.name || !form.value.category || form.value.price == null) {
    toast.error('请填写药名、分类与价格')
    return
  }
  adding.value = true
  try {
    const created = await adminApi.addDrug(form.value)
    drugs.value.push(created)
    toast.success(`已上架「${created.name}」`)
    showAdd.value = false
    form.value = blankForm()
  } catch (e) {
    toast.error(e.message)
  } finally {
    adding.value = false
  }
}

// ---------- 订单 ----------
const orders = ref([])
const ordersLoading = ref(false)
const STATUS = {
  0: { label: '待支付', cls: 'pending' },
  1: { label: '已支付', cls: 'paid' },
  2: { label: '配送中', cls: 'shipping' },
  3: { label: '已完成', cls: 'done' },
  4: { label: '已取消', cls: 'cancelled' }
}
// 允许的下一步流转
const NEXT = {
  0: [{ s: 1, label: '确认支付' }, { s: 4, label: '取消' }],
  1: [{ s: 2, label: '发货' }, { s: 4, label: '取消' }],
  2: [{ s: 3, label: '完成' }],
  3: [],
  4: []
}

async function loadOrders() {
  ordersLoading.value = true
  try {
    orders.value = await adminApi.listOrders()
  } catch (e) {
    toast.error(e.message)
  } finally {
    ordersLoading.value = false
  }
}

async function changeStatus(o, s) {
  try {
    await adminApi.setOrderStatus(o.orderId, s)
    o.status = s
    toast.success(`订单 #${o.orderId} 状态已更新`)
    // 发货后刷新以拿到物流号
    if (s === 2) loadOrders()
  } catch (e) {
    toast.error(e.message)
  }
}

function switchTab(t) {
  tab.value = t
  if (t === 'orders' && !orders.value.length) loadOrders()
  if (t === 'users' && !users.value.length) loadUsers()
}

// ---------- 人员管理 ----------
const users = ref([])
const usersLoading = ref(false)
const ROLE_TAG = {
  0: { label: '消费者', cls: 'r-consumer' },
  1: { label: '执业药师', cls: 'r-pharmacist' },
  2: { label: '客服', cls: 'r-staff' },
  3: { label: '运营', cls: 'r-operator' }
}

async function loadUsers() {
  usersLoading.value = true
  try {
    users.value = await adminApi.listUsers()
  } catch (e) {
    toast.error(e.message)
  } finally {
    usersLoading.value = false
  }
}

const showAddStaff = ref(false)
const staffForm = ref(blankStaff())
const addingStaff = ref(false)
function blankStaff() {
  return { phone: '', nickname: '', password: '', role: 1 }
}
async function submitStaff() {
  if (!/^\d{11}$/.test(staffForm.value.phone)) {
    toast.error('请填写 11 位手机号')
    return
  }
  if ((staffForm.value.password || '').length < 6) {
    toast.error('初始密码至少 6 位')
    return
  }
  addingStaff.value = true
  try {
    const created = await adminApi.createStaff(staffForm.value)
    users.value.unshift(created)
    toast.success(`已创建${created.roleName}「${created.nickname}」`)
    showAddStaff.value = false
    staffForm.value = blankStaff()
  } catch (e) {
    toast.error(e.message)
  } finally {
    addingStaff.value = false
  }
}

onMounted(() => {
  loadDrugs()
  // 在线药师监测：立即查一次 + 每 30 秒轮询
  refreshPharmacistOnline()
  onlineTimer = setInterval(refreshPharmacistOnline, 30000)
})

onUnmounted(() => {
  if (onlineTimer) clearInterval(onlineTimer)
})
</script>

<template>
  <div class="admin container">
    <header class="a-head rise">
      <div>
        <p class="overline">OPERATIONS CONSOLE</p>
        <h1>管理后台</h1>
      </div>
      <div class="a-stats">
        <div class="stat online" :class="pharmacistOnline ? 'on' : 'off'">
          <span class="stat-num">
            <span class="dot" :class="{ off: !pharmacistOnline }"></span>{{ pharmacistOnline ? pharmacistCount : 0 }}
          </span>
          <span class="stat-label">在线药师</span>
        </div>
        <div class="stat">
          <span class="stat-num">{{ drugs.length }}</span>
          <span class="stat-label">在售药品</span>
        </div>
        <div class="stat" :class="{ warn: lowStockCount }">
          <span class="stat-num">{{ lowStockCount }}</span>
          <span class="stat-label">库存偏低</span>
        </div>
      </div>
    </header>

    <!-- 标签页 -->
    <div class="tabs rise" style="animation-delay: 60ms">
      <button :class="{ on: tab === 'drugs' }" @click="switchTab('drugs')">药品库存</button>
      <button :class="{ on: tab === 'orders' }" @click="switchTab('orders')">订单管理</button>
      <button :class="{ on: tab === 'users' }" @click="switchTab('users')">人员管理</button>
    </div>

    <!-- 药品库存 -->
    <section v-show="tab === 'drugs'" class="panel rise" style="animation-delay: 120ms">
      <div class="panel-bar">
        <span class="panel-title">药品库存管理</span>
        <BaseButton size="sm" @click="showAdd = !showAdd">{{ showAdd ? '收起' : '+ 上架新药品' }}</BaseButton>
      </div>

      <!-- 新增表单 -->
      <transition name="slide">
        <div v-if="showAdd" class="add-form">
          <div class="af-grid">
            <label>药品名称<input v-model="form.name" placeholder="如 板蓝根颗粒" /></label>
            <label>分类<input v-model="form.category" placeholder="如 感冒用药" /></label>
            <label>价格(元)<input v-model.number="form.price" type="number" min="0" step="0.01" placeholder="0.00" /></label>
            <label>初始库存<input v-model.number="form.stock" type="number" min="0" placeholder="0" /></label>
            <label>类型
              <select v-model.number="form.isRx">
                <option :value="0">OTC 非处方</option>
                <option :value="1">Rx 处方药</option>
              </select>
            </label>
          </div>
          <label class="af-wide">适应症<input v-model="form.indication" placeholder="用于…" /></label>
          <label class="af-wide">用法用量<input v-model="form.dosage" placeholder="成人一次…" /></label>
          <label class="af-wide">禁忌<input v-model="form.contraindication" placeholder="…者禁用" /></label>
          <BaseButton :loading="adding" @click="submitAdd">确认上架</BaseButton>
        </div>
      </transition>

      <div v-if="drugsLoading" class="state">加载中…</div>
      <table v-else class="grid">
        <thead>
          <tr><th>ID</th><th>药品</th><th>分类</th><th>类型</th><th>价格</th><th>库存</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="d in drugs" :key="d.id" :class="{ low: d.stock <= 60 }">
            <td class="muted">{{ d.id }}</td>
            <td class="strong">{{ d.name }}</td>
            <td>{{ d.category }}</td>
            <td><span class="tag" :class="d.isRx ? 'rx' : 'otc'">{{ d.isRx ? 'Rx' : 'OTC' }}</span></td>
            <td>¥{{ d.price }}</td>
            <td>
              <span class="stock" :class="{ alarm: d.stock <= 60 }">{{ d.stock }}</span>
            </td>
            <td>
              <div class="stock-edit">
                <input
                  type="number" min="0"
                  :placeholder="String(d.stock)"
                  v-model="editingStock[d.id]"
                />
                <button
                  class="mini"
                  :disabled="editingStock[d.id] == null || editingStock[d.id] === '' || savingStock[d.id]"
                  @click="saveStock(d)"
                >改库存</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 订单管理 -->
    <section v-show="tab === 'orders'" class="panel rise">
      <div class="panel-bar"><span class="panel-title">全部订单</span></div>
      <div v-if="ordersLoading" class="state">加载中…</div>
      <div v-else-if="!orders.length" class="state">暂无订单</div>
      <table v-else class="grid">
        <thead>
          <tr><th>订单号</th><th>用户ID</th><th>金额</th><th>状态</th><th>物流单号</th><th>下单时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.orderId">
            <td class="strong">#{{ o.orderId }}</td>
            <td class="muted">{{ o.userId }}</td>
            <td>¥{{ o.totalAmount }}</td>
            <td><span class="oc-status" :class="STATUS[o.status]?.cls">{{ STATUS[o.status]?.label }}</span></td>
            <td class="mono">{{ o.logisticsNo || '—' }}</td>
            <td class="muted">{{ o.createdAt }}</td>
            <td>
              <div class="ops">
                <button
                  v-for="n in NEXT[o.status]"
                  :key="n.s"
                  class="mini"
                  :class="{ danger: n.s === 4 }"
                  @click="changeStatus(o, n.s)"
                >{{ n.label }}</button>
                <span v-if="!NEXT[o.status]?.length" class="muted">—</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 人员管理 -->
    <section v-show="tab === 'users'" class="panel rise">
      <div class="panel-bar">
        <span class="panel-title">人员管理</span>
        <BaseButton size="sm" @click="showAddStaff = !showAddStaff">{{ showAddStaff ? '收起' : '+ 新增员工' }}</BaseButton>
      </div>

      <!-- 新增员工表单 -->
      <transition name="slide">
        <div v-if="showAddStaff" class="add-form">
          <div class="af-grid">
            <label>手机号<input v-model="staffForm.phone" maxlength="11" placeholder="11 位手机号" /></label>
            <label>昵称<input v-model="staffForm.nickname" placeholder="可选，留空自动生成" /></label>
            <label>初始密码<input v-model="staffForm.password" placeholder="至少 6 位" /></label>
            <label>角色
              <select v-model.number="staffForm.role">
                <option :value="1">执业药师</option>
                <option :value="2">客服</option>
                <option :value="3">运营</option>
              </select>
            </label>
          </div>
          <BaseButton :loading="addingStaff" @click="submitStaff">确认创建</BaseButton>
        </div>
      </transition>

      <div v-if="usersLoading" class="state">加载中…</div>
      <table v-else class="grid">
        <thead>
          <tr><th>ID</th><th>昵称</th><th>手机号</th><th>角色</th><th>注册时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td class="muted">{{ u.id }}</td>
            <td class="strong">{{ u.nickname }}</td>
            <td class="mono">{{ u.phone }}</td>
            <td><span class="tag" :class="ROLE_TAG[u.role]?.cls">{{ ROLE_TAG[u.role]?.label || '用户' }}</span></td>
            <td class="muted">{{ u.createdAt || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.admin { padding-top: var(--sp-6); max-width: 1100px; }
.a-head { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--sp-5); margin-bottom: var(--sp-5); }
.a-head h1 { font-size: 36px; margin-top: 6px; }
.a-stats { display: flex; gap: var(--sp-3); }
.stat {
  display: flex; flex-direction: column; align-items: center;
  padding: 10px 22px; background: var(--surface);
  border: 1px solid var(--line); border-radius: var(--r-md);
}
.stat.warn { border-color: var(--clay); background: var(--clay-soft); }
.stat.online.on { border-color: var(--success); background: var(--pine-soft); }
.stat.online .stat-num { display: inline-flex; align-items: center; gap: 8px; }
.stat.online.on .stat-num { color: var(--pine); }
.stat.online.off .stat-num { color: var(--ink-faint); }
.stat.online .dot { width: 9px; height: 9px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 3px rgba(63,125,82,0.2); }
.stat.online .dot.off { background: var(--ink-faint); box-shadow: 0 0 0 3px rgba(120,120,120,0.14); }
.stat-num { font-family: var(--font-display); font-size: 28px; font-weight: 600; color: var(--pine); }
.stat.warn .stat-num { color: var(--clay); }
.stat-label { font-size: 12px; color: var(--ink-faint); margin-top: 2px; }

/* tabs */
.tabs { display: flex; gap: 4px; margin-bottom: var(--sp-4); border-bottom: 1px solid var(--line); }
.tabs button {
  padding: 11px 22px; font-size: 15px; font-weight: 600; color: var(--ink-soft);
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  transition: color var(--t-fast), border-color var(--t-fast);
}
.tabs button:hover { color: var(--pine); }
.tabs button.on { color: var(--pine); border-bottom-color: var(--pine); }

/* panel */
.panel { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg); overflow: hidden; }
.panel-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--sp-4) var(--sp-5); border-bottom: 1px solid var(--line);
  background: var(--paper-2);
}
.panel-title { font-family: var(--font-display); font-size: 18px; font-weight: 600; }
.state { padding: var(--sp-8); text-align: center; color: var(--ink-faint); }

/* 表格 */
.grid { width: 100%; border-collapse: collapse; font-size: 14px; }
.grid th {
  text-align: left; padding: 12px var(--sp-4);
  font-size: 11px; font-weight: 700; letter-spacing: 0.06em; color: var(--ink-faint);
  text-transform: uppercase; border-bottom: 1px solid var(--line); background: var(--surface);
}
.grid td { padding: 12px var(--sp-4); border-bottom: 1px solid var(--line); vertical-align: middle; }
.grid tbody tr:hover { background: var(--paper-2); }
.grid tr.low td { background: rgba(200, 100, 60, 0.05); }
.strong { font-weight: 600; }
.muted { color: var(--ink-faint); }
.mono { font-family: var(--font-mono); font-size: 12.5px; letter-spacing: 0.02em; }

.tag { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: var(--r-sm); }
.tag.otc { background: var(--pine-soft); color: var(--pine); }
.tag.rx { background: var(--clay-soft); color: #8f3e23; }
.tag.r-consumer { background: var(--paper-2); color: var(--ink-soft); }
.tag.r-pharmacist { background: var(--pine-soft); color: var(--pine); }
.tag.r-staff { background: #e7eef6; color: #345b86; }
.tag.r-operator { background: var(--clay-soft); color: #8f3e23; }

.stock { font-weight: 600; }
.stock.alarm { color: var(--clay); }

.stock-edit { display: flex; gap: 6px; align-items: center; }
.stock-edit input { width: 72px; padding: 6px 8px; border: 1px solid var(--line); border-radius: var(--r-sm); font-size: 13px; }
.stock-edit input:focus { outline: none; border-color: var(--pine); }

.mini {
  padding: 6px 12px; font-size: 12.5px; font-weight: 600;
  background: var(--pine); color: var(--paper); border-radius: var(--r-sm);
  transition: background var(--t-fast), opacity var(--t-fast);
}
.mini:hover:not(:disabled) { background: var(--pine-deep); }
.mini:disabled { opacity: 0.4; cursor: not-allowed; }
.mini.danger { background: transparent; color: var(--danger); border: 1px solid var(--line-strong); }
.mini.danger:hover { background: var(--clay-soft); border-color: var(--danger); }

.ops { display: flex; gap: 6px; flex-wrap: wrap; }

.oc-status { font-size: 12px; font-weight: 700; padding: 3px 11px; border-radius: var(--r-pill); }
.oc-status.pending { background: var(--clay-soft); color: #8f3e23; }
.oc-status.paid { background: #e7eef6; color: #345b86; }
.oc-status.shipping { background: #fbf0d8; color: #8a6516; }
.oc-status.done { background: var(--pine-soft); color: var(--pine); }
.oc-status.cancelled { background: var(--paper-2); color: var(--ink-faint); }

/* 新增表单 */
.add-form { padding: var(--sp-5); border-bottom: 1px solid var(--line); background: var(--paper-2); }
.af-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: var(--sp-3); }
.add-form label { display: flex; flex-direction: column; gap: 5px; font-size: 12px; font-weight: 600; color: var(--ink-soft); margin-bottom: var(--sp-3); }
.af-wide { width: 100%; }
.add-form input, .add-form select {
  padding: 9px 11px; border: 1px solid var(--line); border-radius: var(--r-sm);
  font-size: 14px; background: var(--surface); font-weight: 400; color: var(--ink);
}
.add-form input:focus, .add-form select:focus { outline: none; border-color: var(--pine); }

.slide-enter-active, .slide-leave-active { transition: all var(--t-mid) var(--ease); overflow: hidden; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { max-height: 600px; }

@media (max-width: 760px) {
  .a-head { flex-direction: column; align-items: flex-start; }
  .grid { font-size: 12.5px; }
  .grid th:nth-child(3), .grid td:nth-child(3) { display: none; }
}
</style>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { drugApi } from '@/api/shop'
import { pharmacistApi, adminApi } from '@/api/admin'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import DrugCard from '@/components/DrugCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()
const toast = useToastStore()

const keyword = ref('')
const featured = ref([])
const loading = ref(true)

const quickTags = ['感冒', '布洛芬', '维生素', '止咳', '过敏']

// ===== 药师仪表盘 =====
const isPharmacist = computed(() => auth.isPharmacist)
const stats = ref(null)
const statsLoading = ref(true)

// 问候语随时间变化
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 处方审核通过率（通过 / 已审），用于环形图
const rxApproveRate = computed(() => {
  if (!stats.value || !stats.value.rxTotal) return 0
  return Math.round((stats.value.rxApproved / stats.value.rxTotal) * 100)
})

// 柱状图数据：四类工作量，取最大值做归一化
const barData = computed(() => {
  if (!stats.value) return []
  const s = stats.value
  const items = [
    { label: '工单处理中', value: s.ticketDoing, cls: 'b-doing' },
    { label: '工单已完成', value: s.ticketDone, cls: 'b-done' },
    { label: '处方通过', value: s.rxApproved, cls: 'b-approve' },
    { label: '处方驳回', value: s.rxRejected, cls: 'b-reject' }
  ]
  const max = Math.max(1, ...items.map((i) => i.value))
  return items.map((i) => ({ ...i, pct: Math.round((i.value / max) * 100) }))
})

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await pharmacistApi.stats()
  } catch (e) {
    toast.error(e.message)
  } finally {
    statsLoading.value = false
  }
}

// ===== 运营仪表盘 =====
const isOperator = computed(() => auth.isOperator)
const adminStats = ref(null)
const adminLoading = ref(true)

// 订单状态分布柱状图
const orderBars = computed(() => {
  if (!adminStats.value) return []
  const s = adminStats.value
  const items = [
    { label: '待支付', value: s.orderPending, cls: 'b-pending' },
    { label: '已支付', value: s.orderPaid, cls: 'b-paid' },
    { label: '配送中', value: s.orderShipping, cls: 'b-shipping' },
    { label: '已完成', value: s.orderDone, cls: 'b-done' },
    { label: '已取消', value: s.orderCancelled, cls: 'b-cancelled' }
  ]
  const max = Math.max(1, ...items.map((i) => i.value))
  return items.map((i) => ({ ...i, pct: Math.round((i.value / max) * 100) }))
})

// 用户角色分布（环形图分段）
const userSegments = computed(() => {
  if (!adminStats.value) return []
  const s = adminStats.value
  const total = Math.max(1, s.userTotal)
  const segs = [
    { label: '消费者', value: s.userConsumers, cls: 'consumer' },
    { label: '药师', value: s.userPharmacists, cls: 'pharmacist' },
    { label: '客服/运营', value: s.userStaff, cls: 'staff' }
  ]
  let offset = 0
  return segs.map((seg) => {
    const pct = (seg.value / total) * 100
    const out = { ...seg, pct, dash: pct * 3.14159, offset: -offset * 3.14159 }
    offset += pct
    return out
  })
})

async function loadAdminStats() {
  adminLoading.value = true
  try {
    adminStats.value = await adminApi.stats()
  } catch (e) {
    toast.error(e.message)
  } finally {
    adminLoading.value = false
  }
}

function doSearch(kw) {
  const q = kw ?? keyword.value
  if (!q || !q.trim()) {
    toast.info('试试搜索「感冒」或「布洛芬」')
    return
  }
  router.push({ name: 'drugs', query: { q: q.trim() } })
}

function addToCart(drug) {
  cart.add(drug)
  toast.success(`已加入「${drug.name}」`)
}

onMounted(async () => {
  if (isOperator.value) {
    loadAdminStats()
    return
  }
  if (isPharmacist.value) {
    loadStats()
    return
  }
  try {
    // 用一个常见词拉一批药品作为「精选」展示
    const res = await drugApi.search('维生素', 1, 3)
    let list = res.list || []
    if (list.length < 3) {
      const more = await drugApi.search('感冒', 1, 3)
      list = [...list, ...(more.list || [])].slice(0, 3)
    }
    featured.value = list
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="home">
    <!-- ============ 运营管理仪表盘 ============ -->
    <template v-if="isOperator">
      <section class="dash container">
        <header class="dash-head rise">
          <div>
            <p class="overline">OPERATIONS DASHBOARD</p>
            <h1>{{ greeting }}，运营管理员</h1>
            <p class="dash-lead">松和堂经营概览。用户、订单、营收与库存，一眼掌握。</p>
          </div>
          <BaseButton variant="line" size="md" @click="router.push({ name: 'admin' })">进入管理后台</BaseButton>
        </header>

        <div v-if="adminLoading" class="state">正在加载经营数据…</div>

        <template v-else-if="adminStats">
          <!-- KPI -->
          <div class="kpis kpis-4 rise" style="animation-delay: 60ms">
            <div class="kpi">
              <span class="kpi-num">¥{{ adminStats.revenue }}</span>
              <span class="kpi-label">累计营收</span>
              <span class="kpi-sub">已支付及以后订单</span>
            </div>
            <div class="kpi">
              <span class="kpi-num">{{ adminStats.orderTotal }}</span>
              <span class="kpi-label">订单总数</span>
              <span class="kpi-sub">完成 {{ adminStats.orderDone }} · 待支付 {{ adminStats.orderPending }}</span>
            </div>
            <div class="kpi">
              <span class="kpi-num">{{ adminStats.userTotal }}</span>
              <span class="kpi-label">注册用户</span>
              <span class="kpi-sub">消费者 {{ adminStats.userConsumers }} · 员工 {{ adminStats.userPharmacists + adminStats.userStaff }}</span>
            </div>
            <div class="kpi" :class="{ warn: adminStats.drugLowStock }">
              <span class="kpi-num">{{ adminStats.drugTotal }}</span>
              <span class="kpi-label">在售药品</span>
              <span class="kpi-sub">{{ adminStats.drugLowStock }} 项库存偏低</span>
            </div>
          </div>

          <!-- 图表 -->
          <div class="charts rise" style="animation-delay: 120ms">
            <!-- 订单状态分布 -->
            <section class="chart-card">
              <h2 class="chart-title">订单状态分布</h2>
              <div class="bars">
                <div v-for="b in orderBars" :key="b.label" class="bar-row">
                  <span class="bar-label">{{ b.label }}</span>
                  <div class="bar-track">
                    <div class="bar-fill" :class="b.cls" :style="{ width: b.pct + '%' }"></div>
                  </div>
                  <span class="bar-val">{{ b.value }}</span>
                </div>
              </div>
            </section>

            <!-- 用户角色分布环形图 -->
            <section class="chart-card donut-card">
              <h2 class="chart-title">用户构成</h2>
              <div class="donut-wrap">
                <svg viewBox="0 0 120 120" width="150" height="150" class="donut">
                  <circle cx="60" cy="60" r="50" class="donut-bg" />
                  <circle
                    v-for="seg in userSegments"
                    :key="seg.label"
                    cx="60" cy="60" r="50"
                    class="donut-seg"
                    :class="seg.cls"
                    :stroke-dasharray="`${seg.dash} 314.159`"
                    :stroke-dashoffset="seg.offset"
                    transform="rotate(-90 60 60)"
                  />
                  <text x="60" y="56" class="donut-pct">{{ adminStats.userTotal }}</text>
                  <text x="60" y="76" class="donut-cap">总用户</text>
                </svg>
                <div class="donut-legend col">
                  <span class="lg-item"><i class="lg-dot u-consumer"></i>消费者 {{ adminStats.userConsumers }}</span>
                  <span class="lg-item"><i class="lg-dot u-pharmacist"></i>药师 {{ adminStats.userPharmacists }}</span>
                  <span class="lg-item"><i class="lg-dot u-staff"></i>客服/运营 {{ adminStats.userStaff }}</span>
                </div>
              </div>
            </section>
          </div>

          <!-- 快捷入口 -->
          <div class="shortcuts shortcuts-3 rise" style="animation-delay: 180ms">
            <button class="sc" @click="router.push({ name: 'admin' })">
              <div class="sc-icon pine">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="sc-text">
                <span class="sc-title">药品库存</span>
                <span class="sc-sub">{{ adminStats.drugTotal }} 项 · {{ adminStats.drugLowStock }} 项偏低</span>
              </div>
              <svg class="sc-arrow" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button class="sc" @click="router.push({ name: 'admin' })">
              <div class="sc-icon clay">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4zM3 6h18M16 10a4 4 0 0 1-8 0" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="sc-text">
                <span class="sc-title">订单管理</span>
                <span class="sc-sub">{{ adminStats.orderTotal }} 笔 · {{ adminStats.orderPending }} 待支付</span>
              </div>
              <svg class="sc-arrow" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button class="sc" @click="router.push({ name: 'admin' })">
              <div class="sc-icon slate">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="sc-text">
                <span class="sc-title">人员管理</span>
                <span class="sc-sub">{{ adminStats.userTotal }} 个账号 · 新增员工</span>
              </div>
              <svg class="sc-arrow" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </div>
        </template>
      </section>
    </template>

    <!-- ============ 药师员工仪表盘 ============ -->
    <template v-else-if="isPharmacist">
      <section class="dash container">
        <header class="dash-head rise">
          <div>
            <p class="overline">PHARMACIST DASHBOARD</p>
            <h1>{{ greeting }}，{{ auth.user?.phone }}</h1>
            <p class="dash-lead">这是你的工作概览。今天也辛苦了，稳一点，准一点。</p>
          </div>
          <div class="dash-pending" v-if="stats">
            <div class="dp-item" :class="{ hot: stats.pendingTickets }">
              <span class="dp-num">{{ stats.pendingTickets }}</span>
              <span class="dp-label">待接工单</span>
            </div>
            <div class="dp-item" :class="{ hot: stats.pendingRx }">
              <span class="dp-num">{{ stats.pendingRx }}</span>
              <span class="dp-label">待审处方</span>
            </div>
          </div>
        </header>

        <div v-if="statsLoading" class="state">正在加载工作概览…</div>

        <template v-else-if="stats">
          <!-- 工作量数字卡 -->
          <div class="kpis rise" style="animation-delay: 60ms">
            <div class="kpi">
              <span class="kpi-num">{{ stats.ticketTotal }}</span>
              <span class="kpi-label">累计接手工单</span>
              <span class="kpi-sub">处理中 {{ stats.ticketDoing }} · 已完成 {{ stats.ticketDone }}</span>
            </div>
            <div class="kpi">
              <span class="kpi-num">{{ stats.rxTotal }}</span>
              <span class="kpi-label">累计审核处方</span>
              <span class="kpi-sub">通过 {{ stats.rxApproved }} · 驳回 {{ stats.rxRejected }}</span>
            </div>
            <div class="kpi">
              <span class="kpi-num">{{ stats.ticketDone + stats.rxTotal }}</span>
              <span class="kpi-label">累计完成事项</span>
              <span class="kpi-sub">工单 + 处方</span>
            </div>
          </div>

          <!-- 图表区 -->
          <div class="charts rise" style="animation-delay: 120ms">
            <!-- 柱状图：工作量分布 -->
            <section class="chart-card">
              <h2 class="chart-title">工作量分布</h2>
              <div class="bars">
                <div v-for="b in barData" :key="b.label" class="bar-row">
                  <span class="bar-label">{{ b.label }}</span>
                  <div class="bar-track">
                    <div class="bar-fill" :class="b.cls" :style="{ width: b.pct + '%' }"></div>
                  </div>
                  <span class="bar-val">{{ b.value }}</span>
                </div>
              </div>
            </section>

            <!-- 环形图：处方通过率 -->
            <section class="chart-card donut-card">
              <h2 class="chart-title">处方通过率</h2>
              <div class="donut-wrap">
                <svg viewBox="0 0 120 120" width="150" height="150" class="donut">
                  <circle cx="60" cy="60" r="50" class="donut-bg" />
                  <circle
                    cx="60" cy="60" r="50"
                    class="donut-fg"
                    :stroke-dasharray="`${rxApproveRate * 3.14159} 314.159`"
                    transform="rotate(-90 60 60)"
                  />
                  <text x="60" y="56" class="donut-pct">{{ rxApproveRate }}%</text>
                  <text x="60" y="76" class="donut-cap">通过率</text>
                </svg>
                <div class="donut-legend">
                  <span class="lg-item"><i class="lg-dot approve"></i>通过 {{ stats.rxApproved }}</span>
                  <span class="lg-item"><i class="lg-dot reject"></i>驳回 {{ stats.rxRejected }}</span>
                </div>
              </div>
            </section>
          </div>

          <!-- 快捷入口 -->
          <div class="shortcuts rise" style="animation-delay: 180ms">
            <button class="sc" @click="router.push({ name: 'workbench-handoff' })">
              <div class="sc-icon pine">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="sc-text">
                <span class="sc-title">转人工工单</span>
                <span class="sc-sub">{{ stats.pendingTickets }} 个待接 · 与用户实时对话</span>
              </div>
              <svg class="sc-arrow" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button class="sc" @click="router.push({ name: 'workbench-rx' })">
              <div class="sc-icon clay">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 2v6h6M9 13l2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <div class="sc-text">
                <span class="sc-title">处方审核</span>
                <span class="sc-sub">{{ stats.pendingRx }} 张待审 · 核验真伪与有效性</span>
              </div>
              <svg class="sc-arrow" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </div>
        </template>
      </section>
    </template>

    <!-- ============ 消费者首页 ============ -->
    <template v-else>
    <!-- HERO -->
    <section class="hero container">
      <div class="hero-copy">
        <p class="overline rise">道地药材 · 执业药师 · 智能问询</p>
        <h1 class="hero-title rise" style="animation-delay: 80ms">
          需要的药，<br />和<em>懂药</em>的人。
        </h1>
        <p class="hero-desc rise" style="animation-delay: 160ms">
          在松和堂，每一次购药都有迹可循：智能药师先答，拿不准就转真人，<br />
          处方药从不含糊。慢一点，稳一点。
        </p>

        <div class="search rise" style="animation-delay: 240ms">
          <div class="search-box">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" class="s-ic"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2" stroke-linecap="round"/></svg>
            <input
              v-model="keyword"
              placeholder="搜索药品名、症状或适应症…"
              @keyup.enter="doSearch()"
            />
            <BaseButton size="md" @click="doSearch()">搜索</BaseButton>
          </div>
          <div class="tags">
            <span class="tags-label">热门</span>
            <button v-for="t in quickTags" :key="t" class="tag" @click="doSearch(t)">{{ t }}</button>
          </div>
        </div>
      </div>

      <!-- 装饰性「药匣」卡片堆叠 -->
      <div class="hero-art rise" style="animation-delay: 200ms" aria-hidden="true">
        <div class="vial v1"></div>
        <div class="vial v2"></div>
        <div class="card-float">
          <span class="cf-tag">智能药师 · 在线</span>
          <p class="cf-q">"成人感冒发烧吃什么？"</p>
          <div class="cf-meter">
            <div class="cf-bar"><span></span></div>
            <span class="cf-pct">置信 95%</span>
          </div>
          <p class="cf-a">建议布洛芬或对乙酰氨基酚，单一成分用药，多休息多饮水…</p>
        </div>
      </div>
    </section>

    <!-- 三大保障 -->
    <section class="pillars container">
      <div class="pillar">
        <span class="p-num">①</span>
        <h3>有据可循</h3>
        <p>每条建议都标注知识来源与置信度，不臆造、可追溯。</p>
      </div>
      <div class="pillar">
        <span class="p-num">②</span>
        <h3>真人兜底</h3>
        <p>低置信或高风险问题，一键转接执业药师，带着对话上下文。</p>
      </div>
      <div class="pillar">
        <span class="p-num">③</span>
        <h3>处方严管</h3>
        <p>处方药下单前严格拦截校验，把安全放在便捷之前。</p>
      </div>
    </section>

    <!-- 精选药品 -->
    <section class="featured container">
      <div class="sec-head">
        <div>
          <p class="overline">SELECTED</p>
          <h2>药师精选</h2>
        </div>
        <router-link :to="{ name: 'drugs', query: { q: '维生素' } }" class="more">
          查看全部
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </router-link>
      </div>

      <div v-if="loading" class="grid">
        <div v-for="i in 3" :key="i" class="skeleton"></div>
      </div>
      <div v-else-if="featured.length" class="grid">
        <DrugCard
          v-for="(d, i) in featured"
          :key="d.id"
          :drug="d"
          :index="i"
          @add="addToCart"
        />
      </div>
      <p v-else class="empty">暂无精选药品，去<router-link :to="{ name: 'drugs', query: { q: '感冒' } }">搜索</router-link>看看。</p>
    </section>

    <!-- AI 引导条 -->
    <section class="cta container">
      <div class="cta-inner">
        <div class="cta-text">
          <p class="overline" style="color: #e9b48a">SMART PHARMACIST</p>
          <h2>拿不准吃什么药？<br />先问问智能药师。</h2>
        </div>
        <BaseButton variant="clay" size="lg" @click="router.push({ name: 'consult' })">
          开始咨询
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-6-6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </BaseButton>
      </div>
    </section>
    </template>
  </div>
</template>

<style scoped>
/* ===== 药师仪表盘 ===== */
.dash { padding-top: var(--sp-7); padding-bottom: var(--sp-8); max-width: 1000px; }
.dash-head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--sp-5); margin-bottom: var(--sp-6); }
.dash-head h1 { font-size: clamp(28px, 4vw, 42px); margin-top: 6px; }
.dash-lead { color: var(--ink-soft); font-size: 15px; margin-top: var(--sp-3); }
.dash-pending { display: flex; gap: var(--sp-3); flex-shrink: 0; }
.dp-item {
  display: flex; flex-direction: column; align-items: center;
  padding: 12px 22px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-md);
}
.dp-item.hot { border-color: var(--clay); background: var(--clay-soft); }
.dp-num { font-family: var(--font-display); font-size: 28px; font-weight: 600; color: var(--ink-faint); }
.dp-item.hot .dp-num { color: var(--clay); }
.dp-label { font-size: 12px; color: var(--ink-faint); margin-top: 2px; }

.kpis { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--sp-4); margin-bottom: var(--sp-5); }
.kpis-4 { grid-template-columns: repeat(4, 1fr); }
.kpis-4 .kpi-num { font-size: 30px; word-break: break-all; }
.kpi {
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  padding: var(--sp-5); display: flex; flex-direction: column;
}
.kpi.warn { border-color: var(--clay); background: var(--clay-soft); }
.kpi-num { font-family: var(--font-display); font-size: 40px; font-weight: 600; color: var(--pine); line-height: 1; }
.kpi.warn .kpi-num { color: var(--clay); }
.kpi-label { font-size: 15px; font-weight: 600; margin-top: var(--sp-3); }
.kpi-sub { font-size: 12.5px; color: var(--ink-faint); margin-top: 4px; }

.charts { display: grid; grid-template-columns: 1.4fr 1fr; gap: var(--sp-4); margin-bottom: var(--sp-5); }
.chart-card {
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  padding: var(--sp-6);
}
.chart-title { font-size: 17px; margin-bottom: var(--sp-5); }

/* 柱状图 */
.bars { display: flex; flex-direction: column; gap: var(--sp-4); }
.bar-row { display: grid; grid-template-columns: 88px 1fr 32px; align-items: center; gap: var(--sp-3); }
.bar-label { font-size: 13px; color: var(--ink-soft); }
.bar-track { height: 12px; background: var(--paper-2); border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; transition: width 0.8s var(--ease-out); min-width: 2px; }
.bar-fill.b-doing { background: #d8a93a; }
.bar-fill.b-done { background: var(--pine); }
.bar-fill.b-approve { background: var(--success, #3f7d52); }
.bar-fill.b-reject { background: var(--clay); }
.bar-fill.b-pending { background: #c8623c; }
.bar-fill.b-paid { background: #5b86b3; }
.bar-fill.b-shipping { background: #d8a93a; }
.bar-fill.b-cancelled { background: var(--ink-faint); }
.bar-val { font-family: var(--font-mono); font-size: 13px; font-weight: 600; text-align: right; }

/* 环形图 */
.donut-card { display: flex; flex-direction: column; }
.donut-wrap { display: flex; flex-direction: column; align-items: center; gap: var(--sp-4); margin: auto 0; }
.donut-bg { fill: none; stroke: var(--paper-2); stroke-width: 12; }
.donut-fg { fill: none; stroke: var(--pine); stroke-width: 12; stroke-linecap: round; transition: stroke-dasharray 0.9s var(--ease-out); }
.donut-seg { fill: none; stroke-width: 12; transition: stroke-dasharray 0.9s var(--ease-out); }
.donut-seg.consumer { stroke: var(--pine); }
.donut-seg.pharmacist { stroke: #d8a93a; }
.donut-seg.staff { stroke: var(--clay); }
.donut-pct { font-family: var(--font-display); font-size: 22px; font-weight: 700; fill: var(--ink); text-anchor: middle; }
.donut-cap { font-size: 9px; fill: var(--ink-faint); text-anchor: middle; letter-spacing: 0.1em; }
.donut-legend { display: flex; gap: var(--sp-4); }
.donut-legend.col { flex-direction: column; gap: var(--sp-2); }
.lg-item { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--ink-soft); }
.lg-dot { width: 10px; height: 10px; border-radius: 3px; }
.lg-dot.approve { background: var(--pine); }
.lg-dot.reject { background: var(--clay); }
.lg-dot.u-consumer { background: var(--pine); }
.lg-dot.u-pharmacist { background: #d8a93a; }
.lg-dot.u-staff { background: var(--clay); }

/* 快捷入口 */
.shortcuts { display: grid; grid-template-columns: 1fr 1fr; gap: var(--sp-4); }
.shortcuts-3 { grid-template-columns: repeat(3, 1fr); }
.sc {
  display: flex; align-items: center; gap: var(--sp-4); text-align: left;
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  padding: var(--sp-5); transition: border-color var(--t-fast), box-shadow var(--t-fast), transform var(--t-fast);
}
.sc:hover { border-color: var(--pine); box-shadow: var(--shadow-md); transform: translateY(-2px); }
.sc-icon { width: 48px; height: 48px; border-radius: var(--r-md); display: grid; place-items: center; flex-shrink: 0; color: #fff; }
.sc-icon.pine { background: var(--pine); }
.sc-icon.clay { background: var(--clay); }
.sc-icon.slate { background: #5b86b3; }
.sc-text { flex: 1; display: flex; flex-direction: column; }
.sc-title { font-size: 16px; font-weight: 600; }
.sc-sub { font-size: 12.5px; color: var(--ink-faint); margin-top: 3px; }
.sc-arrow { color: var(--ink-faint); transition: transform var(--t-fast); }
.sc:hover .sc-arrow { transform: translateX(4px); color: var(--pine); }

.state { padding: var(--sp-9); text-align: center; color: var(--ink-soft); font-family: var(--font-display); font-size: 19px; }

@media (max-width: 860px) {
  .dash-head { flex-direction: column; }
  .kpis { grid-template-columns: 1fr; }
  .kpis-4 { grid-template-columns: 1fr 1fr; }
  .charts { grid-template-columns: 1fr; }
  .shortcuts { grid-template-columns: 1fr; }
  .shortcuts-3 { grid-template-columns: 1fr; }
}

/* HERO */
.hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: var(--sp-7);
  align-items: center;
  padding-top: var(--sp-9);
  padding-bottom: var(--sp-8);
}
.hero-title {
  font-size: clamp(40px, 5.4vw, 72px);
  margin-top: var(--sp-3);
  line-height: 1.04;
}
.hero-title em { font-style: italic; color: var(--clay); }
.hero-desc { margin-top: var(--sp-5); color: var(--ink-soft); font-size: 17px; line-height: 1.75; }

.search { margin-top: var(--sp-6); }
.search-box {
  display: flex; align-items: center; gap: 10px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-pill);
  padding: 7px 7px 7px 18px;
  box-shadow: var(--shadow-sm);
  transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.search-box:focus-within { border-color: var(--pine); box-shadow: 0 0 0 4px var(--pine-soft); }
.s-ic { color: var(--ink-faint); flex-shrink: 0; }
.search-box input { flex: 1; border: none; background: none; outline: none; font-size: 16px; padding: 8px 0; }

.tags { display: flex; align-items: center; gap: 8px; margin-top: var(--sp-4); flex-wrap: wrap; }
.tags-label { font-size: 13px; color: var(--ink-faint); font-weight: 600; }
.tag {
  padding: 5px 14px; font-size: 13.5px; font-weight: 500;
  background: var(--surface); border: 1px solid var(--line);
  border-radius: var(--r-pill); color: var(--ink-soft);
  transition: all var(--t-fast) var(--ease);
}
.tag:hover { border-color: var(--pine); color: var(--pine); background: var(--pine-soft); }

/* HERO 装饰 */
.hero-art { position: relative; height: 420px; }
.vial {
  position: absolute; border-radius: var(--r-lg);
  background: linear-gradient(160deg, var(--pine), var(--pine-deep));
}
.v1 { width: 200px; height: 270px; right: 40px; top: 20px; transform: rotate(-8deg); opacity: 0.92; }
.v2 { width: 160px; height: 220px; right: 150px; top: 120px; transform: rotate(7deg); background: linear-gradient(160deg, var(--clay), #a8482a); opacity: 0.92; }
.card-float {
  position: absolute; left: 0; bottom: 10px;
  width: 290px; padding: var(--sp-5);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-lg);
}
.cf-tag {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11px; font-weight: 700; color: var(--pine);
  background: var(--pine-soft); padding: 4px 10px; border-radius: var(--r-pill);
}
.cf-tag::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 3px rgba(63,125,82,0.2); }
.cf-q { font-family: var(--font-display); font-size: 18px; margin-top: 12px; color: var(--ink); }
.cf-meter { display: flex; align-items: center; gap: 10px; margin-top: 14px; }
.cf-bar { flex: 1; height: 7px; background: var(--paper-2); border-radius: 999px; overflow: hidden; }
.cf-bar span { display: block; height: 100%; width: 95%; background: var(--pine); border-radius: 999px; animation: grow 1.4s var(--ease-out) both 0.5s; }
@keyframes grow { from { width: 0; } }
.cf-pct { font-size: 12px; font-weight: 700; color: var(--pine); }
.cf-a { font-size: 13px; color: var(--ink-soft); margin-top: 12px; line-height: 1.55; }

/* 三大保障 */
.pillars {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--sp-5);
  padding: var(--sp-7) var(--sp-5);
}
.pillar {
  padding: var(--sp-5);
  border-left: 2px solid var(--line);
  transition: border-color var(--t-mid);
}
.pillar:hover { border-left-color: var(--clay); }
.p-num { font-family: var(--font-display); font-size: 28px; color: var(--clay); }
.pillar h3 { font-size: 20px; margin-top: 10px; }
.pillar p { color: var(--ink-soft); font-size: 14.5px; margin-top: 8px; line-height: 1.6; }

/* 区块标题 */
.sec-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: var(--sp-6); }
.sec-head h2 { font-size: 34px; margin-top: 6px; }
.more {
  display: inline-flex; align-items: center; gap: 6px;
  font-weight: 600; color: var(--pine); font-size: 15px;
  transition: gap var(--t-fast);
}
.more:hover { gap: 11px; }

.featured { padding-top: var(--sp-7); }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--sp-5); }
.skeleton { height: 230px; border-radius: var(--r-lg); background: linear-gradient(100deg, var(--paper-2) 30%, var(--surface) 50%, var(--paper-2) 70%); background-size: 200% 100%; animation: shimmer 1.4s infinite; }
@keyframes shimmer { to { background-position: -200% 0; } }
.empty { color: var(--ink-soft); }
.empty a { color: var(--pine); font-weight: 600; text-decoration: underline; }

/* CTA */
.cta { margin-top: var(--sp-8); }
.cta-inner {
  display: flex; align-items: center; justify-content: space-between; gap: var(--sp-5);
  flex-wrap: wrap;
  padding: var(--sp-7);
  background: var(--pine);
  border-radius: var(--r-lg);
  position: relative; overflow: hidden;
}
.cta-inner::after {
  content: ''; position: absolute; right: -60px; top: -60px;
  width: 240px; height: 240px; border-radius: 50%;
  background: radial-gradient(circle, rgba(200,100,60,0.4), transparent 70%);
  pointer-events: none;
}
.cta-text { position: relative; }
.cta-inner :deep(.btn) { position: relative; z-index: 1; }
.cta-text h2 { color: var(--paper); font-size: 30px; margin-top: 8px; }

@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; }
  .hero-art { display: none; }
  .pillars { grid-template-columns: 1fr; gap: 0; }
  .grid { grid-template-columns: 1fr; }
  .sec-head h2 { font-size: 28px; }
}
</style>

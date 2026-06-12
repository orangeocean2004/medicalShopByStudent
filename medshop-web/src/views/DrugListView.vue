<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { drugApi } from '@/api/shop'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import DrugCard from '@/components/DrugCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()

const keyword = ref(route.query.q || '')
const drugs = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const filter = ref('all') // all | otc | rx

const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const shown = computed(() => {
  if (filter.value === 'otc') return drugs.value.filter((d) => d.isRx === 0)
  if (filter.value === 'rx') return drugs.value.filter((d) => d.isRx === 1)
  return drugs.value
})

async function load() {
  loading.value = true
  try {
    // 关键词为空时后端返回全部药品（目录默认浏览）
    const res = await drugApi.search(keyword.value.trim(), page.value, size)
    drugs.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  router.replace({ query: { q: keyword.value.trim() } })
  load()
}

function go(p) {
  if (p < 1 || p > pages.value) return
  page.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function addToCart(drug) {
  cart.add(drug)
  toast.success(`已加入「${drug.name}」`)
}

watch(() => route.query.q, (q) => {
  if (q !== undefined && q !== keyword.value) {
    keyword.value = q
    page.value = 1
    load()
  }
})

onMounted(() => {
  load()
})
</script>

<template>
  <div class="list-page container">
    <!-- 搜索头 -->
    <div class="search-head">
      <div>
        <p class="overline">DRUG CATALOG</p>
        <h1>药品目录</h1>
      </div>
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.8" class="s-ic"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2" stroke-linecap="round"/></svg>
        <input v-model="keyword" placeholder="搜索药品名、症状或适应症…" @keyup.enter="search" />
        <BaseButton size="sm" @click="search">搜索</BaseButton>
      </div>
    </div>

    <!-- 结果工具条 -->
    <div class="toolbar">
      <p class="result-meta">
        <template v-if="keyword">关于「<strong>{{ keyword }}</strong>」找到 <strong>{{ total }}</strong> 个结果</template>
        <template v-else>共 <strong>{{ total }}</strong> 种在售药品</template>
      </p>
      <div class="filters">
        <button :class="{ on: filter === 'all' }" @click="filter = 'all'">全部</button>
        <button :class="{ on: filter === 'otc' }" @click="filter = 'otc'">非处方 OTC</button>
        <button :class="{ on: filter === 'rx' }" @click="filter = 'rx'">处方药 Rx</button>
      </div>
    </div>

    <!-- 列表 -->
    <div v-if="loading" class="grid">
      <div v-for="i in 6" :key="i" class="skeleton"></div>
    </div>

    <div v-else-if="shown.length" class="grid">
      <DrugCard
        v-for="(d, i) in shown"
        :key="d.id"
        :drug="d"
        :index="i"
        @add="addToCart"
      />
    </div>

    <!-- 空态 -->
    <div v-else class="empty-state">
      <div class="es-art">
        <svg viewBox="0 0 24 24" width="38" height="38" fill="none" stroke="currentColor" stroke-width="1.4"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2" stroke-linecap="round"/></svg>
      </div>
      <h3>{{ keyword ? '没有找到相关药品' : '暂无药品' }}</h3>
      <p>{{ keyword ? '换个关键词试试，或咨询智能药师。' : '药品目录为空。' }}</p>
      <BaseButton v-if="keyword" variant="ghost" @click="router.push({ name: 'consult' })">问问智能药师</BaseButton>
    </div>

    <!-- 分页 -->
    <div v-if="!loading && pages > 1 && filter === 'all'" class="pager">
      <button class="pg" :disabled="page === 1" @click="go(page - 1)">上一页</button>
      <span class="pg-info">{{ page }} / {{ pages }}</span>
      <button class="pg" :disabled="page === pages" @click="go(page + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.list-page { padding-top: var(--sp-7); }
.search-head {
  display: flex; align-items: flex-end; justify-content: space-between; gap: var(--sp-5);
  flex-wrap: wrap;
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--line);
}
.search-head h1 { font-size: 38px; margin-top: 6px; }
.search-box {
  display: flex; align-items: center; gap: 8px;
  background: var(--surface); border: 1px solid var(--line);
  border-radius: var(--r-pill); padding: 6px 6px 6px 16px;
  min-width: 340px;
  transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.search-box:focus-within { border-color: var(--pine); box-shadow: 0 0 0 4px var(--pine-soft); }
.s-ic { color: var(--ink-faint); }
.search-box input { flex: 1; border: none; background: none; outline: none; font-size: 15px; padding: 6px 0; }

.toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: var(--sp-4);
  flex-wrap: wrap;
  margin: var(--sp-5) 0;
}
.result-meta { color: var(--ink-soft); font-size: 15px; }
.result-meta strong { color: var(--ink); }
.filters { display: inline-flex; gap: 4px; padding: 4px; background: var(--paper-2); border-radius: var(--r-pill); }
.filters button {
  padding: 7px 16px; font-size: 13.5px; font-weight: 600; color: var(--ink-soft);
  border-radius: var(--r-pill); transition: all var(--t-fast) var(--ease);
}
.filters button.on { background: var(--surface); color: var(--pine); box-shadow: var(--shadow-sm); }

.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--sp-5); margin-top: var(--sp-4); }
.skeleton { height: 230px; border-radius: var(--r-lg); background: linear-gradient(100deg, var(--paper-2) 30%, var(--surface) 50%, var(--paper-2) 70%); background-size: 200% 100%; animation: shimmer 1.4s infinite; }
@keyframes shimmer { to { background-position: -200% 0; } }

.empty-state { text-align: center; padding: var(--sp-9) var(--sp-5); }
.es-art { display: inline-grid; place-items: center; width: 84px; height: 84px; border-radius: 50%; background: var(--pine-soft); color: var(--pine); margin-bottom: var(--sp-4); }
.empty-state h3 { font-size: 24px; }
.empty-state p { color: var(--ink-soft); margin: 10px 0 var(--sp-5); }

.pager { display: flex; align-items: center; justify-content: center; gap: var(--sp-4); margin-top: var(--sp-7); }
.pg { padding: 10px 22px; border: 1px solid var(--line-strong); border-radius: var(--r-pill); font-weight: 600; font-size: 14px; transition: all var(--t-fast); }
.pg:hover:not(:disabled) { border-color: var(--pine); color: var(--pine); }
.pg:disabled { opacity: 0.4; cursor: not-allowed; }
.pg-info { font-family: var(--font-display); font-size: 16px; font-weight: 600; }

@media (max-width: 900px) {
  .grid { grid-template-columns: 1fr; }
  .search-box { min-width: 0; width: 100%; }
  .search-head h1 { font-size: 30px; }
}
</style>

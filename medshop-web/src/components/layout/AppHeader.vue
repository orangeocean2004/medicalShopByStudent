<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const cart = useCartStore()

// 导航按角色区分：消费者购药，药师/运营进各自工作台
const nav = computed(() => {
  if (auth.isOperator) {
    return [
      { name: 'home', label: '首页' },
      { name: 'admin', label: '管理后台' },
      { name: 'profile', label: '个人中心' }
    ]
  }
  if (auth.isPharmacist) {
    return [
      { name: 'home', label: '首页' },
      { name: 'workbench-handoff', label: '转人工工单' },
      { name: 'workbench-rx', label: '处方审核' },
      { name: 'profile', label: '个人中心' }
    ]
  }
  // 消费者（默认）
  return [
    { name: 'home', label: '首页' },
    { name: 'drugs', label: '药品' },
    { name: 'consult', label: '智能药师' },
    { name: 'prescriptions', label: '我的处方' },
    { name: 'orders', label: '我的订单' },
    { name: 'privacy', label: '隐私授权' }
  ]
})

// 仅消费者显示购物车
const showCart = computed(() => auth.isConsumer || auth.role == null)

const initial = computed(() => (auth.user?.phone || '用')[0])

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <header class="hd">
    <div class="container hd-inner">
      <!-- 品牌 -->
      <router-link to="/home" class="brand">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 32 32" width="30" height="30">
            <path d="M16 3c5 4 8 8 8 13a8 8 0 0 1-16 0c0-5 3-9 8-13z"
                  fill="none" stroke="currentColor" stroke-width="1.6"/>
            <path d="M16 11v10M11 16h10" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="brand-text">
          <span class="brand-cn">松和堂</span>
          <span class="brand-en">SONGHE&nbsp;APOTHECARY</span>
        </span>
      </router-link>

      <!-- 导航 -->
      <nav class="nav">
        <router-link
          v-for="n in nav"
          :key="n.name"
          :to="{ name: n.name }"
          class="nav-link"
          :class="{ active: route.name === n.name }"
        >{{ n.label }}</router-link>
      </nav>

      <!-- 右侧操作 -->
      <div class="actions">
        <router-link v-if="showCart" to="/cart" class="cart-btn" aria-label="购物车">
          <svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M3 4h2l2.4 12.3a1 1 0 0 0 1 .8h8.7a1 1 0 0 0 1-.8L21 8H6" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="9.5" cy="20" r="1.3" fill="currentColor" stroke="none"/>
            <circle cx="17.5" cy="20" r="1.3" fill="currentColor" stroke="none"/>
          </svg>
          <span v-if="cart.count" class="cart-badge">{{ cart.count }}</span>
        </router-link>

        <div class="user">
          <router-link :to="{ name: 'profile' }" class="user-link" title="个人中心">
            <span class="avatar">{{ initial }}</span>
            <div class="user-meta">
              <span class="user-phone">{{ auth.user?.phone }}</span>
              <span class="user-role">{{ auth.roleName }}</span>
            </div>
          </router-link>
          <button class="logout" @click="logout" title="退出登录">
            <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.7">
              <path d="M15 4h3a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-3M10 12H3m0 0 3.5-3.5M3 12l3.5 3.5"
                    stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.hd {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--header-h);
  display: flex;
  align-items: center;
  background: rgba(244, 240, 232, 0.82);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
}
.hd-inner {
  display: flex;
  align-items: center;
  gap: var(--sp-6);
}

/* 品牌 */
.brand { display: flex; align-items: center; gap: 12px; }
.brand-mark { color: var(--pine); display: grid; place-items: center; }
.brand-text { display: flex; flex-direction: column; line-height: 1; }
.brand-cn {
  font-family: var(--font-display);
  font-size: 22px; font-weight: 600; color: var(--ink);
  letter-spacing: 0.04em;
}
.brand-en {
  font-size: 9px; letter-spacing: 0.28em; color: var(--ink-faint);
  margin-top: 4px; font-weight: 600;
}

/* 导航 */
.nav { display: flex; gap: 4px; margin-left: auto; }
.nav-link {
  position: relative;
  padding: 8px 16px;
  font-size: 15px; font-weight: 500;
  color: var(--ink-soft);
  border-radius: var(--r-pill);
  transition: color var(--t-fast) var(--ease), background var(--t-fast) var(--ease);
}
.nav-link:hover { color: var(--pine); background: var(--pine-soft); }
.nav-link.active { color: var(--pine); }
.nav-link.active::after {
  content: '';
  position: absolute;
  left: 50%; bottom: 2px;
  width: 5px; height: 5px;
  background: var(--clay);
  border-radius: 50%;
  transform: translateX(-50%);
}

/* 右侧 */
.actions { display: flex; align-items: center; gap: var(--sp-4); }
.cart-btn {
  position: relative;
  display: grid; place-items: center;
  width: 42px; height: 42px;
  border-radius: 50%;
  color: var(--ink);
  transition: background var(--t-fast) var(--ease);
}
.cart-btn:hover { background: var(--pine-soft); color: var(--pine); }
.cart-badge {
  position: absolute; top: 1px; right: 0;
  min-width: 18px; height: 18px; padding: 0 5px;
  background: var(--clay); color: #fff;
  font-size: 11px; font-weight: 700;
  border-radius: 999px;
  display: grid; place-items: center;
  border: 2px solid var(--paper);
}

.user { display: flex; align-items: center; gap: 10px; padding-left: var(--sp-4); border-left: 1px solid var(--line); }
.user-link {
  display: flex; align-items: center; gap: 10px;
  padding: 4px 8px 4px 4px; border-radius: var(--r-pill);
  transition: background var(--t-fast);
}
.user-link:hover { background: var(--pine-soft); }
.avatar {
  width: 38px; height: 38px;
  border-radius: 50%;
  background: var(--pine); color: var(--paper);
  display: grid; place-items: center;
  font-weight: 700; font-size: 16px;
  text-transform: uppercase;
}
.user-meta { display: flex; flex-direction: column; line-height: 1.25; }
.user-phone { font-size: 13px; font-weight: 600; }
.user-role { font-size: 11px; color: var(--clay); font-weight: 600; }
.logout {
  display: grid; place-items: center;
  width: 32px; height: 32px; border-radius: 8px;
  color: var(--ink-faint);
  transition: color var(--t-fast), background var(--t-fast);
}
.logout:hover { color: var(--danger); background: var(--clay-soft); }

@media (max-width: 860px) {
  .nav { display: none; }
  .user-meta { display: none; }
}
</style>

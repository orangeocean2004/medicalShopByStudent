<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()

const mode = ref('login') // login | register
const phone = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)

async function submit() {
  if (!phone.value || !password.value) {
    toast.error('请填写手机号与密码')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(phone.value, password.value)
      toast.success('欢迎回来')
    } else {
      await auth.register(phone.value, password.value, nickname.value)
      toast.success('注册成功，已为你登录')
    }
    router.push(route.query.redirect || '/home')
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function fillDemo() {
  phone.value = '13800000000'
  password.value = '123456'
  mode.value = 'login'
}
</script>

<template>
  <div class="auth">
    <!-- 左侧：品牌叙事面板 -->
    <aside class="brand-panel">
      <div class="bp-grain"></div>
      <div class="bp-content">
        <div class="bp-top">
          <span class="bp-mark">
            <svg viewBox="0 0 32 32" width="34" height="34">
              <path d="M16 3c5 4 8 8 8 13a8 8 0 0 1-16 0c0-5 3-9 8-13z" fill="none" stroke="currentColor" stroke-width="1.5"/>
              <path d="M16 11v10M11 16h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </span>
          <span class="bp-brand">松和堂</span>
        </div>

        <div class="bp-mid">
          <p class="bp-overline">SONGHE APOTHECARY · EST. 2026</p>
          <h1 class="bp-title">把一所<em>讲究</em>的<br/>药铺，搬进屏幕里。</h1>
          <p class="bp-desc">
            道地药材、执业药师、智能问询。<br/>
            从一次搜索到一份安心，慢工细做。
          </p>
        </div>

        <ul class="bp-feat">
          <li><span>01</span> 智能药师 · 有据可循的用药建议</li>
          <li><span>02</span> 处方药严格审核 · 安全第一</li>
          <li><span>03</span> 低置信即转人工 · 真人兜底</li>
        </ul>
      </div>
    </aside>

    <!-- 右侧：表单 -->
    <section class="form-panel">
      <div class="form-wrap rise">
        <div class="switch">
          <button :class="{ on: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button :class="{ on: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>

        <h2 class="form-title">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h2>
        <p class="form-sub">{{ mode === 'login' ? '登录以继续选购与咨询' : '一分钟开启你的健康档案' }}</p>

        <form @submit.prevent="submit" class="form">
          <BaseInput v-model="phone" label="手机号" placeholder="请输入手机号" icon>
            <template #icon>
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="6" y="2.5" width="12" height="19" rx="2.5"/><path d="M10.5 18.5h3" stroke-linecap="round"/></svg>
            </template>
          </BaseInput>

          <BaseInput v-if="mode === 'register'" v-model="nickname" label="昵称（选填）" placeholder="想让药师怎么称呼你" icon>
            <template #icon>
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c0-3.9 3.1-6 7-6s7 2.1 7 6" stroke-linecap="round"/></svg>
            </template>
          </BaseInput>

          <BaseInput v-model="password" label="密码" type="password" placeholder="请输入密码" icon>
            <template #icon>
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4.5" y="10.5" width="15" height="9.5" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" stroke-linecap="round"/></svg>
            </template>
          </BaseInput>

          <BaseButton type="submit" size="lg" block :loading="loading">
            {{ mode === 'login' ? '登 录' : '注 册 并 登 录' }}
          </BaseButton>
        </form>

        <button class="demo" @click="fillDemo">
          使用演示账号体验 · 13800000000 / 123456
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
}

/* 左侧叙事面板 */
.brand-panel {
  position: relative;
  background: var(--pine);
  color: var(--paper);
  overflow: hidden;
  display: flex;
}
.bp-grain {
  position: absolute; inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle at 78% 18%, rgba(200, 100, 60, 0.34), transparent 44%),
    radial-gradient(circle at 14% 88%, rgba(127, 160, 140, 0.34), transparent 46%);
}
.bp-grain::after {
  content: '';
  position: absolute; inset: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 5px 5px;
  mix-blend-mode: overlay;
}
.bp-content {
  position: relative;
  display: flex; flex-direction: column; justify-content: space-between;
  padding: var(--sp-8) var(--sp-7);
  width: 100%;
}
.bp-top { display: flex; align-items: center; gap: 12px; }
.bp-mark { color: var(--paper); }
.bp-brand { font-family: var(--font-display); font-size: 24px; font-weight: 600; letter-spacing: 0.06em; }

.bp-overline { font-size: 11px; letter-spacing: 0.26em; color: var(--sage); font-weight: 600; }
.bp-title {
  font-size: clamp(34px, 4vw, 52px);
  color: var(--paper);
  margin-top: var(--sp-4);
  line-height: 1.08;
}
.bp-title em { color: #e9b48a; font-style: italic; }
.bp-desc { margin-top: var(--sp-5); color: rgba(244,240,232,0.74); font-size: 16px; line-height: 1.7; }

.bp-feat { display: flex; flex-direction: column; gap: 14px; }
.bp-feat li {
  display: flex; align-items: center; gap: 14px;
  font-size: 14.5px; color: rgba(244,240,232,0.85);
  padding-top: 14px; border-top: 1px solid rgba(244,240,232,0.16);
}
.bp-feat span {
  font-family: var(--font-display); font-size: 13px; font-weight: 600;
  color: #e9b48a; min-width: 22px;
}

/* 右侧表单 */
.form-panel { display: grid; place-items: center; padding: var(--sp-6); }
.form-wrap { width: 100%; max-width: 392px; }

.switch {
  display: inline-flex; padding: 4px;
  background: var(--paper-2);
  border-radius: var(--r-pill);
  margin-bottom: var(--sp-6);
}
.switch button {
  padding: 8px 24px; font-size: 14.5px; font-weight: 600;
  border-radius: var(--r-pill); color: var(--ink-soft);
  transition: all var(--t-fast) var(--ease);
}
.switch button.on { background: var(--surface); color: var(--pine); box-shadow: var(--shadow-sm); }

.form-title { font-size: 32px; }
.form-sub { color: var(--ink-soft); margin-top: 6px; margin-bottom: var(--sp-6); }
.form { display: flex; flex-direction: column; gap: var(--sp-4); }
.form > :last-child { margin-top: var(--sp-3); }

.demo {
  display: block; width: 100%;
  margin-top: var(--sp-5);
  padding: 12px;
  font-size: 13px; font-weight: 600; color: var(--clay);
  background: var(--clay-soft);
  border-radius: var(--r-md);
  border: 1px dashed #e0b9a7;
  transition: background var(--t-fast);
}
.demo:hover { background: #efd5c8; }

@media (max-width: 900px) {
  .auth { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
}
</style>

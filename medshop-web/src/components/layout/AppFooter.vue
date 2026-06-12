<script setup>
const year = new Date().getFullYear()
</script>

<template>
  <footer class="ft">
    <div class="container ft-inner">
      <div class="ft-brand">
        <span class="ft-cn">松和堂</span>
        <p class="ft-tag">以药为媒，以和为本。<br />一所长在屏幕里的现代药铺。</p>
      </div>

      <div class="ft-cols">
        <div class="ft-col">
          <span class="ft-h">服务</span>
          <router-link :to="{ name: 'drugs' }">药品目录</router-link>
          <router-link :to="{ name: 'consult' }">智能药师咨询</router-link>
          <router-link :to="{ name: 'cart' }">我的购物车</router-link>
        </div>
        <div class="ft-col">
          <span class="ft-h">保障</span>
          <router-link :to="{ name: 'privacy' }">隐私与授权</router-link>
          <span class="ft-static">处方药审核</span>
          <span class="ft-static">7×24 人工兜底</span>
        </div>
        <div class="ft-col">
          <span class="ft-h">提示</span>
          <span class="ft-static">用药请遵医嘱</span>
          <span class="ft-static">本平台仅供学习演示</span>
        </div>
      </div>
    </div>
    <div class="ft-base">
      <div class="container ft-base-inner">
        <span>© {{ year }} 松和堂 · 线上购药系统</span>
        <span class="ft-note">课程设计作品 · Vue 3 + Spring Boot</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.ft {
  margin-top: var(--sp-9);
  background: var(--pine);
  color: var(--paper);
}
.ft-inner {
  display: flex;
  justify-content: space-between;
  gap: var(--sp-7);
  padding-top: var(--sp-8);
  padding-bottom: var(--sp-7);
  flex-wrap: wrap;
}
.ft-cn { font-family: var(--font-display); font-size: 26px; font-weight: 600; letter-spacing: 0.05em; }
.ft-tag { margin-top: var(--sp-3); color: rgba(244, 240, 232, 0.62); font-size: 14px; max-width: 240px; }

.ft-cols { display: flex; gap: var(--sp-8); flex-wrap: wrap; }
.ft-col { display: flex; flex-direction: column; gap: 10px; }
.ft-h {
  font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase;
  color: var(--sage); font-weight: 600; margin-bottom: 4px;
}
.ft-col a, .ft-static { color: rgba(244, 240, 232, 0.78); font-size: 14px; transition: color var(--t-fast); }
.ft-col a:hover { color: #fff; }
.ft-static { cursor: default; }

.ft-base { border-top: 1px solid rgba(244, 240, 232, 0.14); }
.ft-base-inner {
  display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px;
  padding: var(--sp-4) var(--sp-5);
  font-size: 12.5px; color: rgba(244, 240, 232, 0.5);
}

@media (max-width: 640px) {
  .ft-inner { flex-direction: column; gap: var(--sp-6); }
  .ft-cols { gap: var(--sp-6); }
}
</style>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { userApi } from '@/api/admin'
import { addressApi } from '@/api/address'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import BaseButton from '@/components/ui/BaseButton.vue'

const auth = useAuthStore()
const toast = useToastStore()

const profile = ref(null)
const loading = ref(true)

// 昵称编辑
const nickname = ref('')
const savingName = ref(false)

// 密码修改
const oldPwd = ref('')
const newPwd = ref('')
const newPwd2 = ref('')
const savingPwd = ref(false)

// ===== 收货地址（仅消费者）=====
const isConsumer = computed(() => auth.isConsumer || auth.role == null)
const addresses = ref([])
const addrLoading = ref(false)
const showAddrForm = ref(false)
const editingId = ref(null) // null=新增, 否则编辑
const addrForm = ref(blankAddr())
const savingAddr = ref(false)

function blankAddr() {
  return { receiver: '', phone: '', region: '', detail: '', isDefault: 0 }
}

async function loadAddresses() {
  addrLoading.value = true
  try {
    addresses.value = await addressApi.list()
  } catch (e) {
    toast.error(e.message)
  } finally {
    addrLoading.value = false
  }
}

function openAddNew() {
  editingId.value = null
  addrForm.value = blankAddr()
  showAddrForm.value = true
}

function openEdit(a) {
  editingId.value = a.id
  addrForm.value = { receiver: a.receiver, phone: a.phone, region: a.region, detail: a.detail, isDefault: a.isDefault }
  showAddrForm.value = true
}

function cancelAddr() {
  showAddrForm.value = false
  editingId.value = null
  addrForm.value = blankAddr()
}

async function saveAddr() {
  const f = addrForm.value
  if (!f.receiver.trim()) return toast.error('请填写收货人')
  if (!/^\d{11}$/.test(f.phone)) return toast.error('请填写 11 位联系电话')
  if (!f.region.trim()) return toast.error('请填写所在地区')
  if (!f.detail.trim()) return toast.error('请填写详细地址')
  savingAddr.value = true
  try {
    if (editingId.value) {
      await addressApi.update(editingId.value, f)
      toast.success('地址已更新')
    } else {
      await addressApi.create(f)
      toast.success('地址已添加')
    }
    cancelAddr()
    await loadAddresses()
  } catch (e) {
    toast.error(e.message)
  } finally {
    savingAddr.value = false
  }
}

async function removeAddr(a) {
  if (!confirm(`确定删除收货地址「${a.receiver} · ${a.detail}」？`)) return
  try {
    await addressApi.remove(a.id)
    toast.success('地址已删除')
    await loadAddresses()
  } catch (e) {
    toast.error(e.message)
  }
}

async function makeDefault(a) {
  if (a.isDefault === 1) return
  try {
    await addressApi.setDefault(a.id)
    await loadAddresses()
  } catch (e) {
    toast.error(e.message)
  }
}

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await userApi.me()
    nickname.value = profile.value.nickname || ''
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function saveNickname() {
  const n = nickname.value.trim()
  if (!n) {
    toast.error('昵称不能为空')
    return
  }
  if (n === profile.value.nickname) {
    toast.info('昵称没有变化')
    return
  }
  savingName.value = true
  try {
    await userApi.updateNickname(n)
    profile.value.nickname = n
    toast.success('昵称已更新')
  } catch (e) {
    toast.error(e.message)
  } finally {
    savingName.value = false
  }
}

async function savePassword() {
  if (!oldPwd.value) {
    toast.error('请输入原密码')
    return
  }
  if (newPwd.value.length < 6) {
    toast.error('新密码至少 6 位')
    return
  }
  if (newPwd.value !== newPwd2.value) {
    toast.error('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await userApi.changePassword(oldPwd.value, newPwd.value)
    toast.success('密码已修改，下次登录请使用新密码')
    oldPwd.value = ''
    newPwd.value = ''
    newPwd2.value = ''
  } catch (e) {
    toast.error(e.message)
  } finally {
    savingPwd.value = false
  }
}

onMounted(() => {
  loadProfile()
  if (isConsumer.value) loadAddresses()
})
</script>

<template>
  <div class="profile container">
    <header class="pf-head rise">
      <p class="overline">MY ACCOUNT</p>
      <h1>个人中心</h1>
    </header>

    <div v-if="loading" class="state">正在加载…</div>

    <div v-else-if="profile" class="pf-grid">
      <!-- 资料卡 -->
      <section class="card identity rise">
        <div class="id-avatar">{{ (profile.phone || '用')[0] }}</div>
        <div class="id-name">{{ profile.nickname }}</div>
        <span class="id-role">{{ profile.roleName }}</span>
        <dl class="id-meta">
          <div><dt>手机号</dt><dd class="mono">{{ profile.phone }}</dd></div>
          <div><dt>账号 ID</dt><dd class="mono">#{{ profile.id }}</dd></div>
          <div><dt>注册时间</dt><dd>{{ profile.createdAt || '—' }}</dd></div>
        </dl>
      </section>

      <div class="pf-right">
        <!-- 编辑昵称 -->
        <section class="card rise" style="animation-delay: 80ms">
          <h2 class="card-title">编辑昵称</h2>
          <p class="card-sub">展示在工作台与对话中的名字。</p>
          <div class="field-row">
            <input v-model="nickname" type="text" maxlength="20" placeholder="输入昵称" @keyup.enter="saveNickname" />
            <BaseButton size="md" :loading="savingName" @click="saveNickname">保存</BaseButton>
          </div>
        </section>

        <!-- 修改密码 -->
        <section class="card rise" style="animation-delay: 160ms">
          <h2 class="card-title">修改密码</h2>
          <p class="card-sub">修改后请用新密码重新登录。</p>
          <div class="pwd-fields">
            <label><span>原密码</span><input v-model="oldPwd" type="password" placeholder="当前密码" autocomplete="current-password" /></label>
            <label><span>新密码</span><input v-model="newPwd" type="password" placeholder="至少 6 位" autocomplete="new-password" /></label>
            <label><span>确认新密码</span><input v-model="newPwd2" type="password" placeholder="再输入一次" autocomplete="new-password" @keyup.enter="savePassword" /></label>
          </div>
          <BaseButton size="md" :loading="savingPwd" @click="savePassword">更新密码</BaseButton>
        </section>

        <!-- 收货地址（仅消费者）-->
        <section v-if="isConsumer" class="card rise" style="animation-delay: 240ms">
          <div class="addr-head">
            <div>
              <h2 class="card-title">收货地址</h2>
              <p class="card-sub">下单结算时可选择，默认地址会自动选中。</p>
            </div>
            <BaseButton v-if="!showAddrForm" size="sm" @click="openAddNew">+ 新增地址</BaseButton>
          </div>

          <!-- 新增/编辑表单 -->
          <transition name="slide">
            <div v-if="showAddrForm" class="addr-form">
              <div class="af-row">
                <label><span>收货人</span><input v-model="addrForm.receiver" maxlength="20" placeholder="如 李阿姨" /></label>
                <label><span>联系电话</span><input v-model="addrForm.phone" maxlength="11" placeholder="11 位手机号" /></label>
              </div>
              <label class="af-block"><span>所在地区</span><input v-model="addrForm.region" placeholder="如 北京市朝阳区" /></label>
              <label class="af-block"><span>详细地址</span><input v-model="addrForm.detail" placeholder="街道、门牌号、楼栋单元室" /></label>
              <label class="af-check">
                <input type="checkbox" :checked="addrForm.isDefault === 1" @change="addrForm.isDefault = $event.target.checked ? 1 : 0" />
                设为默认地址
              </label>
              <div class="af-actions">
                <BaseButton size="md" :loading="savingAddr" @click="saveAddr">{{ editingId ? '保存修改' : '确认添加' }}</BaseButton>
                <button class="link-btn" @click="cancelAddr">取消</button>
              </div>
            </div>
          </transition>

          <div v-if="addrLoading" class="addr-state">加载中…</div>
          <div v-else-if="!addresses.length && !showAddrForm" class="addr-empty">
            还没有收货地址，添加一个吧。
          </div>
          <ul v-else class="addr-list">
            <li v-for="a in addresses" :key="a.id" class="addr-item" :class="{ def: a.isDefault === 1 }">
              <div class="ai-main">
                <div class="ai-line1">
                  <span class="ai-name">{{ a.receiver }}</span>
                  <span class="ai-phone">{{ a.phone }}</span>
                  <span v-if="a.isDefault === 1" class="ai-default">默认</span>
                </div>
                <p class="ai-addr">{{ a.region }} {{ a.detail }}</p>
              </div>
              <div class="ai-ops">
                <button v-if="a.isDefault !== 1" class="link-btn" @click="makeDefault(a)">设为默认</button>
                <button class="link-btn" @click="openEdit(a)">编辑</button>
                <button class="link-btn danger" @click="removeAddr(a)">删除</button>
              </div>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile { padding-top: var(--sp-6); max-width: 920px; }
.pf-head { margin-bottom: var(--sp-6); }
.pf-head h1 { font-size: 36px; margin-top: 6px; }
.state { padding: var(--sp-9); text-align: center; color: var(--ink-soft); font-family: var(--font-display); font-size: 19px; }

.pf-grid { display: grid; grid-template-columns: 300px 1fr; gap: var(--sp-5); align-items: start; }
.card {
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg);
  padding: var(--sp-6);
}

/* 资料卡 */
.identity { display: flex; flex-direction: column; align-items: center; text-align: center; }
.id-avatar {
  width: 76px; height: 76px; border-radius: 50%;
  background: var(--pine); color: var(--paper);
  display: grid; place-items: center;
  font-family: var(--font-display); font-size: 32px; font-weight: 600;
  text-transform: uppercase;
}
.id-name { font-family: var(--font-display); font-size: 22px; font-weight: 600; margin-top: var(--sp-4); }
.id-role {
  margin-top: 6px; font-size: 12px; font-weight: 700; color: var(--clay);
  background: var(--clay-soft); padding: 3px 12px; border-radius: var(--r-pill);
}
.id-meta { width: 100%; margin-top: var(--sp-5); border-top: 1px solid var(--line); padding-top: var(--sp-4); }
.id-meta > div { display: flex; justify-content: space-between; align-items: center; padding: 7px 0; }
.id-meta dt { font-size: 13px; color: var(--ink-faint); }
.id-meta dd { font-size: 14px; color: var(--ink); font-weight: 500; }
.mono { font-family: var(--font-mono); }

.pf-right { display: flex; flex-direction: column; gap: var(--sp-5); }
.card-title { font-size: 19px; }
.card-sub { font-size: 13.5px; color: var(--ink-faint); margin-top: 4px; margin-bottom: var(--sp-4); }

.field-row { display: flex; gap: var(--sp-3); }
.field-row input { flex: 1; }

input[type="text"], input[type="password"] {
  width: 100%; padding: 11px 14px; font-size: 14px;
  border: 1px solid var(--line); border-radius: var(--r-md); background: var(--paper);
  transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
input:focus { outline: none; border-color: var(--pine); box-shadow: 0 0 0 3px var(--pine-soft); }

.pwd-fields { display: flex; flex-direction: column; gap: var(--sp-3); margin-bottom: var(--sp-4); }
.pwd-fields label { display: block; }
.pwd-fields span { display: block; font-size: 12.5px; font-weight: 600; color: var(--ink-soft); margin-bottom: 6px; }

/* 收货地址 */
.addr-head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--sp-4); margin-bottom: var(--sp-4); }
.addr-head .card-sub { margin-bottom: 0; }

.addr-form { padding: var(--sp-4); margin-bottom: var(--sp-4); background: var(--paper-2); border: 1px solid var(--line); border-radius: var(--r-md); }
.af-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--sp-3); }
.addr-form label { display: block; }
.addr-form .af-block { margin-top: var(--sp-3); }
.addr-form span { display: block; font-size: 12.5px; font-weight: 600; color: var(--ink-soft); margin-bottom: 6px; }
.af-check { display: flex; align-items: center; gap: 8px; margin-top: var(--sp-4); font-size: 14px; color: var(--ink-soft); cursor: pointer; }
.af-check input { width: auto; }
.af-actions { display: flex; align-items: center; gap: var(--sp-4); margin-top: var(--sp-4); }

.addr-state, .addr-empty { padding: var(--sp-5); text-align: center; color: var(--ink-faint); font-size: 14px; }

.addr-list { display: flex; flex-direction: column; gap: var(--sp-3); }
.addr-item {
  display: flex; align-items: flex-start; justify-content: space-between; gap: var(--sp-4);
  padding: var(--sp-4); border: 1px solid var(--line); border-radius: var(--r-md); background: var(--paper);
}
.addr-item.def { border-color: var(--pine); background: var(--pine-soft); }
.ai-line1 { display: flex; align-items: center; gap: 10px; }
.ai-name { font-weight: 600; font-size: 15px; }
.ai-phone { font-family: var(--font-mono); font-size: 13px; color: var(--ink-soft); }
.ai-default { font-size: 11px; font-weight: 700; color: var(--pine); background: var(--surface); border: 1px solid var(--pine); padding: 1px 8px; border-radius: var(--r-pill); }
.ai-addr { font-size: 13.5px; color: var(--ink-soft); margin-top: 5px; line-height: 1.5; }
.ai-ops { display: flex; gap: var(--sp-3); flex-shrink: 0; }

.link-btn { font-size: 13px; font-weight: 600; color: var(--pine); transition: opacity var(--t-fast); }
.link-btn:hover { opacity: 0.7; }
.link-btn.danger { color: var(--danger); }

@media (max-width: 760px) {
  .pf-grid { grid-template-columns: 1fr; }
  .af-row { grid-template-columns: 1fr; }
  .addr-item { flex-direction: column; }
}
</style>

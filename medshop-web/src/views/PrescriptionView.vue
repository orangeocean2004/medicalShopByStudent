<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { drugApi, prescriptionApi } from '@/api/shop'
import { useToastStore } from '@/stores/toast'
import BaseButton from '@/components/ui/BaseButton.vue'

const route = useRoute()
const toast = useToastStore()

const rxDrugs = ref([])          // 可选处方药
const selectedDrugId = ref(null)
const imageData = ref('')        // base64 data URL
const fileName = ref('')
const submitting = ref(false)

const mine = ref([])
const loadingMine = ref(true)

const STATUS = {
  0: { label: '待药师审核', cls: 'wait' },
  1: { label: '审核通过', cls: 'pass' },
  2: { label: '已驳回', cls: 'reject' }
}

const selectedDrug = computed(() =>
  rxDrugs.value.find((d) => d.id === selectedDrugId.value) || null
)

async function loadRxDrugs() {
  try {
    // 拉全部药品，筛出处方药供选择
    const res = await drugApi.search('', 1, 100)
    rxDrugs.value = (res.list || []).filter((d) => d.isRx === 1)
    // 支持从药品详情页带入 drugId
    const qDrug = Number(route.query.drugId)
    if (qDrug && rxDrugs.value.some((d) => d.id === qDrug)) {
      selectedDrugId.value = qDrug
    }
  } catch (e) {
    toast.error(e.message)
  }
}

async function loadMine() {
  loadingMine.value = true
  try {
    mine.value = await prescriptionApi.mine()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loadingMine.value = false
  }
}

function onFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast.error('请上传图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    toast.error('图片不要超过 2MB')
    return
  }
  fileName.value = file.name
  const reader = new FileReader()
  reader.onload = () => { imageData.value = reader.result }
  reader.readAsDataURL(file)
}

async function submit() {
  if (!selectedDrugId.value) {
    toast.error('请选择要购买的处方药')
    return
  }
  if (!imageData.value) {
    toast.error('请上传处方图片')
    return
  }
  submitting.value = true
  try {
    await prescriptionApi.upload(selectedDrugId.value, imageData.value)
    toast.success('处方已提交，等待药师审核')
    imageData.value = ''
    fileName.value = ''
    selectedDrugId.value = null
    loadMine()
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRxDrugs()
  loadMine()
})
</script>

<template>
  <div class="rx container">
    <header class="rx-head rise">
      <p class="overline">PRESCRIPTION</p>
      <h1>我的处方</h1>
      <p class="rx-lead">
        购买处方药前，请先上传医生开具的处方，由执业药师审核通过后即可下单购买。
      </p>
    </header>

    <div class="rx-grid">
      <!-- 上传表单 -->
      <section class="upload rise">
        <h2>上传处方</h2>

        <label class="field">
          <span class="f-label">选择处方药</span>
          <select v-model="selectedDrugId" class="f-select">
            <option :value="null" disabled>请选择要购买的处方药</option>
            <option v-for="d in rxDrugs" :key="d.id" :value="d.id">{{ d.name }}（¥{{ d.price }}）</option>
          </select>
        </label>

        <div class="field">
          <span class="f-label">处方图片</span>
          <label class="dropzone" :class="{ filled: imageData }">
            <input type="file" accept="image/*" @change="onFile" hidden />
            <img v-if="imageData" :src="imageData" alt="处方预览" class="preview" />
            <div v-else class="dz-empty">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 16V4m0 0L8 8m4-4 4 4" stroke-linecap="round" stroke-linejoin="round"/><path d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" stroke-linecap="round"/></svg>
              <span>点击上传处方照片</span>
              <small>支持 JPG/PNG，不超过 2MB</small>
            </div>
          </label>
          <p v-if="fileName" class="f-file">已选择：{{ fileName }}</p>
        </div>

        <div v-if="selectedDrug" class="hint">
          将为「<strong>{{ selectedDrug.name }}</strong>」申请购药资格，审核通过后即可在药品页或购物车下单。
        </div>

        <BaseButton :loading="submitting" :disabled="!selectedDrugId || !imageData" @click="submit">
          提交审核
        </BaseButton>
      </section>

      <!-- 我的处方列表 -->
      <section class="history rise" style="animation-delay: 100ms">
        <h2>审核记录</h2>

        <div v-if="loadingMine" class="state">加载中…</div>
        <div v-else-if="!mine.length" class="state empty">还没有上传过处方</div>
        <ul v-else class="rx-list">
          <li v-for="p in mine" :key="p.id" class="rx-item">
            <div class="ri-main">
              <span class="ri-drug">{{ p.drugName || ('药品 #' + p.drugId) }}</span>
              <span class="ri-status" :class="STATUS[p.status]?.cls">{{ STATUS[p.status]?.label }}</span>
            </div>
            <div class="ri-meta">
              <span>#{{ p.id }} · {{ p.createdAt }}</span>
              <span v-if="p.status === 2 && p.reviewComment" class="ri-comment">驳回原因：{{ p.reviewComment }}</span>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.rx { padding-top: var(--sp-6); max-width: 960px; }
.rx-head { margin-bottom: var(--sp-6); }
.rx-head h1 { font-size: 36px; margin-top: 6px; }
.rx-lead { color: var(--ink-soft); font-size: 15px; margin-top: var(--sp-3); max-width: 600px; line-height: 1.7; }

.rx-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--sp-5); align-items: start; }
.upload, .history {
  background: var(--surface); border: 1px solid var(--line);
  border-radius: var(--r-lg); padding: var(--sp-5); box-shadow: var(--shadow-sm);
}
.upload h2, .history h2 { font-size: 20px; margin-bottom: var(--sp-4); }

.field { display: block; margin-bottom: var(--sp-4); }
.f-label { display: block; font-size: 13px; font-weight: 600; color: var(--ink-soft); margin-bottom: 8px; }
.f-select {
  width: 100%; padding: 11px 14px; font-size: 15px;
  border: 1px solid var(--line); border-radius: var(--r-md); background: var(--paper);
}
.f-select:focus { outline: none; border-color: var(--pine); box-shadow: 0 0 0 3px var(--pine-soft); }

.dropzone {
  display: block; cursor: pointer;
  border: 1.5px dashed var(--line-strong); border-radius: var(--r-md);
  background: var(--paper); transition: border-color var(--t-fast), background var(--t-fast);
  overflow: hidden;
}
.dropzone:hover { border-color: var(--pine); background: var(--pine-soft); }
.dropzone.filled { border-style: solid; border-color: var(--line); padding: 0; }
.dz-empty {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: var(--sp-7) var(--sp-5); color: var(--ink-faint);
}
.dz-empty span { font-size: 14.5px; font-weight: 500; color: var(--ink-soft); }
.dz-empty small { font-size: 12px; }
.preview { display: block; width: 100%; max-height: 280px; object-fit: contain; background: var(--paper-2); }
.f-file { font-size: 12.5px; color: var(--ink-faint); margin-top: 8px; }

.hint {
  font-size: 13.5px; color: var(--pine); background: var(--pine-soft);
  padding: 11px 14px; border-radius: var(--r-md); margin-bottom: var(--sp-4); line-height: 1.6;
}

.state { padding: var(--sp-6); text-align: center; color: var(--ink-faint); font-size: 14px; }
.rx-list { display: flex; flex-direction: column; gap: var(--sp-3); }
.rx-item { padding: var(--sp-4); border: 1px solid var(--line); border-radius: var(--r-md); background: var(--paper); }
.ri-main { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3); }
.ri-drug { font-weight: 600; font-size: 15px; }
.ri-status { font-size: 11.5px; font-weight: 700; padding: 3px 10px; border-radius: var(--r-pill); flex-shrink: 0; }
.ri-status.wait { background: #fbf0d8; color: #8a6516; }
.ri-status.pass { background: var(--pine-soft); color: var(--pine); }
.ri-status.reject { background: var(--clay-soft); color: #8f3e23; }
.ri-meta { margin-top: 8px; font-size: 12.5px; color: var(--ink-faint); display: flex; flex-direction: column; gap: 4px; }
.ri-comment { color: #8f3e23; }

@media (max-width: 760px) {
  .rx-grid { grid-template-columns: 1fr; }
}
</style>

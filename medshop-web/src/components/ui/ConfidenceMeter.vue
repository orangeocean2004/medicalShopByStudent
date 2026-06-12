<script setup>
import { computed } from 'vue'

/**
 * AI 置信度计量条 —— M2 智能药师的视觉亮点。
 * 把后端返回的 confidence(0~1) 画成一段会生长的弧形仪表，
 * 并按阈值变色：高=松绿(可信)、中=金棕、低=陶土橙(建议转人工)。
 */
const props = defineProps({
  value: { type: Number, default: 0 }, // 0~1
  threshold: { type: Number, default: 0.6 }
})

const pct = computed(() => Math.round(Math.max(0, Math.min(1, props.value)) * 100))

// 半圆弧：半径 52，周长的一半 ≈ 163.36
const ARC = 163.36
const dash = computed(() => (pct.value / 100) * ARC)

const level = computed(() => {
  if (props.value >= 0.8) return 'high'
  if (props.value >= props.threshold) return 'mid'
  return 'low'
})
const color = computed(() => ({ high: '#1f4d3a', mid: '#b8924a', low: '#c8643c' }[level.value]))
const label = computed(() => ({ high: '高置信', mid: '中等置信', low: '低置信' }[level.value]))
</script>

<template>
  <div class="meter">
    <svg viewBox="0 0 120 70" class="gauge">
      <!-- 轨道 -->
      <path d="M8 62 A52 52 0 0 1 112 62" fill="none"
            stroke="var(--line)" stroke-width="9" stroke-linecap="round" />
      <!-- 进度 -->
      <path d="M8 62 A52 52 0 0 1 112 62" fill="none"
            :stroke="color" stroke-width="9" stroke-linecap="round"
            :stroke-dasharray="`${dash} ${ARC}`" class="gauge-fill" />
    </svg>
    <div class="readout">
      <span class="num" :style="{ color }">{{ pct }}<small>%</small></span>
      <span class="lv" :style="{ color }">{{ label }}</span>
    </div>
  </div>
</template>

<style scoped>
.meter { display: inline-flex; flex-direction: column; align-items: center; width: 132px; }
.gauge { width: 132px; height: auto; overflow: visible; }
.gauge-fill {
  transition: stroke-dasharray 900ms var(--ease-out), stroke 400ms var(--ease);
}
.readout { margin-top: -16px; text-align: center; line-height: 1; }
.num { font-family: var(--font-display); font-size: 30px; font-weight: 600; }
.num small { font-size: 14px; font-weight: 600; }
.lv { display: block; font-size: 12px; font-weight: 700; margin-top: 4px; letter-spacing: 0.04em; }
</style>

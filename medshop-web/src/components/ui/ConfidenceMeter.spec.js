import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfidenceMeter from '@/components/ui/ConfidenceMeter.vue'

/**
 * ConfidenceMeter 组件单元测试（M2 视觉亮点）：
 * 置信度→百分比换算与钳制、按阈值分档显示文案/颜色、弧长 dash 随值生长。
 */
describe('ConfidenceMeter.vue', () => {
  const ARC = 163.36

  it('value=0.74 → 显示 74%', () => {
    const w = mount(ConfidenceMeter, { props: { value: 0.74 } })
    expect(w.find('.num').text()).toContain('74')
  })

  it('高置信（>=0.8）：显示「高置信」', () => {
    const w = mount(ConfidenceMeter, { props: { value: 0.9 } })
    expect(w.find('.lv').text()).toBe('高置信')
  })

  it('中置信（>=threshold 且 <0.8）：显示「中等置信」', () => {
    const w = mount(ConfidenceMeter, { props: { value: 0.7, threshold: 0.6 } })
    expect(w.find('.lv').text()).toBe('中等置信')
  })

  it('低置信（<threshold）：显示「低置信」（建议转人工场景）', () => {
    const w = mount(ConfidenceMeter, { props: { value: 0.3, threshold: 0.6 } })
    expect(w.find('.lv').text()).toBe('低置信')
  })

  it('自定义阈值：抬高 threshold 使原本中档变低档', () => {
    const w = mount(ConfidenceMeter, { props: { value: 0.7, threshold: 0.75 } })
    expect(w.find('.lv').text()).toBe('低置信')
  })

  it('百分比钳制：>1 按 100%，<0 按 0%', () => {
    expect(mount(ConfidenceMeter, { props: { value: 1.5 } }).find('.num').text()).toContain('100')
    expect(mount(ConfidenceMeter, { props: { value: -0.3 } }).find('.num').text()).toContain('0')
  })

  it('进度弧 dasharray 随置信度生长：满值时 dash≈弧长', () => {
    const w = mount(ConfidenceMeter, { props: { value: 1 } })
    const fill = w.find('.gauge-fill')
    expect(fill.attributes('stroke-dasharray')).toBe(`${ARC} ${ARC}`)
  })

  it('低置信用陶土橙、高置信用松绿', () => {
    const low = mount(ConfidenceMeter, { props: { value: 0.2 } })
    expect(low.find('.gauge-fill').attributes('stroke')).toBe('#c8643c')
    const high = mount(ConfidenceMeter, { props: { value: 0.95 } })
    expect(high.find('.gauge-fill').attributes('stroke')).toBe('#1f4d3a')
  })
})

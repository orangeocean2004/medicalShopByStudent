import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCartStore } from '@/stores/cart'

/**
 * 购物车 store 单元测试：加购合并、数量下限、增删清空、count/total/hasRx 计算属性、
 * localStorage 持久化。每例前重置 pinia 与 localStorage，互不污染。
 */
describe('cart store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  const otc = { id: 5001, name: '感冒灵', price: 18, isRx: 0 }
  const rx = { id: 5004, name: '阿莫西林', price: 25, isRx: 1 }

  it('初始为空：count/total 为 0，hasRx 为 false', () => {
    const cart = useCartStore()
    expect(cart.items).toEqual([])
    expect(cart.count).toBe(0)
    expect(cart.total).toBe(0)
    expect(cart.hasRx).toBe(false)
  })

  it('加购新药品：进入列表并带默认数量 1', () => {
    const cart = useCartStore()
    cart.add(otc)
    expect(cart.items).toHaveLength(1)
    expect(cart.items[0]).toMatchObject({ id: 5001, name: '感冒灵', price: 18, quantity: 1 })
  })

  it('重复加购同一药品：数量累加而非新增条目', () => {
    const cart = useCartStore()
    cart.add(otc, 2)
    cart.add(otc, 3)
    expect(cart.items).toHaveLength(1)
    expect(cart.items[0].quantity).toBe(5)
  })

  it('count 累计所有条目数量，total 为单价×数量之和', () => {
    const cart = useCartStore()
    cart.add(otc, 2) // 18 * 2 = 36
    cart.add(rx, 1) // 25 * 1 = 25
    expect(cart.count).toBe(3)
    expect(cart.total).toBe(61)
  })

  it('hasRx：含处方药时为 true', () => {
    const cart = useCartStore()
    cart.add(otc)
    expect(cart.hasRx).toBe(false)
    cart.add(rx)
    expect(cart.hasRx).toBe(true)
  })

  it('setQuantity：更新指定药品数量', () => {
    const cart = useCartStore()
    cart.add(otc)
    cart.setQuantity(5001, 4)
    expect(cart.items[0].quantity).toBe(4)
  })

  it('setQuantity：数量下限为 1（传入 0 或负数被钳到 1）', () => {
    const cart = useCartStore()
    cart.add(otc)
    cart.setQuantity(5001, 0)
    expect(cart.items[0].quantity).toBe(1)
    cart.setQuantity(5001, -5)
    expect(cart.items[0].quantity).toBe(1)
  })

  it('setQuantity：对不存在的药品 id 不报错且无副作用', () => {
    const cart = useCartStore()
    cart.add(otc)
    cart.setQuantity(9999, 3)
    expect(cart.items[0].quantity).toBe(1)
  })

  it('remove：移除指定药品', () => {
    const cart = useCartStore()
    cart.add(otc)
    cart.add(rx)
    cart.remove(5001)
    expect(cart.items).toHaveLength(1)
    expect(cart.items[0].id).toBe(5004)
  })

  it('clear：清空购物车', () => {
    const cart = useCartStore()
    cart.add(otc)
    cart.add(rx)
    cart.clear()
    expect(cart.items).toEqual([])
    expect(cart.count).toBe(0)
  })

  it('持久化：加购后写入 localStorage', () => {
    const cart = useCartStore()
    cart.add(otc, 2)
    const saved = JSON.parse(localStorage.getItem('medshop_cart'))
    expect(saved).toHaveLength(1)
    expect(saved[0].quantity).toBe(2)
  })

  it('初始化：从 localStorage 恢复已存购物车', () => {
    localStorage.setItem(
      'medshop_cart',
      JSON.stringify([{ id: 5001, name: '感冒灵', price: 18, isRx: 0, quantity: 3 }])
    )
    const cart = useCartStore()
    expect(cart.items).toHaveLength(1)
    expect(cart.count).toBe(3)
    expect(cart.total).toBe(54)
  })
})

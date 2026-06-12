import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 购物车：纯前端持久化（演示用）。下单时把内含的处方药交后端拦截校验。
 * 每项结构：{ id, name, price, isRx, quantity }
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref(JSON.parse(localStorage.getItem('medshop_cart') || '[]'))

  const count = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const total = computed(() =>
    items.value.reduce((s, i) => s + i.price * i.quantity, 0)
  )
  const hasRx = computed(() => items.value.some((i) => i.isRx === 1))

  function persist() {
    localStorage.setItem('medshop_cart', JSON.stringify(items.value))
  }

  function add(drug, quantity = 1) {
    const found = items.value.find((i) => i.id === drug.id)
    if (found) {
      found.quantity += quantity
    } else {
      items.value.push({
        id: drug.id,
        name: drug.name,
        price: drug.price,
        isRx: drug.isRx,
        quantity
      })
    }
    persist()
  }

  function setQuantity(id, quantity) {
    const it = items.value.find((i) => i.id === id)
    if (it) {
      it.quantity = Math.max(1, quantity)
      persist()
    }
  }

  function remove(id) {
    items.value = items.value.filter((i) => i.id !== id)
    persist()
  }

  function clear() {
    items.value = []
    persist()
  }

  return { items, count, total, hasRx, add, setQuantity, remove, clear }
})

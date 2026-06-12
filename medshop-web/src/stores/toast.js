import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 全局轻提示。type: 'info' | 'success' | 'error' */
export const useToastStore = defineStore('toast', () => {
  const list = ref([])
  let seq = 0

  function push(message, type = 'info', duration = 2600) {
    const id = ++seq
    list.value.push({ id, message, type })
    setTimeout(() => dismiss(id), duration)
  }

  function dismiss(id) {
    list.value = list.value.filter((t) => t.id !== id)
  }

  const success = (m) => push(m, 'success')
  const error = (m) => push(m, 'error')
  const info = (m) => push(m, 'info')

  return { list, push, dismiss, success, error, info }
})

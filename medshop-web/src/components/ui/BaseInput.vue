<script setup>
defineProps({
  modelValue: [String, Number],
  label: String,
  type: { type: String, default: 'text' },
  placeholder: String,
  icon: Boolean
})
defineEmits(['update:modelValue'])
</script>

<template>
  <label class="field">
    <span v-if="label" class="field-label">{{ label }}</span>
    <span class="field-box">
      <span v-if="icon" class="field-icon"><slot name="icon" /></span>
      <input
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :class="{ 'has-icon': icon }"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    </span>
  </label>
</template>

<style scoped>
.field { display: block; }
.field-label {
  display: block;
  font-size: 13px; font-weight: 600;
  color: var(--ink-soft);
  margin-bottom: 7px;
  letter-spacing: 0.01em;
}
.field-box { position: relative; display: block; }
.field-icon {
  position: absolute; left: 15px; top: 50%; transform: translateY(-50%);
  color: var(--ink-faint); display: grid; place-items: center;
  pointer-events: none;
}
input {
  width: 100%;
  padding: 13px 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  font-size: 15px;
  transition: border-color var(--t-fast) var(--ease), box-shadow var(--t-fast) var(--ease), background var(--t-fast);
}
input.has-icon { padding-left: 44px; }
input::placeholder { color: var(--ink-faint); }
input:hover { border-color: var(--line-strong); }
input:focus {
  outline: none;
  border-color: var(--pine);
  box-shadow: 0 0 0 3px var(--pine-soft);
  background: #fff;
}
</style>

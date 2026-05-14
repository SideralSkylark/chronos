<template>
  <div
    :class="[
      mergedHeader && collapsed ? 'sticky top-[44px] z-10 mb-0' : 'mb-5'
    ]"
  >
    <div
      class="bg-white border-slate-200"
      :class="[
        mergedHeader && collapsed
          ? 'rounded-none border-x-0 border-t-0 border-b border-l-[3px] border-l-blue-800 px-5 py-2 shadow-sm'
          : 'rounded-[10px] border px-5 py-4 shadow-sm'
      ]"
    >
      <div class="flex flex-wrap items-end gap-4">
        <slot name="filters" :collapsed="collapsed" />
        <div class="flex-1 flex items-end justify-end">
          <slot name="actions" :collapsed="collapsed">
            <button
              v-if="activeFilterCount > 0"
              @click="$emit('clear')"
              class="h-8 flex items-center gap-1.5 px-3 border border-gray-200 text-xs text-gray-500 rounded-lg hover:bg-gray-50 transition"
            >
              <X class="w-3.5 h-3.5" />
              Limpar filtros
              <span class="bg-blue-100 text-blue-800 text-xs rounded-full w-4 h-4 flex items-center justify-center font-medium leading-none ml-1">
                {{ activeFilterCount }}
              </span>
            </button>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { X } from 'lucide-vue-next'

defineProps<{
  activeFilterCount: number
  collapsed?: boolean
  mergedHeader?: boolean
}>()

defineEmits<{
  (e: 'clear'): void
}>()
</script>

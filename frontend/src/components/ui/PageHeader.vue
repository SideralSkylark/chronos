<template>
  <div
    :class="[
      sticky ? 'sticky top-0 z-20 bg-transparent' : 'mb-6',
      (sticky && !collapsed) ? 'mb-6' : ''
    ]"
  >
    <div
      class="page-header-card bg-white border-l-[3px] border-l-blue-800"
      :class="[
        !collapsed
          ? 'rounded-[10px] border border-slate-200 shadow-sm'
          : 'rounded-none'
      ]"
    >
      <div
        class="flex items-center justify-between transition-all duration-200"
        :class="[
          collapsed ? 'py-2 px-5' : 'p-5',
          (collapsed && mergedFilter) ? 'border-b border-slate-200' : '',
          (collapsed && !mergedFilter) ? 'border-b border-black/5 shadow-[0_1px_0_0_rgba(0,0,0,0.06)]' : ''
        ]"
      >
        <div class="flex items-center gap-3">
          <div
            class="bg-blue-50 rounded-md transition-all duration-200"
            :class="collapsed ? 'p-1.5' : 'p-2'"
          >
            <component
              :is="icon"
              class="text-blue-800 transition-all duration-200"
              :class="collapsed ? 'w-4 h-4' : 'w-5 h-5'"
            />
          </div>
          <div>
            <h1
              class="font-semibold text-gray-900 transition-all duration-200"
              :class="collapsed ? 'text-sm' : 'text-xl'"
            >
              {{ title }}
            </h1>
            <p
              class="text-gray-400 text-sm overflow-hidden"
              :style="{
                maxHeight: collapsed ? '0' : '40px',
                opacity: collapsed ? '0' : '1',
                transition: 'max-height 0.2s ease, opacity 0.2s ease'
              }"
            >
              {{ subtitle }}
            </p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <slot name="actions" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  icon: Component
  title: string
  subtitle: string
  sticky?: boolean
  collapsed?: boolean
  mergedFilter?: boolean
}>()
</script>

<style scoped>
.page-header-card {
  transition: padding 0.2s ease, border-radius 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
</style>

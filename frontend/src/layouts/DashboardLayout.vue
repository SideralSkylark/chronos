<template>
  <div class="flex h-screen bg-slate-100">
    <Sidebar />
    <main
      class="flex-1 h-full"
      :class="
        route.meta.hasStickyHeader
          ? 'overflow-hidden'
          : 'overflow-y-auto px-6 py-6'
      "
    >
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import Sidebar from '@/components/layout/Sidebar.vue'
import { RouterView, useRoute } from 'vue-router'

const route = useRoute()
</script>

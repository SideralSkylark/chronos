<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 bg-black/40 z-50 transition-opacity"
    @click="$emit('update:modelValue', false)"
  ></div>

  <Transition name="notif-slide">
    <div
      v-if="modelValue"
      class="fixed right-0 top-0 h-full w-80 bg-white shadow-xl rounded-l-[10px] z-[60] flex flex-col overflow-hidden"
      @click.stop
    >
      <!-- Header -->
      <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900">Notificações</h3>
        <button
          @click="$emit('update:modelValue', false)"
          class="text-gray-400 hover:text-gray-600 transition p-1"
        >
          <X class="w-4 h-4" />
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto">
        <!-- Loading -->
        <div v-if="notificationStore.loading" class="flex flex-col items-center justify-center py-12 gap-3">
          <Loader2 class="w-5 h-5 animate-spin text-blue-900" />
        </div>

        <!-- Empty state -->
        <div
          v-else-if="notificationStore.notifications.length === 0"
          class="flex flex-col items-center justify-center py-12 gap-2"
        >
          <CalendarDays class="w-6 h-6 text-gray-300" />
          <p class="text-xs text-gray-400">Sem notificações</p>
        </div>

        <!-- List -->
        <div v-else>
          <div
            v-for="notification in notificationStore.notifications"
            :key="notification.id"
            class="px-4 py-3 border-b border-gray-50 flex gap-3 items-start transition-colors"
            :class="{ 'bg-blue-50/40': !notification.read }"
          >
            <div
              class="mt-1.5 w-1.5 h-1.5 rounded-full shrink-0"
              :class="notification.read ? 'bg-transparent' : 'bg-blue-500'"
            ></div>
            <div class="flex flex-col gap-0.5 min-w-0">
              <p class="text-xs text-gray-700 leading-normal">{{ notification.message }}</p>
              <span class="text-[10px] text-gray-400 font-medium">
                {{ relativeTime(notification.createdAt) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { X, Loader2, CalendarDays } from 'lucide-vue-next'
import { useNotificationStore } from '@/stores/notification'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const notificationStore = useNotificationStore()

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      notificationStore.fetchNotifications()
      setTimeout(() => {
        notificationStore.markAllRead()
      }, 1000)
    }
  }
)

function relativeTime(isoString: string): string {
  const diff = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000)
  if (diff < 60) return 'agora mesmo'
  if (diff < 3600) return `há ${Math.floor(diff / 60)} min`
  if (diff < 86400) return `há ${Math.floor(diff / 3600)}h`
  return `há ${Math.floor(diff / 86400)}d`
}
</script>

<style scoped>
.notif-slide-enter-active,
.notif-slide-leave-active {
  transition: transform 0.25s ease;
}
.notif-slide-enter-from,
.notif-slide-leave-to {
  transform: translateX(100%);
}
</style>
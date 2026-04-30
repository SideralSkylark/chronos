<template>
  <div>
    <div
      v-if="modelValue"
      class="fixed inset-0 bg-black/40 z-50 transition-opacity"
      @click="$emit('update:modelValue', false)"
    ></div>

    <Transition name="notif-slide">
      <div
        v-if="modelValue"
        class="fixed right-0 top-0 h-full w-80 bg-white shadow-sm border-l border-gray-100 z-[60] flex flex-col overflow-hidden rounded-l-[10px]"
        @click.stop
      >
        <!-- Header -->
        <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-gray-900">Notificações</span>
            <span
              v-if="notificationStore.unreadCount > 0"
              class="bg-blue-900 text-white text-[10px] font-medium rounded-full min-w-[18px] h-[18px] px-1 flex items-center justify-center leading-none"
            >
              {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
            </span>
          </div>

          <div class="flex items-center gap-3">
            <button
              v-if="notificationStore.unreadCount > 0"
              @click="notificationStore.markAllRead()"
              class="h-6 flex items-center gap-1 px-2.5 text-[10px] font-medium text-blue-900 bg-blue-50 border border-blue-100 rounded-md hover:bg-blue-100 transition"
            >
              Marcar todas
            </button>
            <button
              @click="$emit('update:modelValue', false)"
              class="text-gray-400 hover:text-gray-600 transition p-1"
            >
              <X class="w-4 h-4" />
            </button>
          </div>
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
            class="flex flex-col items-center justify-center flex-1 gap-3 py-16"
          >
            <div class="bg-gray-50 rounded-full p-3">
              <BellOff class="w-5 h-5 text-gray-300" />
            </div>
            <p class="text-xs text-gray-400">Sem notificações</p>
          </div>

          <!-- List -->
          <div v-else>
            <div
              v-for="notification in notificationStore.notifications"
              :key="notification.id"
              class="group px-4 py-3 border-b border-gray-50 flex flex-col gap-1.5 transition-colors"
              :class="[
                notification.read
                  ? 'bg-white hover:bg-gray-50'
                  : 'bg-blue-50/30 hover:bg-blue-50/60',
              ]"
            >
              <div class="flex items-start gap-2.5">
                <div
                  class="mt-1 shrink-0 w-2 h-2 rounded-full"
                  :class="notification.read ? 'bg-transparent' : 'bg-blue-900'"
                ></div>
                <p class="text-xs text-gray-700 leading-relaxed flex-1">
                  {{ notification.message }}
                </p>
              </div>
              <div class="flex items-center justify-between pl-4">
                <span class="text-[10px] text-gray-400">
                  {{ relativeTime(notification.createdAt) }}
                </span>
                <button
                  v-if="!notification.read"
                  @click="markOneRead(notification.id)"
                  class="text-[10px] text-blue-900 font-medium hover:underline underline-offset-2 transition"
                >
                  Marcar como lida
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div
          v-if="notificationStore.notifications.length > 0"
          class="px-4 py-2.5 border-t border-gray-100 text-center"
        >
          <span class="text-[10px] text-gray-500">
            {{ notificationStore.notifications.length }} notificação(ões) no total
          </span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { X, Loader2, BellOff } from 'lucide-vue-next'
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
    }
  }
)

async function markOneRead(id: number) {
  await notificationStore.markAsRead(id)
}

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

<template>
  <div>
    <div
      v-if="modelValue"
      class="fixed inset-0 bg-black/30 backdrop-blur-[1px] z-50 transition-opacity"
      @click="$emit('update:modelValue', false)"
    ></div>

    <Transition name="notif-slide">
      <div
        v-if="modelValue"
        class="fixed right-0 top-0 h-full w-[380px] bg-white shadow-2xl border-l border-gray-100 z-[60] flex flex-col overflow-hidden rounded-l-2xl"
        @click.stop
      >
        <!-- Header -->
        <div class="px-6 py-5 border-b border-gray-100 flex items-center justify-between bg-white sticky top-0 z-10">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-lg bg-blue-50 border border-blue-100 shadow-sm">
              <Bell class="w-5 h-5 text-blue-900" />
            </div>
            <div>
              <h2 class="text-sm font-bold text-gray-900">Notificações</h2>
              <p class="text-[10px] text-gray-400 font-medium uppercase tracking-wider">Centro de actualizações</p>
            </div>
          </div>

          <button
            @click="$emit('update:modelValue', false)"
            class="text-gray-400 hover:text-gray-600 hover:bg-gray-50 transition p-2 rounded-full"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- Actions Bar -->
        <div 
          v-if="notificationStore.notifications.length > 0"
          class="px-6 py-2 bg-gray-50/50 border-b border-gray-50 flex items-center justify-between"
        >
          <div class="flex items-center gap-2">
            <span
              v-if="notificationStore.unreadCount > 0"
              class="bg-blue-900 text-white text-[10px] font-bold rounded-full px-2 py-0.5 shadow-sm"
            >
              {{ notificationStore.unreadCount }} novas
            </span>
            <span v-else class="text-[10px] font-bold text-gray-400 uppercase tracking-tight">Tudo em dia</span>
          </div>
          <div class="flex items-center gap-2">
            <button
              v-if="notificationStore.unreadCount > 0"
              @click="notificationStore.markAllRead()"
              class="text-[10px] font-bold text-blue-700 hover:text-blue-900 px-2 py-1 rounded transition"
            >
              Marcar lidas
            </button>
            <button
              @click="notificationStore.clearRead()"
              class="text-[10px] font-bold text-gray-500 hover:text-red-600 px-2 py-1 rounded transition"
            >
              Limpar lidas
            </button>
          </div>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto custom-scrollbar">
          <!-- Loading -->
          <div v-if="notificationStore.loading && notificationStore.notifications.length === 0" class="flex flex-col items-center justify-center py-24 gap-4">
            <div class="w-12 h-12 rounded-full border-2 border-blue-100 border-t-blue-900 animate-spin"></div>
            <p class="text-xs font-bold text-gray-400 uppercase tracking-widest">A carregar...</p>
          </div>

          <!-- Empty state -->
          <div
            v-else-if="notificationStore.notifications.length === 0"
            class="flex flex-col items-center justify-center flex-1 gap-4 py-24 px-12 text-center"
          >
            <div class="w-20 h-20 bg-gray-50 rounded-full flex items-center justify-center border border-gray-100 shadow-inner">
              <BellOff class="w-10 h-10 text-gray-200" />
            </div>
            <div class="space-y-1">
              <h3 class="text-sm font-bold text-gray-900">Sem notificações</h3>
              <p class="text-xs text-gray-400 leading-relaxed">Não existem mensagens ou actualizações do sistema neste momento.</p>
            </div>
          </div>

          <!-- List -->
          <div v-else class="divide-y divide-gray-50">
            <div
              v-for="notification in notificationStore.notifications"
              :key="notification.id"
              class="group px-6 py-4 flex items-start gap-4 transition-all duration-300 relative border-l-4"
              :class="[
                notification.read
                  ? 'bg-white border-transparent hover:bg-gray-50'
                  : 'bg-blue-50/20 border-blue-600 hover:bg-blue-50/40',
              ]"
            >
              <!-- Icon Bubble -->
              <div class="shrink-0 mt-0.5">
                <div 
                  class="w-10 h-10 rounded-xl bg-white border border-gray-100 shadow-sm flex items-center justify-center group-hover:scale-105 transition-transform duration-300"
                >
                  <component 
                    :is="getIcon(notification.message)" 
                    class="w-5 h-5" 
                    :class="notification.read ? 'text-gray-400' : 'text-blue-900'"
                  />
                </div>
              </div>

              <!-- Content -->
              <div class="flex-1 min-w-0 space-y-1">
                <div class="flex justify-between items-start gap-2">
                  <p 
                    class="text-[13px] leading-snug break-words"
                    :class="notification.read ? 'text-gray-500' : 'text-gray-900 font-medium'"
                  >
                    {{ notification.message }}
                  </p>
                  <button
                    @click="notificationStore.dismiss(notification.id)"
                    class="p-1 text-gray-300 hover:text-red-500 transition-colors rounded hover:bg-red-50 opacity-0 group-hover:opacity-100"
                    title="Eliminar"
                  >
                    <Trash2 class="w-3.5 h-3.5" />
                  </button>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-[10px] font-bold text-gray-400 uppercase tracking-tighter">
                    {{ relativeTime(notification.createdAt) }}
                  </span>
                  <div v-if="!notification.read" class="w-1 h-1 rounded-full bg-blue-600"></div>
                  <button
                    v-if="!notification.read"
                    @click="notificationStore.markAsRead(notification.id)"
                    class="text-[10px] font-bold text-blue-700 hover:text-blue-900 uppercase tracking-tighter"
                  >
                    Marcar lida
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div
          v-if="notificationStore.notifications.length > 0"
          class="px-6 py-4 border-t border-gray-100 bg-white"
        >
          <button 
            @click="notificationStore.fetchNotifications()"
            class="w-full py-2 bg-gray-50 text-gray-500 text-[10px] font-bold uppercase tracking-widest rounded-lg border border-gray-100 hover:bg-gray-100 transition-colors shadow-sm"
          >
            Actualizar lista
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { 
  X, 
  Loader2, 
  BellOff, 
  Bell, 
  Trash2, 
  Info, 
  CheckCircle, 
  AlertTriangle, 
  Calendar,
  Zap,
  Moon,
  Sun,
  User,
  LayoutDashboard
} from 'lucide-vue-next'
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

function getIcon(message: string) {
  const msg = message.toLowerCase()
  if (msg.includes('horário') || msg.includes('calendário')) return Calendar
  if (msg.includes('gerar') || msg.includes('optimiza')) return Zap
  if (msg.includes('erro') || msg.includes('falha')) return AlertTriangle
  if (msg.includes('sucesso') || msg.includes('concluído')) return CheckCircle
  if (msg.includes('manhã')) return Sun
  if (msg.includes('tarde') || msg.includes('noite')) return Moon
  if (msg.includes('utilizador') || msg.includes('perfil')) return User
  return Info
}

function relativeTime(isoString: string): string {
  const diff = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000)
  if (diff < 60) return 'agora'
  if (diff < 3600) return `${Math.floor(diff / 60)}m atrás`
  if (diff < 86400) return `${Math.floor(diff / 3600)}h atrás`
  return `${Math.floor(diff / 86400)}d atrás`
}
</script>

<style scoped>
.notif-slide-enter-active,
.notif-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.notif-slide-enter-from,
.notif-slide-leave-to {
  transform: translateX(100%);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #f1f1f1;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #e2e8f0;
}
</style>
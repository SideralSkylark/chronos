<template>
  <aside class="w-60 bg-slate-50 border-r border-slate-200 flex flex-col">
    <!-- Brand -->
    <div class="px-5 py-5 border-b border-slate-200">
      <div class="flex items-center gap-3">
        <div class="bg-blue-50 p-2 rounded-md">
          <GraduationCap class="w-5 h-5 text-blue-800" />
        </div>
        <div>
          <h2 class="text-sm font-semibold text-gray-900">UCM-FEG</h2>
          <p class="text-[10px] text-blue-800 font-bold uppercase tracking-wider">Sistema de gestão</p>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 py-4 space-y-0.5">
      <RouterLink v-for="item in allowedRoutes" :key="item.name" :to="{ name: item.name }"
        class="relative overflow-hidden group flex items-center gap-2.5 px-2.5 py-2 text-sm rounded-md transition-colors"
        :class="isActive(item)
          ? 'bg-blue-50 text-blue-800 font-semibold'
          : 'text-gray-500 hover:bg-gray-50 hover:text-gray-900'">
        <div v-if="isActive(item)"
          class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-[22px] bg-blue-800 rounded-r"></div>
        <component :is="item.icon" class="w-4 h-4 flex-shrink-0"
          :class="isActive(item) ? 'text-blue-800' : 'text-gray-400 group-hover:text-blue-900'" />
        <span class="truncate font-medium">{{ item.label }}</span>
        <ChevronRight v-if="isActive(item)" class="w-3.5 h-3.5 ml-auto text-blue-400" />
      </RouterLink>
    </nav>

    <!-- User footer -->
    <div class="px-3 py-3 border-t border-slate-200">
      <!-- User info -->
      <div class="flex items-center gap-2.5 px-2.5 py-2 rounded-md mb-0.5
        bg-slate-100 border border-slate-200 cursor-pointer
        hover:bg-slate-200 hover:border-slate-300 transition-colors" @click="showNotifications = true">
        <div class="relative w-7 h-7 shrink-0 cursor-pointer">
          <div class="w-7 h-7 rounded-full bg-blue-900 flex items-center justify-center">
            <span class="text-xs font-medium text-white leading-none select-none">
              {{ userInitials }}
            </span>
          </div>
          <span v-if="notificationStore.unreadCount > 0" class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full
            bg-blue-500 border-2 border-slate-100 pointer-events-none z-10" />
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-xs font-medium text-gray-800 truncate">
            {{ auth.user?.username ?? 'Utilizador' }}
          </p>
          <p class="text-[10px] text-blue-800 font-bold uppercase tracking-wider truncate">
            {{ userRoleLabel }}
          </p>
        </div>
      </div>
      <!-- Logout -->
      <button @click="logout"
        class="w-full flex items-center gap-2.5 px-2.5 py-2 text-sm rounded-md text-gray-500 hover:bg-red-50 hover:text-red-600 transition-colors group">
        <LogOut class="w-4 h-4 text-gray-400 group-hover:text-red-500 transition-colors" />
        <span class="font-medium">Terminar sessão</span>
      </button>
    </div>
    <NotificationsModal v-model="showNotifications" />
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import NotificationsModal from '@/components/ui/NotificationsModal.vue'
import {
  LayoutDashboard,
  DoorOpen,
  GraduationCap,
  Users as UsersIcon,
  BookOpen,
  LogOut,
  ChevronRight,
  CalendarDays,
  CalendarCheck,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const showNotifications = ref(false)

const roleLabels: Record<string, string> = {
  ADMIN: 'Administrador',
  COORDINATOR: 'Coordenador',
  DIRECTOR: 'Diretor',
  ASISTENT: 'Assistente',
  STUDENT: 'Estudante',
  TEACHER: 'Docente',
  USER: 'Utilizador',
}

const userInitials = computed(() => {
  const name = auth.user?.username ?? ''
  return name.slice(0, 2).toUpperCase()
})

const userRoleLabel = computed(() => {
  const role = userStore.myHighestRole
  return roleLabels[role] ?? role
})

const dashboardRoutes = [
  { name: 'DashboardHome', label: 'Início', icon: LayoutDashboard, roles: ['ADMIN', 'ASISTENT', 'DIRECTOR'] },
  { name: 'Rooms', label: 'Salas', icon: DoorOpen, roles: ['ADMIN', 'ASISTENT', 'DIRECTOR'] },
  { name: 'Courses', label: 'Cursos', icon: GraduationCap, roles: ['ADMIN', 'COORDINATOR'] },
  { name: 'Cohorts', label: 'Turmas', icon: BookOpen, roles: ['ADMIN', 'COORDINATOR', 'ASISTENT', 'DIRECTOR'] },
  { name: 'Users', label: 'Utilizadores', icon: UsersIcon, roles: ['ADMIN', 'ASISTENT', 'DIRECTOR'] },
  { name: 'Timetable', label: 'Horários', icon: CalendarDays, roles: ['ADMIN', 'COORDINATOR', 'ASISTENT', 'DIRECTOR'] },
  { name: 'MyTimetableView', label: 'Meu Horário', icon: CalendarCheck, roles: ['STUDENT', 'TEACHER'] },
]

const allowedRoutes = computed(() => {
  const roles = auth.user?.roles ?? []
  return dashboardRoutes.filter(item =>
    item.roles.some(role => roles.includes(role))
  )
})

const isActive = (item: typeof dashboardRoutes[number]) => route.name === item.name

const logout = async () => {
  notificationStore.stopPolling()
  await auth.logout()
  router.push({ name: 'Login' })
}

onMounted(() => {
  notificationStore.fetchUnreadCount()
})
</script>

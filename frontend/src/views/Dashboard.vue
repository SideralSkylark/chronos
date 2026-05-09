<template>
  <div>
    <PageHeader
      :icon="LayoutDashboard"
      title="Painel de controlo"
      subtitle="Resumo geral do sistema e acessos rápidos"
    />

    <div class="space-y-4">
      <!-- Welcome Card -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-900 border border-blue-100 shadow-sm">
            <UserIcon class="w-8 h-8" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-gray-900">Olá, {{ auth.username }}!</h2>
            <p class="text-gray-500 text-sm">Bem-vindo de volta ao sistema de gestão de horários.</p>
            <div class="flex flex-wrap gap-1.5 mt-2">
              <span
                v-for="role in auth.roles"
                :key="role"
                class="px-2 py-0.5 bg-gray-100 text-gray-600 rounded-md text-[10px] font-bold uppercase tracking-wider"
              >
                {{ roleLabel(role) }}
              </span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <router-link
            v-if="hasTimetable"
            to="/dashboard/my-timetable"
            class="px-4 py-2 bg-blue-900 text-white rounded-lg text-sm font-medium hover:bg-blue-800 transition shadow-sm flex items-center gap-2"
          >
            <Calendar class="w-4 h-4" />
            Ver o meu horário
          </router-link>
        </div>
      </div>

      <!-- Stats Grid (only for staff/admin) -->
      <div v-if="isStaff" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div
          v-for="stat in stats"
          :key="stat.label"
          class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 transition group relative overflow-hidden h-32 flex flex-col justify-between hover:border-blue-200"
        >
          <div class="relative z-10">
            <span v-if="statValues.loading" class="animate-pulse bg-gray-100 h-8 w-16 rounded block"></span>
            <span v-else class="text-2xl font-bold text-gray-900 leading-none">{{ stat.value }}</span>
            <p class="text-xs text-gray-500 mt-1.5 font-medium uppercase tracking-wider">{{ stat.label }}</p>
          </div>
          
          <div class="relative z-10">
            <router-link
              :to="'/dashboard' + stat.link"
              class="text-[11px] text-blue-600 hover:text-blue-800 font-bold flex items-center gap-1 group-hover:translate-x-0.5 transition-transform"
            >
              Gerir →
            </router-link>
          </div>

          <component 
            :is="stat.icon" 
            class="absolute -right-6 -bottom-6 w-[110px] h-[110px] opacity-[0.06] text-gray-900 group-hover:scale-110 group-hover:opacity-[0.09] transition-all duration-500 transform rotate-12" 
          />
        </div>
      </div>

      <!-- Feasibility diagnostics (staff only) -->
      <DashboardInsights v-if="isStaff" />

      <!-- Main Content Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <!-- Quick Access -->
        <div class="lg:col-span-2 space-y-4">
          <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Acessos Rápidos</h3>
          <div class="bg-white p-4 rounded-[10px] border border-gray-100 shadow-sm">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <router-link
                v-for="action in quickActions"
                :key="action.title"
                :to="action.link"
                class="p-3 rounded-[10px] bg-gray-50/80 border border-transparent hover:border-gray-200 transition group flex items-start gap-3"
              >
                <div class="p-2.5 rounded-lg shrink-0 bg-white border border-gray-100 shadow-sm">
                  <component :is="action.icon" class="w-5 h-5" :class="action.color.split(' ')[1]" />
                </div>
                <div class="min-w-0">
                  <h4 class="text-sm font-bold text-gray-900 group-hover:text-blue-900 transition leading-tight">{{ action.title }}</h4>
                  <p class="text-[10px] text-gray-500 mt-0.5 line-clamp-1">{{ action.description }}</p>
                </div>
              </router-link>
            </div>
          </div>
        </div>

        <!-- System Status -->
        <div class="space-y-4">
          <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Estado do Sistema</h3>
          <div class="bg-white rounded-[10px] border border-gray-100 shadow-sm p-5 space-y-4">
            <div class="flex items-center justify-between text-xs">
              <span class="text-gray-500">Estado da Geração</span>
              <span class="font-bold text-green-700 flex items-center gap-1.5 px-2 py-0.5 bg-green-50 rounded-full border border-green-100">
                <div class="w-1.5 h-1.5 rounded-full bg-green-600 animate-pulse"></div>
                Operacional
              </span>
            </div>

            <div class="space-y-2.5 pt-3 border-t border-gray-50">
              <div class="flex justify-between items-center text-xs">
                <span class="text-gray-500">Período Activo</span>
                <span class="font-bold text-gray-900">2025/2026 · 1º Sem.</span>
              </div>
              <div class="flex justify-between items-center text-xs">
                <span class="text-gray-500">Segurança</span>
                <span class="font-bold text-gray-900">Sessão JWT</span>
              </div>
            </div>

            <div class="bg-blue-900 rounded-lg p-3.5 mt-4 text-white overflow-hidden relative group cursor-pointer shadow-md">
              <div class="relative z-10">
                <p class="text-[9px] font-bold text-blue-200 uppercase tracking-widest mb-0.5">Versão do Sistema</p>
                <p class="text-lg font-bold">v2.4.0-Refactor</p>
                <p class="text-[8px] text-blue-300/80 mt-1 font-mono">#20260410.1-prod</p>
              </div>
              <div class="absolute right-2 bottom-1/2 translate-y-1/2 opacity-20 group-hover:scale-110 transition duration-500 transform">
                <LayoutDashboard class="w-8 h-8" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { useRoomStore } from '@/stores/room'
import { useCourseStore } from '@/stores/course'
import { useCohortStore } from '@/stores/cohorts'
import DashboardInsights from '@/components/dashboard/DashboardInsights.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import {
  LayoutDashboard,
  User as UserIcon,
  Users,
  DoorOpen,
  BookOpen,
  GraduationCap,
  Calendar,
  ArrowRight,
  Info,
  ShieldCheck,
  Zap,
} from 'lucide-vue-next'

const auth = useAuthStore()
const userStore = useUserStore()
const roomStore = useRoomStore()
const courseStore = useCourseStore()
const cohortStore = useCohortStore()

const isStaff = computed(() =>
  auth.roles.some(r => ['ADMIN', 'DIRECTOR', 'ASISTENT', 'COORDINATOR'].includes(r))
)

const hasTimetable = computed(() =>
  auth.roles.includes('STUDENT') || auth.roles.includes('TEACHER')
)

const statValues = reactive({
  users: 0,
  rooms: 0,
  courses: 0,
  cohorts: 0,
  loading: true,
})

const stats = computed(() => {
  const allStats = [
    {
      label: 'Utilizadores',
      value: statValues.users,
      icon: Users,
      link: '/users',
      bg: 'bg-blue-50 group-hover:bg-blue-100',
      text: 'text-blue-900',
    },
    {
      label: 'Salas',
      value: statValues.rooms,
      icon: DoorOpen,
      link: '/rooms',
      bg: 'bg-purple-50 group-hover:bg-purple-100',
      text: 'text-purple-700',
    },
    {
      label: 'Cursos',
      value: statValues.courses,
      icon: GraduationCap,
      link: '/courses',
      bg: 'bg-indigo-50 group-hover:bg-indigo-100',
      text: 'text-indigo-700',
      roles: ['ADMIN', 'COORDINATOR'],
    },
    {
      label: 'Turmas',
      value: statValues.cohorts,
      icon: BookOpen,
      link: '/cohorts',
      bg: 'bg-amber-50 group-hover:bg-amber-100',
      text: 'text-amber-700',
    },
  ]
  return allStats.filter(s => !s.roles || s.roles.some(r => auth.roles.includes(r)))
})

const quickActions = computed(() => {
  const actions = []

  if (auth.roles.includes('ADMIN') || auth.roles.includes('ASISTENT')) {
    actions.push({
      title: 'Gerar Horário',
      description: 'Executar o motor de optimização para o próximo período lectivo.',
      icon: Zap,
      link: '/dashboard/timetable',
      color: 'bg-amber-100 text-amber-700',
    })
  }

  if (auth.roles.includes('ADMIN') || auth.roles.includes('DIRECTOR')) {
    actions.push({
      title: 'Gestão de Salas',
      description: 'Configurar capacidades e restrições de acesso às salas.',
      icon: DoorOpen,
      link: '/dashboard/rooms',
      color: 'bg-purple-100 text-purple-700',
    })
  }

  if (auth.roles.includes('ADMIN') || auth.roles.includes('COORDINATOR')) {
    actions.push({
      title: 'Disciplinas & Cursos',
      description: 'Gerir matrizes curriculares e atribuição de professores.',
      icon: GraduationCap,
      link: '/dashboard/courses',
      color: 'bg-indigo-100 text-indigo-700',
    })
  }

  actions.push({
    title: 'Visualizar Horários',
    description: 'Consultar horários publicados de turmas, salas e professores.',
    icon: Calendar,
    link: '/dashboard/timetable',
    color: 'bg-blue-100 text-blue-900',
  })

  return actions
})

const roleLabel = (role: string) => {
  const labels: Record<string, string> = {
    ADMIN: 'Administrador',
    DIRECTOR: 'Diretor',
    ASISTENT: 'Assistente',
    COORDINATOR: 'Coordenador',
    TEACHER: 'Professor',
    STUDENT: 'Estudante',
    USER: 'Utilizador',
  }
  return labels[role] ?? role
}

onMounted(async () => {
  if (isStaff.value) {
    statValues.loading = true
    try {
      await Promise.allSettled([
        userStore.fetchUsers(0, 1),
        roomStore.fetchRooms(0, 1),
        courseStore.fetchCourses(0, 1),
        cohortStore.fetchCohorts(0, 1),
      ])
      statValues.users = userStore.pagedUsers?.page.totalElements ?? 0
      statValues.rooms = roomStore.pagedRooms?.page.totalElements ?? 0
      statValues.courses = courseStore.pagedCourses?.page.totalElements ?? 0
      statValues.cohorts = cohortStore.cohortsPage?.page.totalElements ?? 0
    } catch (err) {
      console.error('Failed to load dashboard stats:', err)
    } finally {
      statValues.loading = false
    }
  }
})
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

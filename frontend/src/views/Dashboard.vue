<template>
  <div>
    <PageHeader
      :icon="LayoutDashboard"
      title="Painel de controlo"
      subtitle="Resumo geral do sistema e acessos rápidos"
    />

    <div class="space-y-6">
      <!-- Welcome Card -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center text-blue-900 border border-blue-100">
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
          class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 hover:shadow-md transition group"
        >
          <div class="flex items-start justify-between">
            <div :class="stat.bg" class="p-2.5 rounded-md transition-colors">
              <component :is="stat.icon" :class="stat.text" class="w-5 h-5" />
            </div>
            <span v-if="statValues.loading" class="animate-pulse bg-gray-100 h-6 w-10 rounded"></span>
            <span v-else class="text-2xl font-bold text-gray-900">{{ stat.value }}</span>
          </div>
          <div class="mt-4">
            <p class="text-sm font-medium text-gray-500">{{ stat.label }}</p>
            <router-link
              :to="'/dashboard' + stat.link"
              class="text-xs text-blue-600 hover:text-blue-800 font-medium mt-1 flex items-center gap-1 group-hover:underline"
            >
              Gerir {{ stat.label.toLowerCase() }}
              <ArrowRight class="w-3 h-3" />
            </router-link>
          </div>
        </div>
      </div>

      <!-- Feasibility diagnostics (staff only) -->
      <DashboardInsights v-if="isStaff" />

      <!-- Main Content Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Quick Access -->
        <div class="lg:col-span-2 space-y-4">
          <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Acessos Rápidos</h3>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <router-link
              v-for="action in quickActions"
              :key="action.title"
              :to="action.link"
              class="bg-white p-4 rounded-[10px] border border-gray-100 shadow-sm hover:border-blue-200 hover:shadow-md transition group flex items-start gap-4"
            >
              <div :class="action.color" class="p-3 rounded-md shrink-0">
                <component :is="action.icon" class="w-6 h-6" />
              </div>
              <div class="min-w-0">
                <h4 class="font-bold text-gray-900 group-hover:text-blue-900 transition">{{ action.title }}</h4>
                <p class="text-sm text-gray-500 mt-1 line-clamp-2 leading-relaxed">{{ action.description }}</p>
              </div>
            </router-link>
          </div>
        </div>

        <!-- System Status -->
        <div class="space-y-4">
          <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Estado do Sistema</h3>
          <div class="bg-white rounded-[10px] border border-gray-100 shadow-sm p-5 space-y-5">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">Estado da Geração</span>
              <span class="px-2.5 py-1 bg-green-50 text-green-700 rounded-full text-xs font-bold border border-green-100 flex items-center gap-1.5">
                <div class="w-1.5 h-1.5 rounded-full bg-green-600 animate-pulse"></div>
                Operacional
              </span>
            </div>

            <div class="space-y-4 pt-4 border-t border-gray-50">
              <div class="flex items-start gap-3">
                <div class="bg-blue-50 p-2 rounded-md">
                  <span class="text-blue-900"><Info class="w-4 h-4" /></span>
                </div>
                <div>
                  <p class="text-xs font-semibold text-gray-900">Período Activo</p>
                  <p class="text-xs text-gray-500 mt-0.5">2025/2026 · 1º Semestre</p>
                </div>
              </div>
              <div class="flex items-start gap-3">
                <div class="bg-amber-50 p-2 rounded-md">
                  <span class="text-amber-600"><ShieldCheck class="w-4 h-4" /></span>
                </div>
                <div>
                  <p class="text-xs font-semibold text-gray-900">Segurança</p>
                  <p class="text-xs text-gray-500 mt-0.5">Sessão protegida com JWT</p>
                </div>
              </div>
            </div>

            <div class="bg-blue-900 rounded-[10px] p-4 mt-6 text-white overflow-hidden relative group cursor-pointer shadow-lg shadow-blue-900/20">
              <div class="relative z-10">
                <p class="text-xs font-bold text-blue-200 uppercase tracking-widest mb-1">Versão do Sistema</p>
                <p class="text-xl font-bold">v2.4.0-Refactor</p>
                <p class="text-[10px] text-blue-300/80 mt-2 font-mono">Build #20260410.1-prod</p>
              </div>
              <div class="absolute -right-4 -bottom-4 opacity-10 group-hover:scale-110 transition duration-500 transform rotate-12">
                <LayoutDashboard class="w-24 h-24" />
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
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

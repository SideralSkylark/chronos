<template>
  <div class="space-y-4">
    <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Inteligência do sistema</h3>
    
    <div v-if="store.loading" class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div v-for="i in 3" :key="i" class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 h-32 animate-pulse"></div>
    </div>
    
    <div v-else-if="stats" class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- Solver Readiness -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 flex flex-col justify-center items-center text-center">
        <div 
          class="px-4 py-1.5 rounded-full text-sm font-bold border mb-2 flex items-center gap-2"
          :class="{
            'bg-green-50 text-green-700 border-green-200': stats.solverReadiness === 'GREEN',
            'bg-amber-50 text-amber-700 border-amber-200': stats.solverReadiness === 'YELLOW',
            'bg-red-50 text-red-700 border-red-200': stats.solverReadiness === 'RED'
          }"
        >
          <CheckCircle v-if="stats.solverReadiness === 'GREEN'" class="w-4 h-4" />
          <AlertTriangle v-else-if="stats.solverReadiness === 'YELLOW'" class="w-4 h-4" />
          <XCircle v-else class="w-4 h-4" />
          {{ readinessLabel }}
        </div>
        <p class="text-xs text-gray-500 font-medium">{{ stats.solverReadinessReason }}</p>
      </div>

      <!-- Capacity Summary -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
        <h4 class="text-sm font-bold text-gray-900 mb-2">Balanço de Capacidade</h4>
        <div class="flex justify-between items-center text-sm">
          <span class="text-gray-500">Lugares nas salas:</span>
          <span class="font-bold text-gray-900">{{ stats.totalRoomCapacity }}</span>
        </div>
        <div class="flex justify-between items-center text-sm">
          <span class="text-gray-500">Procura estimada:</span>
          <span class="font-bold text-gray-900">{{ stats.totalCohortDemand }} estudantes</span>
        </div>
        <div class="pt-2 border-t border-gray-50 flex justify-between items-center text-sm">
          <span class="text-gray-500 font-medium">Margem:</span>
          <span class="font-bold" :class="stats.capacityMargin >= 0 ? 'text-green-600' : 'text-red-600'">
            {{ stats.capacityMargin > 0 ? '+' : '' }}{{ stats.capacityMargin }}
          </span>
        </div>
      </div>

      <!-- Teacher Workload -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
        <h4 class="text-sm font-bold text-gray-900 mb-2">Carga Docente</h4>
        <div class="flex justify-between items-center text-sm">
          <span class="text-gray-500">Total professores:</span>
          <span class="font-bold text-gray-900">{{ stats.totalTeachers }}</span>
        </div>
        <div class="flex justify-between items-center text-sm">
          <span class="text-gray-500">Sobrecarga:</span>
          <span class="font-bold flex items-center gap-1" :class="stats.teachersOverloaded > 0 ? 'text-amber-600' : 'text-gray-900'">
            <AlertTriangle v-if="stats.teachersOverloaded > 0" class="w-3.5 h-3.5" />
            {{ stats.teachersOverloaded }} profs.
          </span>
        </div>
        <div class="pt-2 border-t border-gray-50 flex justify-between items-center text-sm">
          <span class="text-gray-500 font-medium">Média de sessões:</span>
          <span class="font-bold text-gray-900">{{ stats.avgSlotsPerTeacher.toFixed(1) }}</span>
        </div>
      </div>

      <!-- Section A: Shift capacity panel -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 md:col-span-3 space-y-4">
        <h4 class="text-sm font-bold text-gray-900">Análise por turno</h4>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Morning -->
          <div class="border border-gray-50 rounded-md p-4 bg-gray-50/50">
            <h5 class="text-xs font-bold text-gray-700 uppercase tracking-wider mb-3">Manhã</h5>
            <div class="flex justify-between items-center text-sm mb-2">
              <span class="text-gray-500">Procura estimada:</span>
              <span class="font-bold text-gray-900">{{ stats.morningDemand }} est.</span>
            </div>
            <div class="flex justify-between items-center text-sm mb-3">
              <span class="text-gray-500">Capacidade (partilhada):</span>
              <span class="font-bold text-gray-900">{{ stats.totalRoomCapacity }} lug.</span>
            </div>
            <div 
              class="px-3 py-1.5 rounded-md text-xs font-bold border inline-flex items-center gap-1.5 w-full justify-center"
              :class="{
                'bg-green-50 text-green-700 border-green-200': stats.morningReadiness === 'GREEN',
                'bg-amber-50 text-amber-700 border-amber-200': stats.morningReadiness === 'YELLOW',
                'bg-red-50 text-red-700 border-red-200': stats.morningReadiness === 'RED'
              }"
            >
              <CheckCircle v-if="stats.morningReadiness === 'GREEN'" class="w-3.5 h-3.5" />
              <AlertTriangle v-else-if="stats.morningReadiness === 'YELLOW'" class="w-3.5 h-3.5" />
              <XCircle v-else class="w-3.5 h-3.5" />
              {{ stats.morningReadiness === 'GREEN' ? 'Pronto para gerar' : (stats.morningReadiness === 'YELLOW' ? 'Verificar configuração' : 'Inviável') }}
            </div>
          </div>
          <!-- Afternoon -->
          <div class="border border-gray-50 rounded-md p-4 bg-gray-50/50">
            <h5 class="text-xs font-bold text-gray-700 uppercase tracking-wider mb-3">Tarde</h5>
            <div class="flex justify-between items-center text-sm mb-2">
              <span class="text-gray-500">Procura estimada:</span>
              <span class="font-bold text-gray-900">{{ stats.afternoonDemand }} est.</span>
            </div>
            <div class="flex justify-between items-center text-sm mb-3">
              <span class="text-gray-500">Capacidade (partilhada):</span>
              <span class="font-bold text-gray-900">{{ stats.totalRoomCapacity }} lug.</span>
            </div>
            <div 
              class="px-3 py-1.5 rounded-md text-xs font-bold border inline-flex items-center gap-1.5 w-full justify-center"
              :class="{
                'bg-green-50 text-green-700 border-green-200': stats.afternoonReadiness === 'GREEN',
                'bg-amber-50 text-amber-700 border-amber-200': stats.afternoonReadiness === 'YELLOW',
                'bg-red-50 text-red-700 border-red-200': stats.afternoonReadiness === 'RED'
              }"
            >
              <CheckCircle v-if="stats.afternoonReadiness === 'GREEN'" class="w-3.5 h-3.5" />
              <AlertTriangle v-else-if="stats.afternoonReadiness === 'YELLOW'" class="w-3.5 h-3.5" />
              <XCircle v-else class="w-3.5 h-3.5" />
              {{ stats.afternoonReadiness === 'GREEN' ? 'Pronto para gerar' : (stats.afternoonReadiness === 'YELLOW' ? 'Verificar configuração' : 'Inviável') }}
            </div>
          </div>
        </div>
        <p class="text-[10px] text-gray-400 text-center mt-2">Capacidade das salas partilhada entre turnos — análise por sala em desenvolvimento</p>
      </div>

      <!-- Section B: Courses with most turmas -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 col-span-1">
        <h4 class="text-sm font-bold text-gray-900 mb-4">Cursos com mais turmas</h4>
        <div class="space-y-3">
          <div v-for="(course, index) in stats.topCoursesByCohorts" :key="course.courseId" class="flex items-center gap-3">
            <span class="w-5 h-5 rounded-full bg-blue-50 text-blue-900 text-xs font-bold flex items-center justify-center shrink-0">{{ index + 1 }}</span>
            <span class="text-sm text-gray-700 truncate flex-1" :title="course.courseName">{{ course.courseName }}</span>
            <span class="text-sm font-bold text-gray-900 shrink-0">{{ course.cohortCount }}</span>
          </div>
          <div v-if="!stats.topCoursesByCohorts.length" class="text-sm text-gray-400 text-center py-4">Nenhum curso encontrado</div>
        </div>
      </div>

      <!-- Section C: Teacher workload ranking -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 md:col-span-2">
        <h4 class="text-sm font-bold text-gray-900 mb-4">Carga docente por professor</h4>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div>
            <h5 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Mais sobrecarregados</h5>
            <div class="space-y-2">
              <div v-for="teacher in stats.mostLoadedTeachers" :key="teacher.teacherId" class="flex items-center justify-between text-sm p-2 rounded-md border border-gray-50" :class="teacher.overloaded ? 'bg-amber-50/50' : 'bg-gray-50/50'">
                <span class="text-gray-700 truncate pr-2" :class="teacher.overloaded ? 'font-medium' : ''">{{ teacher.teacherName }}</span>
                <span class="font-bold flex items-center gap-1 shrink-0" :class="teacher.overloaded ? 'text-amber-600' : 'text-gray-900'">
                  {{ teacher.totalSlots }}
                  <AlertTriangle v-if="teacher.overloaded" class="w-3 h-3" />
                </span>
              </div>
              <div v-if="!stats.mostLoadedTeachers.length" class="text-sm text-gray-400 text-center py-2">Nenhum professor encontrado</div>
            </div>
          </div>
          <div>
            <h5 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Menos ocupados</h5>
            <div class="space-y-2">
              <div v-for="teacher in stats.leastLoadedTeachers" :key="teacher.teacherId" class="flex items-center justify-between text-sm p-2 rounded-md border border-gray-50 bg-gray-50/50">
                <span class="text-gray-700 truncate pr-2">{{ teacher.teacherName }}</span>
                <span class="font-bold text-gray-900 shrink-0">{{ teacher.totalSlots }}</span>
              </div>
              <div v-if="!stats.leastLoadedTeachers.length" class="text-sm text-gray-400 text-center py-2">Nenhum professor encontrado</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Cohorts By Year -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 md:col-span-3">
        <h4 class="text-sm font-bold text-gray-900 mb-4">Distribuição por Ano Curricular</h4>
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-left">
            <thead>
              <tr class="text-gray-500 border-b border-gray-100">
                <th class="pb-2 font-medium">Ano</th>
                <th class="pb-2 font-medium">Turmas</th>
                <th class="pb-2 font-medium">Estudantes</th>
                <th class="pb-2 font-medium w-1/3">Proporção</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="year in stats.cohortsByYear" :key="year.year" class="border-b border-gray-50 last:border-0">
                <td class="py-2 font-bold text-gray-900">{{ year.year }}º Ano</td>
                <td class="py-2 text-gray-600">{{ year.cohortCount }}</td>
                <td class="py-2 text-gray-600">{{ year.totalStudents }}</td>
                <td class="py-2">
                  <div class="h-2 bg-gray-100 rounded-full w-full overflow-hidden">
                    <div 
                      class="h-full rounded-full transition-all duration-500"
                      :class="year.year === stats.bottleneckYear ? 'bg-amber-500' : 'bg-blue-300'"
                      :style="{ width: `${maxYearStudents ? (year.totalStudents / maxYearStudents * 100) : 0}%` }"
                    ></div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useDashboardStatsStore } from '@/stores/dashboardStats'
import { 
  CheckCircle, 
  AlertTriangle, 
  XCircle
} from 'lucide-vue-next'

const store = useDashboardStatsStore()

const stats = computed(() => store.stats)

const maxYearStudents = computed(() => {
  if (!stats.value?.cohortsByYear?.length) return 0
  return Math.max(...stats.value.cohortsByYear.map(y => y.totalStudents))
})

const readinessLabel = computed(() => {
  switch (stats.value?.solverReadiness) {
    case 'GREEN': return 'Pronto para gerar'
    case 'YELLOW': return 'Verificar configuração'
    case 'RED': return 'Inviável — sem capacidade'
    default: return 'Desconhecido'
  }
})

onMounted(() => {
  store.fetchStats()
})
</script>
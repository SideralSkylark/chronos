<template>
  <div class="space-y-4">
    <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Diagnóstico de viabilidade</h3>

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div v-for="i in 6" :key="i" class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 h-32 animate-pulse"></div>
    </div>

    <div v-else-if="stats" class="space-y-4">

      <!-- ── Row 0: Teacher load + Conflicts ─────────────────────────── -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">

        <!-- Teacher load -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
          <h4 class="text-xs font-bold text-gray-500 uppercase tracking-wider">Carga docente</h4>
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">Total professores</span>
            <span class="font-bold text-gray-900">{{ stats.totalTeachers }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">Sobrecarga</span>
            <span class="font-bold flex items-center gap-1" :class="stats.teachersOverloaded > 0 ? 'text-amber-600' : 'text-gray-900'">
              <AlertTriangle v-if="stats.teachersOverloaded > 0" class="w-3.5 h-3.5" />
              {{ stats.teachersOverloaded }} prof.
            </span>
          </div>
          <div class="pt-2 border-t border-gray-50 flex justify-between text-sm">
            <span class="text-gray-500 font-medium">Média sessões</span>
            <span class="font-bold text-gray-900">{{ stats.avgSlotsPerTeacher.toFixed(1) }}</span>
          </div>
        </div>

        <!-- Potential conflicts count -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 flex flex-col items-center justify-center text-center gap-2">
          <span
            class="text-4xl font-bold"
            :class="{
              'text-red-600': potentialConflicts > 2,
              'text-amber-500': potentialConflicts === 1 || potentialConflicts === 2,
              'text-green-600': potentialConflicts === 0
            }"
          >{{ potentialConflicts }}</span>
          <p class="text-xs text-gray-500 font-medium leading-tight">Conflitos potenciais<br>identificados</p>
          <span
            class="text-[10px] font-bold px-2 py-0.5 rounded-full border"
            :class="{
              'bg-red-50 text-red-700 border-red-200': potentialConflicts > 2,
              'bg-amber-50 text-amber-700 border-amber-200': potentialConflicts === 1 || potentialConflicts === 2,
              'bg-green-50 text-green-700 border-green-200': potentialConflicts === 0
            }"
          >{{ potentialConflicts > 0 ? 'Requer atenção' : 'Sem conflitos' }}</span>
        </div>
      </div>

      <!-- ── Row 1: Four feasibility diagnostic cards ────────────────────── -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

        <!-- Diagnostic 1: Oversized cohorts -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-red-50 flex items-center justify-center shrink-0">
                <Users class="w-4 h-4 text-red-600" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Turmas sobredimensionadas</h4>
                <p class="text-xs text-gray-400 mt-0.5">Capacidade excede as salas disponíveis</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0"
              :class="stats.oversizedCohorts.length > 0
                ? 'bg-red-50 text-red-700 border-red-200'
                : 'bg-green-50 text-green-700 border-green-200'"
            >
              {{ stats.oversizedCohorts.length > 0
                ? `${stats.oversizedCohorts.length} turma${stats.oversizedCohorts.length > 1 ? 's' : ''}`
                : 'Sem problemas' }}
            </span>
          </div>

          <div v-if="stats.oversizedCohorts.length > 0" class="space-y-2">
            <div
              v-for="c in stats.oversizedCohorts"
              :key="c.cohortId"
              class="px-3 py-2 rounded-lg border-l-2 text-xs leading-relaxed"
              :class="c.severity === 'RED'
                ? 'bg-red-50 border-red-400 text-red-800'
                : 'bg-amber-50 border-amber-400 text-amber-800'"
            >
              <span class="font-bold">{{ c.cohortName }}</span> — {{ c.headcount }} estudantes.
              {{ c.compatibleRooms === 0
                ? 'Nenhuma sala disponível.'
                : `Apenas ${c.compatibleRooms} sala${c.compatibleRooms > 1 ? 's' : ''} compatível${c.compatibleRooms > 1 ? 'eis' : ''} (≥ ${c.minRequiredCapacity} lugares).` }}
            </div>
          </div>
          <div v-else class="px-3 py-2 rounded-lg bg-green-50 border-l-2 border-green-400 text-xs text-green-800">
            Todas as turmas têm pelo menos uma sala compatível.
          </div>
        </div>

        <!-- Diagnostic 2: Room scarcity by tier -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-purple-50 flex items-center justify-center shrink-0">
                <DoorOpen class="w-4 h-4 text-purple-700" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Escassez de salas</h4>
                <p class="text-xs text-gray-400 mt-0.5">Disponibilidade por capacidade vs. procura</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0"
              :class="{
                'bg-red-50 text-red-700 border-red-200': roomScarcitySeverity === 'RED',
                'bg-amber-50 text-amber-700 border-amber-200': roomScarcitySeverity === 'YELLOW',
                'bg-green-50 text-green-700 border-green-200': roomScarcitySeverity === 'GREEN'
              }"
            >
              {{ roomScarcitySeverity === 'RED' ? 'Crítico' : roomScarcitySeverity === 'YELLOW' ? 'Limitado' : 'Adequado' }}
            </span>
          </div>

          <div class="space-y-2.5">
            <div v-for="tier in stats.roomTierDistribution" :key="tier.label" class="space-y-1">
              <div class="flex justify-between text-xs text-gray-500">
                <span>{{ tier.label }}</span>
                <span class="font-medium text-gray-700">{{ tier.roomCount }} sala{{ tier.roomCount !== 1 ? 's' : '' }}</span>
              </div>
              <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500"
                  :class="{
                    'bg-red-400': tier.severity === 'RED',
                    'bg-amber-400': tier.severity === 'YELLOW',
                    'bg-blue-400': tier.severity === 'GREEN'
                  }"
                  :style="{ width: `${tier.supplyPercent}%` }"
                ></div>
              </div>
              <div class="flex justify-between text-[10px] text-gray-400">
                <span>Oferta: {{ tier.supplyPercent }}%</span>
                <span>Procura: {{ tier.demandPercent }}%</span>
              </div>
            </div>
          </div>

          <div
            v-if="stats.roomScarcityNote"
            class="px-3 py-2 rounded-lg border-l-2 text-xs leading-relaxed"
            :class="roomScarcitySeverity === 'RED'
              ? 'bg-red-50 border-red-400 text-red-800'
              : 'bg-amber-50 border-amber-400 text-amber-800'"
          >
            {{ stats.roomScarcityNote }}
          </div>
        </div>

        <!-- Diagnostic 3: Distribution mismatch -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-amber-50 flex items-center justify-center shrink-0">
                <BarChart2 class="w-4 h-4 text-amber-600" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Desequilíbrio de distribuição</h4>
                <p class="text-xs text-gray-400 mt-0.5">Correspondência entre procura e oferta</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0"
              :class="{
                'bg-red-50 text-red-700 border-red-200': distributionMismatchSeverity === 'RED',
                'bg-amber-50 text-amber-700 border-amber-200': distributionMismatchSeverity === 'YELLOW',
                'bg-green-50 text-green-700 border-green-200': distributionMismatchSeverity === 'GREEN'
              }"
            >
              {{ distributionMismatchSeverity === 'GREEN' ? 'Equilibrado' : 'Desequilíbrio' }}
            </span>
          </div>

          <div class="space-y-3">
            <div v-for="m in stats.distributionMismatches" :key="m.category" class="space-y-1">
              <div class="flex justify-between text-xs font-medium text-gray-700">
                <span>{{ m.category }}</span>
                <span
                  class="text-[10px] font-bold px-1.5 py-0.5 rounded"
                  :class="Math.abs(m.demandPercent - m.supplyPercent) > 25
                    ? 'bg-red-50 text-red-700'
                    : 'bg-amber-50 text-amber-700'"
                >
                  Δ{{ Math.abs(m.demandPercent - m.supplyPercent) }}%
                </span>
              </div>
              <div class="flex gap-1 items-center">
                <span class="text-[10px] text-gray-400 w-14 shrink-0">Turmas</span>
                <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full rounded-full bg-red-400 transition-all duration-500" :style="{ width: `${m.demandPercent}%` }"></div>
                </div>
                <span class="text-[10px] text-gray-500 w-8 text-right">{{ m.demandPercent }}%</span>
              </div>
              <div class="flex gap-1 items-center">
                <span class="text-[10px] text-gray-400 w-14 shrink-0">Salas</span>
                <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full rounded-full bg-blue-400 transition-all duration-500" :style="{ width: `${m.supplyPercent}%` }"></div>
                </div>
                <span class="text-[10px] text-gray-500 w-8 text-right">{{ m.supplyPercent }}%</span>
              </div>
            </div>
          </div>

          <div v-if="stats.distributionMismatchNote" class="px-3 py-2 rounded-lg bg-amber-50 border-l-2 border-amber-400 text-xs text-amber-800 leading-relaxed">
            {{ stats.distributionMismatchNote }}
          </div>
        </div>

        <!-- Diagnostic 4: Fragmentation risk -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-3">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-orange-50 flex items-center justify-center shrink-0">
                <AlertOctagon class="w-4 h-4 text-orange-600" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Risco de fragmentação</h4>
                <p class="text-xs text-gray-400 mt-0.5">Turmas próximas do limite das salas disponíveis</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0"
              :class="stats.fragmentationRisk.length > 0
                ? 'bg-amber-50 text-amber-700 border-amber-200'
                : 'bg-green-50 text-green-700 border-green-200'"
            >
              {{ stats.fragmentationRisk.length > 0
                ? `${stats.fragmentationRisk.length} turma${stats.fragmentationRisk.length > 1 ? 's' : ''}`
                : 'Sem risco' }}
            </span>
          </div>

          <div v-if="stats.fragmentationRisk.length > 0" class="space-y-2">
            <div
              v-for="c in stats.fragmentationRisk"
              :key="c.cohortId"
              class="flex items-center justify-between text-xs p-2 rounded-md border border-gray-50"
              :class="c.utilizationPercent >= 95 ? 'bg-red-50/60' : 'bg-amber-50/50'"
            >
              <span class="text-gray-700 truncate pr-2 font-medium">{{ c.cohortName }}</span>
              <div class="flex items-center gap-2 shrink-0">
                <span class="text-gray-400">{{ c.headcount }} / {{ c.maxCompatibleCapacity }}</span>
                <span
                  class="font-bold text-[10px] px-1.5 py-0.5 rounded"
                  :class="c.utilizationPercent >= 95 ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'"
                >{{ c.utilizationPercent }}%</span>
              </div>
            </div>
          </div>
          <div v-else class="px-3 py-2 rounded-lg bg-green-50 border-l-2 border-green-400 text-xs text-green-800">
            Nenhuma turma está próxima do limite de capacidade máxima.
          </div>

          <p v-if="stats.fragmentationRisk.length > 0" class="text-xs text-gray-400 leading-relaxed">
            Turmas a ≥ 85% da capacidade máxima disponível — qualquer inscrição adicional pode tornar a atribuição inviável.
          </p>
        </div>
      </div>

      <!-- ── Row 2: Shift analysis ───────────────────────────────────────── -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-4">
        <h4 class="text-sm font-bold text-gray-900">Análise por turno</h4>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Morning -->
          <div class="border border-gray-100 rounded-lg p-4 bg-gray-50/40">
            <h5 class="text-xs font-bold text-gray-600 uppercase tracking-wider mb-3">Manhã</h5>
            <div class="flex justify-between items-center text-sm mb-2">
              <span class="text-gray-500">Procura estimada</span>
              <span class="font-bold text-gray-900">{{ stats.morningDemand }} est.</span>
            </div>
            <div class="flex justify-between items-center text-sm mb-3">
              <span class="text-gray-500">Capacidade (partilhada)</span>
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
              {{ shiftLabel(stats.morningReadiness) }}
            </div>
            <p v-if="stats.morningReadinessReason" class="text-[10px] text-gray-400 mt-2 text-center">
              {{ stats.morningReadinessReason }}
            </p>
          </div>
          <!-- Afternoon -->
          <div class="border border-gray-100 rounded-lg p-4 bg-gray-50/40">
            <h5 class="text-xs font-bold text-gray-600 uppercase tracking-wider mb-3">Tarde</h5>
            <div class="flex justify-between items-center text-sm mb-2">
              <span class="text-gray-500">Procura estimada</span>
              <span class="font-bold text-gray-900">{{ stats.afternoonDemand }} est.</span>
            </div>
            <div class="flex justify-between items-center text-sm mb-3">
              <span class="text-gray-500">Capacidade (partilhada)</span>
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
              {{ shiftLabel(stats.afternoonReadiness) }}
            </div>
            <p v-if="stats.afternoonReadinessReason" class="text-[10px] text-gray-400 mt-2 text-center">
              {{ stats.afternoonReadinessReason }}
            </p>
          </div>
        </div>
        <p class="text-[10px] text-gray-400 text-center">Capacidade das salas partilhada entre turnos — análise por sala em desenvolvimento</p>
      </div>

      <!-- ── Row 3: Top courses + teacher workload ───────────────────────── -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">

        <!-- Courses with most cohorts -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5">
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

        <!-- Teacher workload ranking -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 md:col-span-2">
          <h4 class="text-sm font-bold text-gray-900 mb-4">Carga docente por professor</h4>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <h5 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Mais sobrecarregados</h5>
              <div class="space-y-2">
                <div
                  v-for="teacher in stats.mostLoadedTeachers"
                  :key="teacher.teacherId"
                  class="flex items-center justify-between text-sm p-2 rounded-md border border-gray-50"
                  :class="teacher.overloaded ? 'bg-amber-50/50' : 'bg-gray-50/50'"
                >
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
                <div
                  v-for="teacher in stats.leastLoadedTeachers"
                  :key="teacher.teacherId"
                  class="flex items-center justify-between text-sm p-2 rounded-md border border-gray-50 bg-gray-50/50"
                >
                  <span class="text-gray-700 truncate pr-2">{{ teacher.teacherName }}</span>
                  <span class="font-bold text-gray-900 shrink-0">{{ teacher.totalSlots }}</span>
                </div>
                <div v-if="!stats.leastLoadedTeachers.length" class="text-sm text-gray-400 text-center py-2">Nenhum professor encontrado</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Row 4: Cohorts by curricular year ──────────────────────────── -->
      <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5">
        <h4 class="text-sm font-bold text-gray-900 mb-4">Distribuição por ano curricular</h4>
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
  AlertTriangle,
  Users,
  DoorOpen,
  BarChart2,
  AlertOctagon,
  CheckCircle,
  XCircle,
} from 'lucide-vue-next'

const store = useDashboardStatsStore()
const stats = computed(() => store.stats)

// ── Derived feasibility computeds ──────────────────────────────────────────

const potentialConflicts = computed(() => {
  const hardFails = (stats.value?.oversizedCohorts ?? []).filter(c => c.severity === 'RED').length
  const nearMisses = (stats.value?.fragmentationRisk ?? []).filter(c => c.utilizationPercent >= 95).length
  return hardFails + nearMisses
})

const roomScarcitySeverity = computed(() => {
  const tiers = stats.value?.roomTierDistribution ?? []
  if (tiers.some(t => t.severity === 'RED')) return 'RED'
  if (tiers.some(t => t.severity === 'YELLOW')) return 'YELLOW'
  return 'GREEN'
})

const distributionMismatchSeverity = computed(() => {
  const mismatches = stats.value?.distributionMismatches ?? []
  if (mismatches.some(m => Math.abs(m.demandPercent - m.supplyPercent) > 30)) return 'RED'
  if (mismatches.some(m => Math.abs(m.demandPercent - m.supplyPercent) > 15)) return 'YELLOW'
  return 'GREEN'
})

const maxYearStudents = computed(() => {
  if (!stats.value?.cohortsByYear?.length) return 0
  return Math.max(...stats.value.cohortsByYear.map(y => y.totalStudents))
})

// ── Labels ─────────────────────────────────────────────────────────────────

const shiftLabel = (readiness: 'GREEN' | 'YELLOW' | 'RED') => {
  switch (readiness) {
    case 'GREEN': return 'Pronto para gerar'
    case 'YELLOW': return 'Verificar configuração'
    case 'RED': return 'Inviável'
  }
}

onMounted(() => {
  store.fetchStats()
})
</script>

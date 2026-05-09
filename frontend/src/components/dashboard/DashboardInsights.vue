<template>
  <div class="space-y-4">
    <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1">Diagnóstico de viabilidade</h3>

    <!-- Loading skeleton -->
    <div v-if="store.loading" class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div v-for="i in 6" :key="i" class="bg-white rounded-[10px] border border-gray-100 p-5 h-32 animate-pulse"></div>
    </div>

    <div v-else-if="stats" class="space-y-4">

      <!-- ── Row 0: KPI strip ────────────────────────────────────────── -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        <!-- (1) Potential conflicts -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-4 flex flex-col h-36 hover:border-blue-200 transition-all group">
          <div class="flex items-start justify-between mb-auto">
            <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all duration-300">
              <AlertOctagon class="w-4 h-4 text-red-500" />
            </div>
            <span
              class="text-3xl font-bold leading-none mt-1"
              :class="{
                'text-red-600': potentialConflicts > 2,
                'text-amber-500': potentialConflicts > 0 && potentialConflicts <= 2,
                'text-green-600': potentialConflicts === 0
              }"
            >{{ potentialConflicts }}</span>
          </div>
          <div class="mt-2">
            <h4 class="text-xs font-bold text-gray-900 mb-2">Conflitos Potenciais</h4>
            <div class="space-y-1">
              <div class="flex justify-between items-center text-[10px] leading-none">
                <span class="text-gray-400 font-medium uppercase tracking-tighter">Inviáveis (RED)</span>
                <span class="font-bold text-red-600">{{ hardFailsCount }}</span>
              </div>
              <div class="flex justify-between items-center text-[10px] leading-none">
                <span class="text-gray-400 font-medium uppercase tracking-tighter">Críticos (≥95%)</span>
                <span class="font-bold text-amber-600">{{ nearMissesCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- (2) Carga docente -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-4 flex flex-col h-36 hover:border-blue-200 transition-all group">
          <div class="flex items-start justify-between mb-auto">
            <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all duration-300">
              <Briefcase class="w-4 h-4 text-blue-600" />
            </div>
            <span class="text-2xl font-bold text-gray-900 mt-1">{{ stats.totalTeachers }}</span>
          </div>
          <div class="mt-2">
            <h4 class="text-xs font-bold text-gray-900 mb-2">Carga Docente</h4>
            <div class="space-y-1">
              <div class="flex justify-between items-center text-[10px] leading-none">
                <span class="text-gray-400 font-medium uppercase tracking-tighter">Sobrecarga</span>
                <span class="font-bold" :class="stats.teachersOverloaded > 0 ? 'text-amber-600' : 'text-gray-400'">{{ stats.teachersOverloaded }} prof.</span>
              </div>
              <div class="flex justify-between items-center text-[10px] leading-none">
                <span class="text-gray-400 font-medium uppercase tracking-tighter">Média sessões</span>
                <span class="font-bold text-gray-400">{{ stats.avgSlotsPerTeacher.toFixed(1) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- (3) Turno Manhã -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-4 flex flex-col h-36 hover:border-blue-200 transition-all group">
          <div class="flex items-start justify-between mb-auto">
            <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all duration-300">
              <Sun class="w-4 h-4 text-amber-500" />
            </div>
            <span 
              class="text-[9px] font-extrabold px-2 py-0.5 rounded-full border border-gray-100 bg-gray-50 text-gray-600 shadow-sm mt-1 uppercase tracking-wider"
              :class="{
                'text-green-700 border-green-200 bg-green-50': stats.morningReadiness === 'GREEN',
                'text-amber-700 border-amber-200 bg-amber-50': stats.morningReadiness === 'YELLOW',
                'text-red-700 border-red-200 bg-red-50': stats.morningReadiness === 'RED'
              }"
            >
              {{ shiftLabel(stats.morningReadiness) }}
            </span>
          </div>
          <div class="mt-2">
            <h4 class="text-xs font-bold text-gray-900 mb-2">Turno Manhã</h4>
            <div class="space-y-1">
              <div class="flex justify-between items-center text-[10px] leading-none">
                <span class="text-gray-400 font-medium uppercase tracking-tighter">Procura / Cap.</span>
                <span class="font-bold text-gray-600">{{ stats.morningDemand }} / {{ stats.totalRoomCapacity }}</span>
              </div>
              <p v-if="stats.morningReadinessReason" class="text-[9px] text-gray-400 truncate italic font-medium">{{ stats.morningReadinessReason }}</p>
            </div>
          </div>
        </div>

        <!-- (4) Turno Tarde -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-4 flex flex-col h-36 hover:border-blue-200 transition-all group">
          <div class="flex items-start justify-between mb-auto">
            <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all duration-300">
              <Moon class="w-4 h-4 text-indigo-500" />
            </div>
            <span 
              class="text-[9px] font-extrabold px-2 py-0.5 rounded-full border border-gray-100 bg-gray-50 text-gray-600 shadow-sm mt-1 uppercase tracking-wider"
              :class="{
                'text-green-700 border-green-200 bg-green-50': stats.afternoonReadiness === 'GREEN',
                'text-amber-700 border-amber-200 bg-amber-50': stats.afternoonReadiness === 'YELLOW',
                'text-red-700 border-red-200 bg-red-50': stats.afternoonReadiness === 'RED'
              }"
            >
              {{ shiftLabel(stats.afternoonReadiness) }}
            </span>
          </div>
          <div class="mt-2">
            <h4 class="text-xs font-bold text-gray-900 mb-2">Turno Tarde</h4>
            <div class="space-y-1">
              <div class="flex justify-between items-center text-[10px] leading-none">
                <span class="text-gray-400 font-medium uppercase tracking-tighter">Procura / Cap.</span>
                <span class="font-bold text-gray-600">{{ stats.afternoonDemand }} / {{ stats.totalRoomCapacity }}</span>
              </div>
              <p v-if="stats.afternoonReadinessReason" class="text-[9px] text-gray-400 truncate italic font-medium">{{ stats.afternoonReadinessReason }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Row 1: Four feasibility diagnostic cards ────────────────────── -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

        <!-- Diagnostic 1: Oversized cohorts -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-4 hover:border-blue-200 transition-colors">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-white flex items-center justify-center shrink-0 border border-gray-100 shadow-sm">
                <Users class="w-4 h-4 text-red-500" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Turmas sobredimensionadas</h4>
                <p class="text-[11px] text-gray-400 font-medium">Capacidade insuficiente em salas compatíveis</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0 shadow-sm"
              :class="stats.oversizedCohorts.length > 0
                ? 'bg-red-50 text-red-700 border-red-200'
                : 'bg-green-50 text-green-700 border-green-200'"
            >
              {{ stats.oversizedCohorts.length > 0
                ? `${stats.oversizedCohorts.length} turma${stats.oversizedCohorts.length > 1 ? 's' : ''}`
                : 'Sem problemas' }}
            </span>
          </div>

          <div v-if="stats.oversizedCohorts.length > 0" class="space-y-2.5">
            <div
              v-for="c in stats.oversizedCohorts"
              :key="c.cohortId"
              class="px-3 py-2 rounded-lg border-l-4 text-xs leading-relaxed shadow-sm bg-white border-gray-100"
              :class="c.severity === 'RED'
                ? 'border-l-red-500 bg-red-50/40'
                : 'border-l-amber-500 bg-amber-50/40'"
            >
              <span class="font-bold text-gray-900">{{ c.cohortName }}</span> — <span class="font-bold text-gray-700">{{ c.headcount }} estudantes</span>.
              <p class="text-gray-500 mt-1 font-medium">
                {{ c.compatibleRooms === 0
                  ? 'Nenhuma sala compatível.'
                  : `Apenas ${c.compatibleRooms} sala${c.compatibleRooms > 1 ? 's' : ''} compatível${c.compatibleRooms > 1 ? 'eis' : ''}.` }}
              </p>
            </div>
          </div>
          <div v-else class="px-4 py-3 rounded-lg bg-green-50 border-l-4 border-green-400 text-xs text-green-800 shadow-sm font-bold">
            Todas as turmas têm pelo menos uma sala compatível disponível.
          </div>
        </div>

        <!-- Diagnostic 2: Room scarcity by tier -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-4 hover:border-blue-200 transition-colors">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-white flex items-center justify-center shrink-0 border border-gray-100 shadow-sm">
                <DoorOpen class="w-4 h-4 text-purple-600" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Escassez de salas</h4>
                <p class="text-[11px] text-gray-400 font-medium">Disponibilidade por capacidade vs. procura</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0 shadow-sm"
              :class="{
                'bg-red-50 text-red-700 border-red-200': roomScarcitySeverity === 'RED',
                'bg-amber-50 text-amber-700 border-amber-200': roomScarcitySeverity === 'YELLOW',
                'bg-green-50 text-green-700 border-green-200': roomScarcitySeverity === 'GREEN'
              }"
            >
              {{ roomScarcitySeverity === 'RED' ? 'Crítico' : roomScarcitySeverity === 'YELLOW' ? 'Limitado' : 'Adequado' }}
            </span>
          </div>

          <div class="space-y-4 pt-1">
            <div v-for="tier in stats.roomTierDistribution" :key="tier.label" class="space-y-2">
              <div class="flex justify-between items-end">
                <span class="text-[11px] font-bold text-gray-700">{{ tier.label }}</span>
                <span class="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">
                  <span class="bg-gray-50 px-1 rounded">oferta {{ tier.supplyPercent.toFixed(0) }}%</span> 
                  <span class="mx-1 text-gray-300">/</span>
                  <span class="bg-gray-50 px-1 rounded">procura {{ tier.demandPercent.toFixed(0) }}%</span>
                </span>
              </div>
              <div class="h-2.5 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                <div
                  class="h-full rounded-full transition-all duration-500 shadow-sm"
                  :class="{
                    'bg-red-500': tier.severity === 'RED',
                    'bg-amber-500': tier.severity === 'YELLOW',
                    'bg-blue-500': tier.severity === 'GREEN'
                  }"
                  :style="{ width: `${tier.supplyPercent}%` }"
                ></div>
              </div>
            </div>
          </div>

          <div
            v-if="stats.roomScarcityNote"
            class="px-3.5 py-2 rounded-lg border-l-4 text-[11px] leading-relaxed bg-gray-50 border-gray-300 text-gray-500 shadow-sm italic font-medium"
          >
            {{ stats.roomScarcityNote }}
          </div>
        </div>

        <!-- Diagnostic 3: Distribution mismatch -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-4 hover:border-blue-200 transition-colors">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-white flex items-center justify-center shrink-0 border border-gray-100 shadow-sm">
                <BarChart2 class="w-4 h-4 text-amber-600" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Desequilíbrio de distribuição</h4>
                <p class="text-[11px] text-gray-400 font-medium">Correspondência entre procura e oferta</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0 shadow-sm"
              :class="{
                'bg-red-50 text-red-700 border-red-200': distributionMismatchSeverity === 'RED',
                'bg-amber-50 text-amber-700 border-amber-200': distributionMismatchSeverity === 'YELLOW',
                'bg-green-50 text-green-700 border-green-200': distributionMismatchSeverity === 'GREEN'
              }"
            >
              {{ distributionMismatchSeverity === 'GREEN' ? 'Equilibrado' : 'Desequilíbrio' }}
            </span>
          </div>

          <div class="space-y-4">
            <div v-for="m in stats.distributionMismatches" :key="m.category" class="space-y-2.5">
              <div class="flex items-center gap-2">
                <span class="text-[11px] font-bold text-gray-800">{{ m.category }}</span>
                <span class="text-[9px] font-bold px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 shadow-sm border border-gray-200">
                  Δ{{ Math.abs(m.demandPercent - m.supplyPercent).toFixed(1) }}%
                </span>
              </div>
              <div class="space-y-2">
                <div class="flex gap-2 items-center">
                  <span class="text-[9px] font-bold text-gray-400 w-12 shrink-0 uppercase tracking-tighter text-right">Turmas</span>
                  <div class="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                    <div class="h-full rounded-full bg-red-400 transition-all duration-500 shadow-sm" :style="{ width: `${m.demandPercent}%` }"></div>
                  </div>
                  <span class="text-[10px] font-bold text-gray-600 w-8 text-right">{{ m.demandPercent.toFixed(0) }}%</span>
                </div>
                <div class="flex gap-2 items-center">
                  <span class="text-[9px] font-bold text-gray-400 w-12 shrink-0 uppercase tracking-tighter text-right">Salas</span>
                  <div class="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                    <div class="h-full rounded-full bg-blue-400 transition-all duration-500 shadow-sm" :style="{ width: `${m.supplyPercent}%` }"></div>
                  </div>
                  <span class="text-[10px] font-bold text-gray-600 w-8 text-right">{{ m.supplyPercent.toFixed(0) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Diagnostic 4: Fragmentation risk -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-4 hover:border-blue-200 transition-colors">
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-white flex items-center justify-center shrink-0 border border-gray-100 shadow-sm">
                <AlertOctagon class="w-4 h-4 text-orange-600" />
              </div>
              <div>
                <h4 class="text-sm font-bold text-gray-900">Risco de fragmentação</h4>
                <p class="text-[11px] text-gray-400 font-medium">Turmas no limite da capacidade das salas</p>
              </div>
            </div>
            <span
              class="text-[10px] font-bold px-2.5 py-1 rounded-full border shrink-0 shadow-sm"
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
              v-for="c in sortedFragmentationRisk"
              :key="c.cohortId"
              class="flex items-center justify-between text-[11px] p-2 rounded-lg border border-gray-100 shadow-sm bg-white hover:bg-gray-50 transition-colors"
              :class="c.utilizationPercent >= 95 ? 'border-l-4 border-l-red-500' : 'border-l-4 border-l-amber-500'"
            >
              <span class="text-gray-900 font-bold truncate pr-2">{{ c.cohortName }}</span>
              <div class="flex items-center gap-3 shrink-0">
                <span class="text-gray-500 font-bold">{{ c.headcount }} / {{ c.maxCompatibleCapacity }}</span>
                <span
                  class="font-bold text-[10px] px-2 py-0.5 rounded-full shadow-sm border border-gray-100"
                  :class="c.utilizationPercent >= 95 ? 'bg-red-50 text-red-700 border-red-100' : 'bg-amber-50 text-amber-700 border-amber-100'"
                >{{ c.utilizationPercent.toFixed(1) }}%</span>
              </div>
            </div>
          </div>
          <div v-else class="px-4 py-3 rounded-lg bg-green-50 border-l-4 border-green-400 text-xs text-green-800 shadow-sm font-bold">
            Nenhuma turma está próxima do limite de capacidade máxima.
          </div>
        </div>
      </div>

      <!-- ── Row 2: Bottom row (3 cols) ────────────────────────────────── -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">

        <!-- (1) Mais sobrecarregados -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 flex flex-col hover:border-blue-200 transition-all group">
          <h4 class="text-sm font-bold text-gray-900 mb-4 flex items-center gap-2">
            <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all">
              <AlertTriangle class="w-4 h-4 text-amber-500" />
            </div>
            Mais sobrecarregados
          </h4>
          <div class="space-y-2 flex-1">
            <div
              v-for="teacher in stats.mostLoadedTeachers"
              :key="teacher.teacherId"
              class="flex items-center justify-between text-[11px] p-2 rounded-lg border border-gray-50 bg-gray-50/50 shadow-sm hover:bg-white transition-colors"
            >
              <span class="text-gray-700 truncate pr-2 font-medium">{{ teacher.teacherName }}</span>
              <span class="font-bold flex items-center gap-1.5 shrink-0 px-2 py-0.5 rounded bg-white border border-gray-100" :class="teacher.overloaded ? 'text-red-600 shadow-sm border-red-100' : 'text-gray-900'">
                {{ teacher.totalSlots }}
                <AlertTriangle v-if="teacher.overloaded" class="w-3 h-3" />
              </span>
            </div>
          </div>
        </div>

        <!-- (2) Menos ocupados -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 flex flex-col hover:border-blue-200 transition-all group">
          <h4 class="text-sm font-bold text-gray-900 mb-4 flex items-center gap-2">
            <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all">
              <Users class="w-4 h-4 text-blue-500" />
            </div>
            Menos ocupados
          </h4>
          <div class="space-y-2 flex-1">
            <div
              v-for="teacher in stats.leastLoadedTeachers"
              :key="teacher.teacherId"
              class="flex items-center justify-between text-[11px] p-2 rounded-lg border border-gray-50 bg-gray-50/50 shadow-sm hover:bg-white transition-colors"
            >
              <span class="text-gray-700 truncate pr-2 font-medium">{{ teacher.teacherName }}</span>
              <span class="font-bold text-gray-900 shrink-0 px-2 py-0.5 rounded bg-white shadow-sm border border-gray-100">{{ teacher.totalSlots }}</span>
            </div>
          </div>
        </div>

        <!-- (3) Cursos + Distribuição Curricular -->
        <div class="bg-white rounded-[10px] shadow-sm border border-gray-100 p-5 space-y-6 hover:border-blue-200 transition-all group">
          <div>
            <h4 class="text-sm font-bold text-gray-900 mb-4 flex items-center gap-2">
              <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all">
                <BarChart2 class="w-4 h-4 text-indigo-500" />
              </div>
              Cursos (top 3)
            </h4>
            <div class="space-y-2">
              <div v-for="(course, index) in stats.topCoursesByCohorts.slice(0, 3)" :key="course.courseId" class="flex items-center gap-3 p-1.5 rounded-lg hover:bg-gray-50 transition-colors border border-transparent hover:border-gray-100 shadow-none hover:shadow-sm">
                <span class="text-[10px] text-indigo-600 font-extrabold font-mono w-5 h-5 rounded-full bg-indigo-50 flex items-center justify-center shrink-0 border border-indigo-100 shadow-inner">{{ index + 1 }}</span>
                <span class="text-[11px] text-gray-800 font-bold truncate flex-1" :title="course.courseName">{{ course.courseName }}</span>
                <span class="text-[11px] font-extrabold text-indigo-900 shrink-0 px-2 py-0.5 bg-white border border-indigo-100 rounded-full shadow-sm">{{ course.cohortCount }}</span>
              </div>
            </div>
          </div>

          <div class="pt-5 border-t border-gray-100">
            <h4 class="text-sm font-bold text-gray-900 mb-4 flex items-center gap-2">
              <div class="p-1.5 rounded-lg bg-white border border-gray-100 shadow-sm group-hover:shadow transition-all">
                <DoorOpen class="w-4 h-4 text-emerald-500" />
              </div>
              Distribuição por ano
            </h4>
            <div class="space-y-3.5">
              <div v-for="year in stats.cohortsByYear" :key="year.year" class="space-y-1.5">
                <div class="flex justify-between text-[10px]">
                  <span class="font-bold text-gray-700">{{ year.year }}º Ano Curricular</span>
                  <span class="text-gray-400 font-bold">{{ year.totalStudents }} est.</span>
                </div>
                <div class="h-2.5 bg-gray-100 rounded-full w-full overflow-hidden shadow-inner">
                  <div
                    class="h-full rounded-full transition-all duration-500 shadow-sm"
                    :class="year.year === stats.bottleneckYear ? 'bg-amber-500' : 'bg-emerald-500'"
                    :style="{ width: `${maxYearStudents ? (year.totalStudents / maxYearStudents * 100) : 0}%` }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
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
  Briefcase,
  Sun,
  Moon,
} from 'lucide-vue-next'

const store = useDashboardStatsStore()
const stats = computed(() => store.stats)

// ── Derived feasibility computeds ──────────────────────────────────────────

const hardFailsCount = computed(() => (stats.value?.oversizedCohorts ?? []).filter(c => c.severity === 'RED').length)
const nearMissesCount = computed(() => (stats.value?.fragmentationRisk ?? []).filter(c => c.utilizationPercent >= 95).length)

const potentialConflicts = computed(() => hardFailsCount.value + nearMissesCount.value)

const sortedFragmentationRisk = computed(() => {
  if (!stats.value?.fragmentationRisk) return []
  return [...stats.value.fragmentationRisk].sort((a, b) => b.utilizationPercent - a.utilizationPercent)
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
    case 'GREEN': return 'OK'
    case 'YELLOW': return 'LTD'
    case 'RED': return 'CRIT'
  }
}

onMounted(() => {
  store.fetchStats()
})
</script>
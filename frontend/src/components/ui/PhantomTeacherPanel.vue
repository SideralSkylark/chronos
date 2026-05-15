<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X, Loader2, UserX, AlertTriangle, ChevronDown, ChevronUp } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'
import { permutationService } from '@/services/permutationService'
import type { LessonAssignment, CandidateTeacher } from '@/services/dto/timetable'

const props = defineProps<{
  lesson: LessonAssignment
}>()

const emit = defineEmits<{
  'replaced': []
}>()

const isPhantom = computed(() => (props.lesson.teacher?.fullName ?? '').toUpperCase().includes('PHANTOM'))
const isOverloaded = computed(() => !!props.lesson.teacher?.overloaded)

const toast = useToast()
const expanded = ref(false)
const candidates = ref<CandidateTeacher[]>([])
const loadingCandidates = ref(false)
const searchQuery = ref('')
const selectedCandidate = ref<CandidateTeacher | null>(null)
const confirming = ref(false)
const applying = ref(false)

const days = [
  { value: 'MONDAY', label: 'Segunda' },
  { value: 'TUESDAY', label: 'Terça' },
  { value: 'WEDNESDAY', label: 'Quarta' },
  { value: 'THURSDAY', label: 'Quinta' },
  { value: 'FRIDAY', label: 'Sexta' },
]

function dayLabel(day?: string) {
  return days.find(d => d.value === day)?.label ?? day ?? ''
}

async function loadCandidates() {
  loadingCandidates.value = true
  candidates.value = []
  selectedCandidate.value = null
  confirming.value = false
  try {
    candidates.value = await permutationService.getTeacherCandidates(props.lesson.id)
  } catch {
    toast.error('Erro ao carregar candidatos.')
  } finally {
    loadingCandidates.value = false
  }
}

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value && candidates.value.length === 0) loadCandidates()
}

watch(() => props.lesson.id, () => {
  expanded.value = false
  candidates.value = []
  selectedCandidate.value = null
  confirming.value = false
})

const filteredCandidates = computed(() => {
  const query = searchQuery.value.toUpperCase()
  return candidates.value.filter(c => c.username.toUpperCase().includes(query))
})

const eligibleAvailable = computed(() => filteredCandidates.value.filter(c => c.isEligible && !c.wouldExceed))
const eligibleOverLimit = computed(() => filteredCandidates.value.filter(c => c.isEligible && c.wouldExceed))
const otherTeachers = computed(() => filteredCandidates.value.filter(c => !c.isEligible))

const isException = computed(() =>
  selectedCandidate.value !== null && (selectedCandidate.value.wouldExceed || !selectedCandidate.value.isEligible)
)

async function applyReassignment() {
  if (!selectedCandidate.value) return
  applying.value = true
  try {
    await permutationService.reassignTeacher(props.lesson.id, selectedCandidate.value.teacherId)
    toast.success('Professor atribuído com sucesso!')
    emit('replaced')
    expanded.value = false
    selectedCandidate.value = null
    confirming.value = false
    candidates.value = []
  } catch {
    toast.error('Erro ao atribuir professor.')
  } finally {
    applying.value = false
  }
}

function handleConfirm() {
  if (!selectedCandidate.value) return
  if (isException.value) confirming.value = true
  else applyReassignment()
}
</script>

<template>
  <div 
    class="border rounded-md overflow-hidden transition-colors"
    :class="isPhantom ? 'border-red-200' : 'border-amber-200'"
  >

    <!-- Header / toggle -->
    <button
      @click="toggle"
      class="w-full flex items-center justify-between px-3 py-2.5 transition text-left"
      :class="isPhantom ? 'bg-red-50 hover:bg-red-100' : 'bg-amber-50 hover:bg-amber-100'"
    >
      <div class="flex items-center gap-2">
        <div 
          class="w-2 h-2 rounded-full ring-1 ring-white shrink-0" 
          :class="isPhantom ? 'bg-red-400' : 'bg-amber-400'"
        />
        <span 
          class="text-xs font-semibold"
          :class="isPhantom ? 'text-red-700' : 'text-amber-700'"
        >
          {{ isPhantom ? 'Professor fantasma' : 'Professor sobrecarregado' }}
        </span>
        <span 
          class="text-[10px]"
          :class="isPhantom ? 'text-red-400' : 'text-amber-400'"
        >
          — {{ isPhantom ? 'substitua antes ou depois de permutar' : 'considere redistribuir a carga' }}
        </span>
      </div>
      <ChevronDown v-if="!expanded" class="w-3.5 h-3.5 shrink-0" :class="isPhantom ? 'text-red-400' : 'text-amber-400'" />
      <ChevronUp v-else class="w-3.5 h-3.5 shrink-0" :class="isPhantom ? 'text-red-400' : 'text-amber-400'" />
    </button>

    <!-- Expanded body -->
    <div v-if="expanded" class="p-3 space-y-3 bg-white">

      <!-- Stage 1: Selection -->
      <template v-if="!confirming">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Pesquisar professor..."
          class="h-8 w-full px-3 border border-gray-200 rounded-md text-sm bg-white outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-900 transition"
        />

        <div v-if="loadingCandidates" class="flex items-center justify-center py-6">
          <Loader2 class="w-4 h-4 animate-spin text-blue-900" />
        </div>

        <div v-else class="max-h-52 overflow-y-auto space-y-1 pr-0.5">

          <div v-if="eligibleAvailable.length > 0">
            <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1 py-1.5">Elegíveis — disponíveis</h3>
            <div
              v-for="c in eligibleAvailable" :key="c.teacherId"
              @click="selectedCandidate = c"
              class="flex items-center gap-2 px-2.5 py-2 rounded-lg border text-xs cursor-pointer transition"
              :class="selectedCandidate?.teacherId === c.teacherId
                ? 'border-blue-400 bg-blue-50 ring-1 ring-blue-400'
                : 'border-gray-100 bg-gray-50 hover:bg-blue-50 hover:border-blue-200'"
            >
              <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-medium shrink-0 bg-blue-100 text-blue-900">
                {{ c.username.slice(0, 2).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-800 truncate">{{ c.username }}</p>
                <p class="text-[10px] text-gray-400">{{ c.currentWeeklyHours }}h / {{ c.weeklyLimit }}h</p>
              </div>
              <div class="w-12 shrink-0">
                <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <div class="h-full rounded-full bg-blue-500" :style="{ width: Math.min(c.currentWeeklyHours / c.weeklyLimit, 1) * 100 + '%' }" />
                </div>
              </div>
            </div>
          </div>

          <div v-if="eligibleOverLimit.length > 0">
            <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1 py-1.5">Elegíveis — limite excedido</h3>
            <div
              v-for="c in eligibleOverLimit" :key="c.teacherId"
              @click="selectedCandidate = c"
              class="flex items-center gap-2 px-2.5 py-2 rounded-lg border text-xs cursor-pointer transition"
              :class="selectedCandidate?.teacherId === c.teacherId
                ? 'border-amber-400 bg-amber-50 ring-1 ring-amber-400'
                : 'border-gray-100 bg-gray-50 hover:bg-amber-50 hover:border-amber-200'"
            >
              <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-medium shrink-0 bg-amber-100 text-amber-800">
                {{ c.username.slice(0, 2).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-800 truncate">{{ c.username }}</p>
                <p class="text-[10px] text-gray-400">{{ c.currentWeeklyHours }}h / {{ c.weeklyLimit }}h</p>
              </div>
              <div class="w-12 shrink-0">
                <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <div class="h-full rounded-full bg-amber-500" :style="{ width: Math.min(c.currentWeeklyHours / c.weeklyLimit, 1.1) * 100 + '%' }" />
                </div>
              </div>
            </div>
          </div>

          <div v-if="otherTeachers.length > 0">
            <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1 py-1.5">Outros professores</h3>
            <div
              v-for="c in otherTeachers" :key="c.teacherId"
              @click="selectedCandidate = c"
              class="flex items-center gap-2 px-2.5 py-2 rounded-lg border text-xs cursor-pointer transition"
              :class="selectedCandidate?.teacherId === c.teacherId
                ? 'border-red-400 bg-red-50 ring-1 ring-red-400'
                : 'border-gray-100 bg-gray-50 hover:bg-red-50 hover:border-red-200'"
            >
              <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-medium shrink-0 bg-red-100 text-red-800">
                {{ c.username.slice(0, 2).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-800 truncate">{{ c.username }}</p>
                <p class="text-[10px] text-gray-400">{{ c.currentWeeklyHours }}h / {{ c.weeklyLimit }}h</p>
              </div>
              <div class="w-12 shrink-0">
                <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                  <div class="h-full rounded-full" :class="c.wouldExceed ? 'bg-amber-500' : 'bg-blue-500'" :style="{ width: Math.min(c.currentWeeklyHours / c.weeklyLimit, 1.1) * 100 + '%' }" />
                </div>
              </div>
            </div>
          </div>

          <p v-if="!loadingCandidates && filteredCandidates.length === 0" class="text-xs text-gray-400 text-center py-4">
            Nenhum candidato encontrado.
          </p>
        </div>

        <button
          @click="handleConfirm"
          :disabled="!selectedCandidate || applying"
          class="w-full h-8 rounded-lg text-xs font-medium text-white transition flex items-center justify-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed"
          :class="!selectedCandidate || (selectedCandidate.isEligible && !selectedCandidate.wouldExceed)
            ? 'bg-blue-900 hover:bg-blue-800'
            : selectedCandidate.isEligible && selectedCandidate.wouldExceed
              ? 'bg-amber-600 hover:bg-amber-700'
              : 'bg-red-600 hover:bg-red-700'"
        >
          <template v-if="!selectedCandidate || (selectedCandidate.isEligible && !selectedCandidate.wouldExceed)">Atribuir professor</template>
          <template v-else-if="selectedCandidate.isEligible && selectedCandidate.wouldExceed">Confirmar (excede limite)</template>
          <template v-else>Confirmar (sem elegibilidade)</template>
        </button>
      </template>

      <!-- Stage 2: Exception confirmation -->
      <template v-else-if="selectedCandidate">
        <div class="flex items-center gap-2 mb-1">
          <AlertTriangle class="w-3.5 h-3.5 text-amber-500 shrink-0" />
          <span class="text-xs font-semibold text-gray-700">Confirmar excepção</span>
        </div>

        <div class="bg-gray-50 rounded-md p-2.5 text-xs">
          <p class="font-medium text-gray-800">{{ selectedCandidate.username }}</p>
          <p class="text-gray-400 mt-0.5">{{ selectedCandidate.currentWeeklyHours }}h / {{ selectedCandidate.weeklyLimit }}h semanais</p>
        </div>

        <div v-if="selectedCandidate.wouldExceed" class="bg-amber-50 border border-amber-100 rounded-md p-2.5 text-xs text-amber-700 leading-relaxed">
          Esta atribuição excede o limite semanal. A excepção ficará registada.
        </div>
        <div v-if="!selectedCandidate.isEligible" class="bg-red-50 border border-red-100 rounded-md p-2.5 text-xs text-red-700 leading-relaxed">
          Este professor não tem elegibilidade para esta disciplina. A excepção ficará registada.
        </div>

        <div class="flex gap-2">
          <button @click="confirming = false" class="flex-1 h-8 border border-gray-200 rounded-lg text-xs text-gray-500 hover:bg-gray-50 transition flex items-center justify-center gap-1">
            <X class="w-3 h-3" /> Voltar
          </button>
          <button
            @click="applyReassignment"
            :disabled="applying"
            class="flex-1 h-8 rounded-lg text-xs font-medium text-white transition flex items-center justify-center gap-1 disabled:opacity-50"
            :class="!selectedCandidate.isEligible ? 'bg-red-600 hover:bg-red-700' : 'bg-amber-600 hover:bg-amber-700'"
          >
            <Loader2 v-if="applying" class="w-3 h-3 animate-spin" />
            Confirmar excepção
          </button>
        </div>
      </template>

    </div>
  </div>
</template>

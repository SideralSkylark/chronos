<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X, Loader2, UserX, AlertTriangle } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'
import { permutationService } from '@/services/permutationService'
import type { LessonAssignment, CandidateTeacher } from '@/services/dto/timetable'

const props = defineProps<{
  modelValue: boolean
  lesson: LessonAssignment
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  'replaced': []
}>()

const toast = useToast()

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
  } catch (e) {
    toast.error('Erro ao carregar candidatos.')
  } finally {
    loadingCandidates.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val) loadCandidates()
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
    emit('update:modelValue', false)
    selectedCandidate.value = null
    confirming.value = false
    candidates.value = []
  } catch (e) {
    toast.error('Erro ao atribuir professor.')
  } finally {
    applying.value = false
  }
}

function handleConfirm() {
  if (!selectedCandidate.value) return
  if (isException.value) {
    confirming.value = true
  } else {
    applyReassignment()
  }
}

function closeModal() {
  emit('update:modelValue', false)
  selectedCandidate.value = null
  confirming.value = false
  candidates.value = []
}
</script>

<template>
  <div v-if="modelValue" class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50" @click.self="closeModal">
    <div class="bg-white rounded-[10px] shadow-2xl w-full max-w-md border border-gray-100 overflow-hidden">
      
      <!-- STAGE 1: Selection -->
      <template v-if="!confirming">
        <div class="p-5 border-b border-gray-100 flex items-center gap-3">
          <div class="bg-red-50 p-2 rounded-md">
            <UserX class="w-4 h-4 text-red-600" />
          </div>
          <h2 class="text-base font-semibold text-gray-900">Substituir professor fantasma</h2>
          <button @click="closeModal" class="ml-auto text-gray-400 hover:text-gray-600 transition p-0.5">
            <X class="w-4 h-4" />
          </button>
        </div>

        <div class="p-5 space-y-3">
          <!-- Phantom info card -->
          <div class="bg-red-50 border border-red-100 rounded-md p-3">
            <div class="flex justify-between items-center">
              <span class="text-xs font-medium text-red-700">Professor fantasma</span>
              <span class="bg-red-100 text-red-700 text-[10px] font-medium px-2 py-0.5 rounded-full">Placeholder</span>
            </div>
            <p class="mt-1 text-sm font-semibold text-gray-800">{{ lesson.subject.name }}</p>
            <p class="mt-0.5 text-xs text-gray-400">
              {{ lesson.cohort.displayName }} · {{ dayLabel(lesson.timeslot?.dayOfWeek) }} · {{ lesson.timeslot?.startTime.substring(0,5) }} · {{ lesson.room?.name }}
            </p>
          </div>

          <!-- Search input -->
          <input 
            v-model="searchQuery"
            type="text"
            placeholder="Pesquisar professor..."
            class="h-8 w-full px-3 border border-gray-200 rounded-md text-sm bg-white outline-none transition focus:ring-2 focus:ring-blue-100 focus:border-blue-900"
          />

          <!-- Loading state -->
          <div v-if="loadingCandidates" class="flex items-center justify-center py-8">
            <Loader2 class="w-5 h-5 animate-spin text-blue-900" />
          </div>

          <!-- Candidate list -->
          <div v-else class="max-h-60 overflow-y-auto space-y-1 pr-1">
            
            <!-- Eligible Available -->
            <div v-if="eligibleAvailable.length > 0">
              <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1 py-1.5">Elegíveis — disponíveis</h3>
              <div 
                v-for="c in eligibleAvailable" :key="c.teacherId"
                @click="selectedCandidate = c"
                class="flex items-center gap-3 px-3 py-2 rounded-lg border text-xs cursor-pointer transition"
                :class="selectedCandidate?.teacherId === c.teacherId 
                  ? 'border-blue-400 bg-blue-50 ring-1 ring-blue-400' 
                  : 'border-gray-100 bg-gray-50 hover:bg-blue-50 hover:border-blue-200'"
              >
                <div class="w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-medium shrink-0 bg-blue-100 text-blue-900">
                  {{ c.username.slice(0, 2).toUpperCase() }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-800 truncate">{{ c.username }}</p>
                  <p class="text-[10px] text-gray-400">{{ c.currentWeeklyHours }}h / {{ c.weeklyLimit }}h semanais</p>
                </div>
                <div class="w-14 shrink-0">
                  <p class="text-[10px] text-right mb-0.5 text-gray-400">{{ c.currentWeeklyHours }}h</p>
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div class="h-full rounded-full bg-blue-600" :style="{ width: Math.min(c.currentWeeklyHours / c.weeklyLimit, 1) * 100 + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Eligible Over Limit -->
            <div v-if="eligibleOverLimit.length > 0">
              <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1 py-1.5">Elegíveis — limite excedido</h3>
              <div 
                v-for="c in eligibleOverLimit" :key="c.teacherId"
                @click="selectedCandidate = c"
                class="flex items-center gap-3 px-3 py-2 rounded-lg border text-xs cursor-pointer transition"
                :class="selectedCandidate?.teacherId === c.teacherId 
                  ? 'border-amber-400 bg-amber-50 ring-1 ring-amber-400' 
                  : 'border-gray-100 bg-gray-50 hover:bg-amber-50 hover:border-amber-200'"
              >
                <div class="w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-medium shrink-0 bg-amber-100 text-amber-800">
                  {{ c.username.slice(0, 2).toUpperCase() }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-800 truncate">{{ c.username }}</p>
                  <p class="text-[10px] text-gray-400">{{ c.currentWeeklyHours }}h / {{ c.weeklyLimit }}h semanais</p>
                </div>
                <div class="w-14 shrink-0">
                  <p class="text-[10px] text-right mb-0.5 text-amber-600">{{ c.currentWeeklyHours }}h</p>
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div class="h-full rounded-full bg-amber-500" :style="{ width: Math.min(c.currentWeeklyHours / c.weeklyLimit, 1.1) * 100 + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Other Teachers -->
            <div v-if="otherTeachers.length > 0">
              <h3 class="text-[10px] font-bold text-blue-800 uppercase tracking-wider px-1 py-1.5">Outros professores</h3>
              <div 
                v-for="c in otherTeachers" :key="c.teacherId"
                @click="selectedCandidate = c"
                class="flex items-center gap-3 px-3 py-2 rounded-lg border text-xs cursor-pointer transition"
                :class="selectedCandidate?.teacherId === c.teacherId 
                  ? 'border-red-400 bg-red-50 ring-1 ring-red-400' 
                  : 'border-gray-100 bg-gray-50 hover:bg-red-50 hover:border-red-200'"
              >
                <div class="w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-medium shrink-0 bg-red-100 text-red-800">
                  {{ c.username.slice(0, 2).toUpperCase() }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-800 truncate">{{ c.username }}</p>
                  <p class="text-[10px] text-gray-400">{{ c.currentWeeklyHours }}h / {{ c.weeklyLimit }}h semanais</p>
                </div>
                <div class="w-14 shrink-0">
                  <p class="text-[10px] text-right mb-0.5" :class="c.wouldExceed ? 'text-amber-600' : 'text-gray-400'">{{ c.currentWeeklyHours }}h</p>
                  <div class="h-1.5 bg-gray-200 rounded-full overflow-hidden">
                    <div class="h-full rounded-full" :class="c.wouldExceed ? 'bg-amber-500' : 'bg-blue-600'" :style="{ width: Math.min(c.currentWeeklyHours / c.weeklyLimit, 1.1) * 100 + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <div class="flex gap-2 pt-2">
            <button @click="closeModal" class="flex-1 h-9 border border-gray-200 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition flex items-center justify-center gap-1.5">
              <X class="w-3.5 h-3.5" /> Cancelar
            </button>
            <button 
              @click="handleConfirm" 
              :disabled="!selectedCandidate || applying"
              class="flex-1 h-9 rounded-lg text-sm font-medium transition flex items-center justify-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="!selectedCandidate ? 'bg-blue-900 text-white' : 
                      (selectedCandidate.isEligible && !selectedCandidate.wouldExceed) ? 'bg-blue-900 text-white hover:bg-blue-800' :
                      (selectedCandidate.isEligible && selectedCandidate.wouldExceed) ? 'bg-amber-600 text-white hover:bg-amber-700' :
                      'bg-red-600 text-white hover:bg-red-700'"
            >
              <template v-if="!selectedCandidate || (selectedCandidate.isEligible && !selectedCandidate.wouldExceed)">
                Confirmar
              </template>
              <template v-else-if="selectedCandidate.isEligible && selectedCandidate.wouldExceed">
                Confirmar (excede limite)
              </template>
              <template v-else>
                Confirmar (sem elegibilidade)
              </template>
            </button>
          </div>
        </div>
      </template>

      <!-- STAGE 2: Exception Confirmation -->
      <template v-else>
        <div class="p-5 border-b border-gray-100 flex items-center gap-3">
          <div class="bg-amber-50 p-2 rounded-md">
            <AlertTriangle class="w-4 h-4 text-amber-600" />
          </div>
          <h2 class="text-base font-semibold text-gray-900">Confirmar excepção</h2>
        </div>

        <div class="p-5 space-y-3" v-if="selectedCandidate">
          <!-- Selected teacher card -->
          <div class="bg-gray-50 rounded-md p-3">
            <div class="flex justify-between items-center">
              <span class="text-sm font-semibold text-gray-800">{{ selectedCandidate.username }}</span>
              <div class="flex gap-1">
                <span v-if="selectedCandidate.wouldExceed" class="bg-amber-100 text-amber-700 text-[10px] font-medium px-2 py-0.5 rounded-full">Excede limite</span>
                <span v-if="!selectedCandidate.isEligible" class="bg-red-100 text-red-700 text-[10px] font-medium px-2 py-0.5 rounded-full">Sem elegibilidade</span>
              </div>
            </div>
            <p class="mt-0.5 text-xs text-gray-400">
              {{ selectedCandidate.currentWeeklyHours }}h / {{ selectedCandidate.weeklyLimit }}h semanais actuais
            </p>
          </div>

          <!-- Warning boxes -->
          <div v-if="selectedCandidate.wouldExceed" class="bg-amber-50 border border-amber-100 rounded-md p-3 text-xs text-amber-700 leading-relaxed">
            Esta atribuição excede o limite semanal de {{ selectedCandidate.username }}. A excepção ficará registada no histórico do sistema.
          </div>
          
          <div v-if="!selectedCandidate.isEligible" class="bg-red-50 border border-red-100 rounded-md p-3 text-xs text-red-700 leading-relaxed">
            Este professor não tem elegibilidade registada para esta disciplina. A excepção ficará registada no histórico do sistema.
          </div>

          <div class="flex gap-2 pt-2">
            <button @click="confirming = false" class="flex-1 h-9 border border-gray-200 rounded-lg text-sm text-gray-500 hover:bg-gray-50 transition flex items-center justify-center gap-1.5">
              <X class="w-3.5 h-3.5" /> Voltar
            </button>
            <button 
              @click="applyReassignment"
              :disabled="applying"
              class="flex-1 h-9 rounded-lg text-sm font-medium text-white transition flex items-center justify-center gap-1.5 disabled:opacity-50"
              :class="!selectedCandidate.isEligible ? 'bg-red-600 hover:bg-red-700' : 'bg-amber-600 hover:bg-amber-700'"
            >
              <Loader2 v-if="applying" class="w-3.5 h-3.5 animate-spin" />
              Confirmar excepção
            </button>
          </div>
        </div>
      </template>

    </div>
  </div>
</template>

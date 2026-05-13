import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

import { timetableService } from '@/services/timetableService'

import type { TimetableSolution } from '@/services/dto/timetable'
import type { AcademicPeriodDto } from '@/services/dto/timetable'

export const useTimetableStore = defineStore('timetable', () => {
  // ─────────────────────────────────────────────────────────────
  // State
  // ─────────────────────────────────────────────────────────────

  const solution = ref<TimetableSolution | null>(null)

  const availablePeriods = ref<AcademicPeriodDto[]>([])

  const selectedYear = ref<number | null>(null)
  const selectedSemester = ref<number | null>(null)

  const loading = ref(false)
  const generating = ref(false)
  const loadingPeriods = ref(false)

  const error = ref<string | null>(null)

  // ─────────────────────────────────────────────────────────────
  // Derived
  // ─────────────────────────────────────────────────────────────

  const currentPeriod = computed(() => {
    if (!selectedYear.value || !selectedSemester.value) {
      return null
    }

    return {
      year: selectedYear.value,
      semester: selectedSemester.value,
    }
  })

  const availableSemesters = computed(() => {
    if (!selectedYear.value) return []

    return availablePeriods.value.find((p) => p.year === selectedYear.value)?.semesters ?? []
  })

  // ─────────────────────────────────────────────────────────────
  // Initialization
  // ─────────────────────────────────────────────────────────────

  async function initialize() {
    await loadAvailablePeriods()

    if (currentPeriod.value) {
      await loadCurrentPeriod()
    }
  }

  async function loadAvailablePeriods() {
    loadingPeriods.value = true

    try {
      availablePeriods.value = await timetableService.getAvailablePeriods()

      initializeCurrentPeriod()
    } finally {
      loadingPeriods.value = false
    }
  }

  function initializeCurrentPeriod() {
    if (availablePeriods.value.length === 0) {
      return
    }

    const now = new Date()

    const currentYear = now.getFullYear()

    const inferredSemester = now.getMonth() <= 5 ? 1 : 2

    const currentYearExists = availablePeriods.value.some((p) => p.year === currentYear)

    if (currentYearExists) {
      selectedYear.value = currentYear
      selectedSemester.value = inferredSemester
      return
    }

    // fallback to latest available period
    const latest = [...availablePeriods.value].sort((a, b) => {
      if (a.year !== b.year) {
        return b.year - a.year
      }

      return Math.max(...b.semesters) - Math.max(...a.semesters)
    })[0]
  }

  // ─────────────────────────────────────────────────────────────
  // Period Selection
  // ─────────────────────────────────────────────────────────────

  function setPeriod(year: number, semester: number) {
    selectedYear.value = year
    selectedSemester.value = semester
  }

  async function loadCurrentPeriod() {
    if (!currentPeriod.value) return

    await loadForPeriod(currentPeriod.value.year, currentPeriod.value.semester)
  }

  // ─────────────────────────────────────────────────────────────
  // Timetable Loading
  // ─────────────────────────────────────────────────────────────

  async function loadForPeriod(academicYear: number, semester: number) {
    loading.value = true
    error.value = null

    try {
      solution.value = await timetableService.loadPersisted(academicYear, semester)
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? 'Erro ao carregar horário.'

      solution.value = null
    } finally {
      loading.value = false
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Generation
  // ─────────────────────────────────────────────────────────────

  async function generate(onTick?: (attempt: number, elapsedSeconds: number) => void) {
    if (!currentPeriod.value) return

    generating.value = true
    error.value = null
    solution.value = null

    try {
      const { year, semester } = currentPeriod.value

      const { jobId } = await timetableService.generate(year, semester)

      await pollUntilReady(jobId, onTick)

      solution.value = await timetableService.loadPersisted(year, semester)
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? 'Erro ao gerar horário.'

      throw e
    } finally {
      generating.value = false
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Polling
  // ─────────────────────────────────────────────────────────────

  async function pollUntilReady(
    jobId: string,
    onTick?: (attempt: number, elapsedSeconds: number) => void,
  ) {
    const getInterval = (elapsedMs: number): number => {
      if (elapsedMs < 30_000) return 3_000
      if (elapsedMs < 120_000) return 6_000
      return 10_000
    }

    const MAX_WAIT_MS = 360_000

    const startedAt = Date.now()

    let attempt = 0

    while (true) {
      const elapsed = Date.now() - startedAt

      if (elapsed >= MAX_WAIT_MS) {
        throw new Error('Tempo limite de geração excedido. Tente novamente.')
      }

      await new Promise((r) => setTimeout(r, getInterval(elapsed)))

      attempt++

      onTick?.(attempt, Math.floor(elapsed / 1000))

      try {
        const result = await timetableService.getSolution(jobId)

        if (result !== null) {
          return
        }
      } catch {
        // retry silently
      }
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Workflow Actions
  // ─────────────────────────────────────────────────────────────

  async function submitForApproval() {
    if (!solution.value) return

    await timetableService.submitForApproval(solution.value.id)

    solution.value = {
      ...solution.value,
      status: 'PENDING_APPROVAL',
    }
  }

  async function approve() {
    if (!solution.value) return

    await timetableService.approve(solution.value.id)

    solution.value = {
      ...solution.value,
      status: 'APPROVED',
    }
  }

  async function reject() {
    if (!solution.value) return

    await timetableService.reject(solution.value.id)

    solution.value = {
      ...solution.value,
      status: 'DRAFT',
    }
  }

  async function publish() {
    if (!solution.value) return

    await timetableService.publish(solution.value.id)

    solution.value = {
      ...solution.value,
      status: 'PUBLISHED',
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Utils
  // ─────────────────────────────────────────────────────────────

  function clear() {
    solution.value = null
    error.value = null
  }

  // ─────────────────────────────────────────────────────────────
  // Auto reload on period change
  // ─────────────────────────────────────────────────────────────

  watch([selectedYear, selectedSemester], async ([year, semester]) => {
    if (!year || !semester) {
      return
    }

    await loadForPeriod(year, semester)
  })

  // ─────────────────────────────────────────────────────────────
  // Exports
  // ─────────────────────────────────────────────────────────────

  return {
    // state
    solution,
    availablePeriods,
    selectedYear,
    selectedSemester,

    loading,
    generating,
    loadingPeriods,
    error,

    // derived
    currentPeriod,
    availableSemesters,

    // actions
    initialize,
    setPeriod,

    loadCurrentPeriod,
    loadForPeriod,

    generate,

    clear,

    submitForApproval,
    approve,
    reject,
    publish,
  }
})

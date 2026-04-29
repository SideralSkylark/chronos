import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import Timetable from '../Timetable.vue'
import { Role } from '@/services'
import { useTimetableStore } from '@/stores/timetable'
import { useAuthStore } from '@/stores/auth'
import { useCourseStore } from '@/stores/course'
import { useToast } from '@/composables/useToast'
import { permutationService } from '@/services/permutationService'

// Mocking the stores and services
vi.mock('@/stores/timetable', () => ({
  useTimetableStore: vi.fn()
}))
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn()
}))
vi.mock('@/stores/course', () => ({
  useCourseStore: vi.fn()
}))
vi.mock('@/composables/useToast', () => ({
  useToast: vi.fn()
}))
vi.mock('@/services/permutationService', () => ({
  permutationService: {
    getValidSlots: vi.fn(),
    getCohortSwapCandidates: vi.fn(),
    applySwap: vi.fn(),
    applyCohortSwap: vi.fn()
  }
}))

// Mock lucide-vue-next
vi.mock('lucide-vue-next', () => ({
  CalendarDays: { render: () => null },
  ChevronDown: { render: () => null },
  Zap: { render: () => null },
  Loader2: { render: () => null },
  CheckCircle: { render: () => null },
  XCircle: { render: () => null },
  ArrowRightLeft: { render: () => null },
  ArrowRight: { render: () => null },
  X: { render: () => null },
  Send: { render: () => null },
  Globe: { render: () => null },
}))

describe('Timetable.vue Business Logic', () => {
  let timetableStoreMock: any
  let authStoreMock: any
  let courseStoreMock: any
  let toastMock: any

  beforeEach(() => {
    setActivePinia(createPinia())
    
    toastMock = { success: vi.fn(), error: vi.fn(), info: vi.fn() }
    vi.mocked(useToast).mockReturnValue(toastMock)

    authStoreMock = {
      user: { id: 1, roles: [Role.ADMIN] }
    }
    vi.mocked(useAuthStore).mockReturnValue(authStoreMock)

    timetableStoreMock = reactive({
      loading: false,
      generating: false,
      solution: {
        status: 'DRAFT',
        feasible: true,
        score: '0hard/0soft',
        lessonAssignments: []
      },
      loadForPeriod: vi.fn(),
      generate: vi.fn(),
      submitForApproval: vi.fn(),
      approve: vi.fn(),
      reject: vi.fn(),
      publish: vi.fn()
    })
    vi.mocked(useTimetableStore).mockReturnValue(timetableStoreMock)

    courseStoreMock = reactive({
      courses: [],
      fetchAllCoursesSimple: vi.fn()
    })
    vi.mocked(useCourseStore).mockReturnValue(courseStoreMock)
  })

  const factory = () => {
    return mount(Timetable, {
      global: {
        stubs: {
          PageHeader: true,
          FilterBar: true,
          Transition: true,
          Teleport: true
        }
      }
    })
  }

  describe('Permissions & Actions', () => {
    it('allows ADMIN to generate, submit, approve, etc.', async () => {
      authStoreMock.user.roles = [Role.ADMIN, Role.DIRECTOR, Role.ASISTENT]
      timetableStoreMock.solution.status = 'DRAFT'
      const wrapper = factory()
      const vm = wrapper.vm as any

      expect(vm.canGenerate).toBe(true)
      expect(vm.canSubmit).toBe(true)
      
      timetableStoreMock.solution.status = 'PENDING_APPROVAL'
      await wrapper.vm.$nextTick()
      expect(vm.canApprove).toBe(true)
      expect(vm.canReject).toBe(true)

      timetableStoreMock.solution.status = 'APPROVED'
      await wrapper.vm.$nextTick()
      expect(vm.canPublish).toBe(true)
    })

    it('restricts STUDENT and TEACHER from generating', () => {
      authStoreMock.user.roles = [Role.STUDENT]
      const wrapper = factory()
      const vm = wrapper.vm as any
      expect(vm.canGenerate).toBe(false)
      
      authStoreMock.user.roles = [Role.TEACHER]
      expect((factory().vm as any).canGenerate).toBe(false)
    })

    it('allows COORDINATOR to edit only their own course cohort', async () => {
      authStoreMock.user = { id: 10, roles: [Role.COORDINATOR] }
      // Mock some lessons to populate availableCohorts
      timetableStoreMock.solution.lessonAssignments = [
        { id: 1, cohort: { id: 1, displayName: 'C1', courseId: 101 } }
      ]
      courseStoreMock.courses = [
        { id: 101, coordinatorId: 10 } // Owned by user
      ]
      
      const wrapper = factory()
      const vm = wrapper.vm as any

      vm.selectedCohort = 1
      await wrapper.vm.$nextTick()
      expect(vm.canEdit).toBe(true)

      // Different course coordinator
      courseStoreMock.courses = [{ id: 101, coordinatorId: 99 }]
      await wrapper.vm.$nextTick()
      expect(vm.canEdit).toBe(false)
    })
  })

  describe('Filtering & Lessons', () => {
    it('extracts unique cohorts from lessons', () => {
      timetableStoreMock.solution.lessonAssignments = [
        { id: 1, cohort: { id: 1, displayName: 'A' } },
        { id: 2, cohort: { id: 1, displayName: 'A' } },
        { id: 3, cohort: { id: 2, displayName: 'B' } }
      ]
      const wrapper = factory()
      const vm = wrapper.vm as any
      expect(vm.availableCohorts).toHaveLength(2)
      expect(vm.availableCohorts[0].id).toBe(1)
    })

    it('filters lessons by selected cohort', async () => {
      timetableStoreMock.solution.lessonAssignments = [
        { id: 1, cohort: { id: 1 } },
        { id: 2, cohort: { id: 2 } }
      ]
      const wrapper = factory()
      const vm = wrapper.vm as any
      
      vm.selectedCohort = 1
      await wrapper.vm.$nextTick()
      expect(vm.filteredLessons).toHaveLength(1)
      expect(vm.filteredLessons[0].id).toBe(1)
    })
  })

  describe('Cell Logic', () => {
    it('identifies lessons for a specific cell', async () => {
      timetableStoreMock.solution.lessonAssignments = [
        { 
          id: 1, 
          timeslot: { dayOfWeek: 'MONDAY', startTime: '07:00:00' },
          cohort: { id: 1, displayName: 'C1' },
          subject: { name: 'Subj1' }
        }
      ]
      const wrapper = factory()
      const vm = wrapper.vm as any
      await wrapper.vm.$nextTick()
      
      const lessons = vm.getCellLessons('MONDAY', '07:00')
      expect(lessons).toHaveLength(1)
      expect(lessons[0].id).toBe(1)
      
      const otherCell = vm.getCellLessons('TUESDAY', '07:00')
      expect(otherCell).toHaveLength(0)
    })

    it('returns correct year color class', () => {
      const wrapper = factory()
      const vm = wrapper.vm as any
      expect(vm.yearColorClass(1)).toContain('bg-green-100')
      expect(vm.yearColorClass(2)).toContain('bg-blue-100')
      expect(vm.yearColorClass(99)).toContain('bg-gray-100')
    })
  })

  describe('Swap Logic Interaction', () => {
    it('calculates valid slots for a selected lesson', async () => {
      const wrapper = factory()
      const vm = wrapper.vm as any
      const mockLesson = { id: 123 }
      
      vm.selectedLesson = mockLesson
      vi.mocked(permutationService.getValidSlots).mockResolvedValue([{ timeslotId: 1, roomId: 1, isSwap: false, dayOfWeek: 'MONDAY', startTime: '08:00' } as any])
      
      await vm.calculateValidSlots()
      
      expect(permutationService.getValidSlots).toHaveBeenCalledWith(123, expect.any(Number), expect.any(Number))
      expect(vm.validSlots).toHaveLength(1)
      expect(vm.slotsCalculated).toBe(true)
    })

    it('clears selection when clearing filters', () => {
      const wrapper = factory()
      const vm = wrapper.vm as any
      vm.selectedLesson = { id: 1 } as any
      vm.validSlots = [{} as any]
      
      vm.clearSelection()
      
      expect(vm.selectedLesson).toBeNull()
      expect(vm.validSlots).toHaveLength(0)
    })
  })
})

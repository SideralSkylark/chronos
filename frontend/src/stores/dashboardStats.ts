import { defineStore } from 'pinia'
import api from '@/services/api'
import type { ApiResponse } from '@/services/responses/apiResponse'

export interface YearBreakdownDTO {
  year: number
  cohortCount: number
  totalStudents: number
}

export interface CourseRankDTO {
  courseId: number
  courseName: string
  cohortCount: number
}

export interface TeacherWorkloadDTO {
  teacherId: number
  teacherName: string
  totalSlots: number
  weeklyHoursLimit: number
  overloaded: boolean
}

export interface DashboardStatsDTO {
  totalRoomCapacity: number
  totalRoomCount: number
  totalCohortDemand: number
  totalCohortCount: number
  largestCohort: number
  smallestCohort: number
  averageCohortSize: number
  bottleneckYear: number
  cohortsByYear: YearBreakdownDTO[]
  totalTeachers: number
  teachersOverloaded: number
  totalAssignedSlots: number
  avgSlotsPerTeacher: number
  solverReadiness: 'GREEN' | 'YELLOW' | 'RED'
  solverReadinessReason: string
  capacityMargin: number

  morningDemand: number
  afternoonDemand: number
  morningReadiness: 'GREEN' | 'YELLOW' | 'RED'
  afternoonReadiness: 'GREEN' | 'YELLOW' | 'RED'
  morningReadinessReason: string
  afternoonReadinessReason: string

  topCoursesByCohorts: CourseRankDTO[]
  mostLoadedTeachers: TeacherWorkloadDTO[]
  leastLoadedTeachers: TeacherWorkloadDTO[]
}

interface State {
  stats: DashboardStatsDTO | null
  loading: boolean
  error: string | null
}

export const useDashboardStatsStore = defineStore('dashboardStats', {
  state: (): State => ({
    stats: null,
    loading: false,
    error: null,
  }),
  actions: {
    async fetchStats() {
      this.loading = true
      try {
        const res = await api.get<ApiResponse<DashboardStatsDTO>>('/v1/dashboard/stats')
        this.stats = res.data.data
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Erro ao carregar estatísticas do dashboard'
      } finally {
        this.loading = false
      }
    }
  }
})
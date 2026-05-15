import { defineStore } from 'pinia'
import api from '@/services/api'
import type { ApiResponse } from '@/services/responses/apiResponse'

// ── Existing DTOs ──────────────────────────────────────────────────────────

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
  totalHours: number
  weeklyHoursLimit: number
  overloaded: boolean
}

// ── New feasibility diagnostic DTOs ───────────────────────────────────────

export interface OversizedCohortDTO {
  cohortId: number
  cohortName: string
  headcount: number
  minRequiredCapacity: number
  /** How many rooms in the system can fit this cohort */
  compatibleRooms: number
  /** RED = zero compatible rooms; YELLOW = very few (1–2) */
  severity: 'RED' | 'YELLOW'
}

export interface FragmentationRiskCohortDTO {
  cohortId: number
  cohortName: string
  headcount: number
  /** Capacity of the largest compatible room */
  maxCompatibleCapacity: number
  /** headcount / maxCompatibleCapacity * 100 */
  utilizationPercent: number
}

export interface RoomTierDTO {
  /** e.g. "Grande (≥55)", "Média (30–54)", "Pequena (≤29)" */
  label: string
  roomCount: number
  /** % of total rooms that fall in this tier */
  supplyPercent: number
  /** % of cohorts that require this tier */
  demandPercent: number
  /** RED = severe supply/demand mismatch; YELLOW = moderate; GREEN = ok */
  severity: 'RED' | 'YELLOW' | 'GREEN'
}

export interface DistributionMismatchDTO {
  /** e.g. "Média dimensão (30–54 lug.)" */
  category: string
  /** % of cohorts needing this room category */
  demandPercent: number
  /** % of rooms in this category */
  supplyPercent: number
}

// ── Main DTO ───────────────────────────────────────────────────────────────

export interface DashboardStatsDTO {
  academicYear: number
  semester: number

  // Capacity KPIs
  totalRoomCapacity: number
  totalRoomCount: number
  totalCohortDemand: number
  totalCohortCount: number
  largestCohort: number
  smallestCohort: number
  averageCohortSize: number
  capacityMargin: number

  // Solver readiness
  solverReadiness: 'GREEN' | 'YELLOW' | 'RED'
  solverReadinessReason: string

  // Shift analysis
  morningDemand: number
  afternoonDemand: number
  morningReadiness: 'GREEN' | 'YELLOW' | 'RED'
  afternoonReadiness: 'GREEN' | 'YELLOW' | 'RED'
  morningReadinessReason: string
  afternoonReadinessReason: string

  // Teacher KPIs
  totalTeachers: number
  teachersOverloaded: number
  totalAssignedHours: number
  avgHoursPerTeacher: number

  // Year distribution
  bottleneckYear: number
  cohortsByYear: YearBreakdownDTO[]

  // Rankings
  topCoursesByCohorts: CourseRankDTO[]
  mostLoadedTeachers: TeacherWorkloadDTO[]
  leastLoadedTeachers: TeacherWorkloadDTO[]

  // ── Feasibility diagnostics (new) ─────────────────────────────────────
  /** Cohorts whose headcount exceeds available room capacity */
  oversizedCohorts: OversizedCohortDTO[]
  /** Cohorts at ≥85% of their largest compatible room's capacity */
  fragmentationRisk: FragmentationRiskCohortDTO[]
  /** Room supply vs. cohort demand broken down by size tier */
  roomTierDistribution: RoomTierDTO[]
  /** Optional free-text note for the scarcity card (backend-generated) */
  roomScarcityNote: string | null
  /** Per-category demand vs. supply mismatch */
  distributionMismatches: DistributionMismatchDTO[]
  /** Optional free-text note for the mismatch card (backend-generated) */
  distributionMismatchNote: string | null
}

// ── Store ──────────────────────────────────────────────────────────────────

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
    async fetchStats(year: number, semester: number) {
      this.loading = true
      try {
        const params = {
          academicYear: year,
          semester: semester
        }

        const res = await api.get<ApiResponse<DashboardStatsDTO>>('/v1/dashboard/stats', { params })
        this.stats = res.data.data
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Erro ao carregar estatísticas do dashboard'
      } finally {
        this.loading = false
      }
    },
  },
})
